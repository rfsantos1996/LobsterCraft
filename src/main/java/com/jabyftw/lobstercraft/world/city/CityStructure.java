package com.jabyftw.lobstercraft.world.city;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OfflinePlayerHandler;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.util.location_util.BlockLocation;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;

import java.util.*;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Email address: rafael.sartori96@gmail.com
 */
public class CityStructure {

    // Configuration for number of homes
    private static final int NUMBER_OF_PLAYERS_NEEDED = LobsterCraft.config.getInt(ConfigValue.CITY_STRUCTURE_BASE_PLAYER_NEEDED.getPath());
    private static final double PLAYER_MULTIPLIER = LobsterCraft.config.getDouble(ConfigValue.CITY_STRUCTURE_PLAYER_MULTIPLIER.getPath());

    // Configuration for money costs on update
    private static final double
            STARTING_CITY_MONEY = LobsterCraft.config.getDouble(ConfigValue.CITY_STRUCTURE_BASE_MONEY_NEEDED.getPath()),
            MONEY_MULTIPLIER = LobsterCraft.config.getDouble(ConfigValue.CITY_STRUCTURE_MONEY_MULTIPLIER.getPath());

    // Ceil/floor taxes
    private static final double
            CEIL_CITY_TAX = LobsterCraft.config.getDouble(ConfigValue.MONEY_CITY_CEIL_TAX.getPath()),
            FLOOR_CITY_TAX = LobsterCraft.config.getDouble(ConfigValue.MONEY_CITY_FLOOR_TAX.getPath()),
            FLOOR_LOW_CAPACITY_CITY_TAX = LobsterCraft.config.getDouble(ConfigValue.MONEY_CITY_FLOOR_TAX.getPath());
    // Alarming economy, tax increase
    private static final double ALARMING_ECONOMY_TRIGGER = LobsterCraft.config.getDouble(ConfigValue.MONEY_CITY_ALARMING_CAPACITY_ECONOMY.getPath());

    private long cityId = PlayerHandler.UNDEFINED_PLAYER;
    private final String cityName;
    //private final EconomyStructure economyStructure;
    private final HashSet<CityHomeLocation> homeLocations = new HashSet<>();
    private final HashMap<ItemStack, Integer> cityInventory = new HashMap<>(), storeItems = new HashMap<>(); // Store as an array of items with their amount

    private DatabaseState databaseState = DatabaseState.NOT_ON_DATABASE;
    private BlockLocation cityCenter;
    private volatile int cityLevel = 1;
    private long lastTaxPayDate;
    private double taxFee = (CEIL_CITY_TAX + FLOOR_CITY_TAX) / 2.0D;

    public CityStructure(long cityId, @NotNull final String cityName, //@NotNull final EconomyStructure economyStructure,
                         @Nullable final ItemStack[] cityInventory, @Nullable final ItemStack[] storeItems,
                         @Nullable final Collection<CityHomeLocation> homeLocations, @NotNull final BlockLocation cityCenter,
                         int cityLevel, long lastTaxPayDate, double taxFee) {
        this.cityId = cityId;
        this.cityName = cityName;
        //this.economyStructure = economyStructure;
        this.homeLocations.addAll(homeLocations);
        this.cityInventory.putAll(getHashMapFromItemStacks(cityInventory));
        this.storeItems.putAll(getHashMapFromItemStacks(storeItems));

        this.databaseState = DatabaseState.ON_DATABASE;
        this.cityCenter = cityCenter;
        this.cityLevel = cityLevel;
        this.lastTaxPayDate = lastTaxPayDate;
        this.taxFee = taxFee;

        checkForLevelUp();
    }

    /*
     * Server taxes for cities will be:
     *      Total earnings since the last server fee date * serverTaxFee => retrieve the earnings (positive amounts) for this economy structure from history since the last server fee pay date
     *
     * World EconomyStructure:
     *      It'll go out from the server to the common players through job
     *      It'll go out from the city to the citizens through job (they'll have a bonus of exp and money)
     *     Every player earnings/expenses will include taxes (server's OR city's taxes; city taxes are higher)
     *
     *     City will get money to pay the default server tax and have some to upgrade or pay more jobs
     *     City expenses will not account on taxes (just earnings)
     *     All player expenses/earnings will have taxes either from city (it'll have a ceiling and floor, the manager will set the right amount for the city) or server (fixed one, lower than the city's floor tax)
     *          Earnings will be discounted the fee: receiver will receive the amount * fee and player amount * (1 - fee)
     *          Expenses will be increased the fee: receiver will pay amount * (1 + fee), receiver will receive the amount * fee
     *     Leaving/Creating/Entering the city will cost money (on server taxes)
     *     Updating the city won't cost taxes since it'll be used the city money (through earnings and deposits)
     *     Depositing to the city will be on server's taxes
     *
     * World without enough money:
     *      Players will receive less job money (server has 25% of his total capacity ? start correcting to economize) => total capacity being Amount per player * number of players
     *      Base fees will be higher
     *
     * City without enough money:
     *      Players will receive less job money (city has 25% of his level up capacity)
     *      If players cant lend the city money => City will be on negative and, eventually, break (after the 3 negatives spents on the server fee)
     */

    public double moneyNeededForUpdate(int cityLevel, boolean getNextLevel) {
        // Current value + remaining 30% of next level value
        return (cityLevel * MONEY_MULTIPLIER * STARTING_CITY_MONEY) + (getNextLevel ? ALARMING_ECONOMY_TRIGGER * moneyNeededForUpdate(cityLevel + 1, false) : 0);
    }

    public int numberOfCitizensNeededForUpdate(int cityLevel) {
        return NumberConversions.ceil(cityLevel * PLAYER_MULTIPLIER * NUMBER_OF_PLAYERS_NEEDED);
    }

    public int maximumNumberOfCitizens() {
        // 1 less than the next level up is capable of handling
        return numberOfCitizensNeededForUpdate(cityLevel + 1) - 1;
    }

    public long getCityId() {
        return cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public BlockLocation getCityCenter() {
        return cityCenter;
    }

    public synchronized int getCityLevel() {
        return cityLevel;
    }

    public JoinCityResponse joinCity(@NotNull final PlayerHandler playerHandler, @NotNull final CityHomeLocation homeLocation) {
        if (playerHandler.hasCity())
            return JoinCityResponse.ALREADY_IN_CITY;

        if (homeLocation.getCityId() != cityId)
            return JoinCityResponse.ERROR_OCCURRED;

        return JoinCityResponse.SUCCESSFULLY_JOINED_CITY;
    }

    public enum JoinCityResponse {
        ERROR_OCCURRED,
        ALREADY_IN_CITY,
        SUCCESSFULLY_JOINED_CITY
    }

    public Set<OfflinePlayerHandler> getCitizens() {
        HashSet<OfflinePlayerHandler> offlinePlayers = new HashSet<>();
        // Iterate through all OfflinePlayers
        for (OfflinePlayerHandler playerHandler : LobsterCraft.playerHandlerService.getOfflinePlayers())
            // Check for same cityId
            if (playerHandler.hasCity() && playerHandler.getCityId() == cityId)
                // Add player to list
                offlinePlayers.add(playerHandler);
        return offlinePlayers;
    }

    public Set<PlayerHandler> getOnlineCitizens() {
        HashSet<PlayerHandler> onlinePlayers = new HashSet<>();
        // Iterate through all OfflinePlayers
        for (PlayerHandler playerHandler : LobsterCraft.playerHandlerService.getOnlinePlayers())
            // Check for the same cityId
            if (playerHandler.hasCity() && playerHandler.getCityId() == cityId)
                // Add player to list
                onlinePlayers.add(playerHandler);
        return onlinePlayers;
    }

    public void sendMessage(String message) {
        for (PlayerHandler playerHandler : getOnlineCitizens())
            playerHandler.sendMessage(message);
    }

    public TaxFeeChangeResponse setTaxFee(double taxFee) {
        if (taxFee < 0.0D || taxFee > 1.0D)
            return TaxFeeChangeResponse.OUT_OF_RANGE;
        if (taxFee > CEIL_CITY_TAX)
            return TaxFeeChangeResponse.TAX_TOO_HIGH;
        if (taxFee < FLOOR_CITY_TAX)
            return TaxFeeChangeResponse.TAX_TOO_LOW;

        sendMessage("§6Taxas mudaram de §c" + Util.formatTaxes(this.taxFee) + "§6 para §c" + Util.formatTaxes(taxFee));
        this.taxFee = taxFee;
        setAsModified();
        return TaxFeeChangeResponse.SUCCESSFULLY_CHANGED_TAXES;
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    public void setAsModified() {
        if (databaseState == DatabaseState.NOT_ON_DATABASE)
            databaseState = DatabaseState.INSERT_TO_DATABASE;
        if (databaseState == DatabaseState.ON_DATABASE)
            databaseState = DatabaseState.UPDATE_DATABASE;
    }

    /*private void checkTaxLevel() {
        if ((economyStructure.getMoneyAmount() / moneyNeededForUpdate(cityLevel, false)) <= ALARMING_ECONOMY_TRIGGER)
            // Update tax
            setTaxFee(Math.max(taxFee, FLOOR_LOW_CAPACITY_CITY_TAX));
    }*/

    private synchronized LevelUpResponse checkForLevelUp() {
        // Check if tax is right
        //checkTaxLevel();

        // Check for required money
        //if (economyStructure.getMoneyAmount() < moneyNeededForUpdate(cityLevel, true))
        //return LevelUpResponse.NOT_ENOUGH_MONEY;

        // Check for required players
        if (getCitizens().size() < numberOfCitizensNeededForUpdate(cityLevel))
            return LevelUpResponse.NOT_ENOUGH_CITIZENS;

        // Added level
        cityLevel += 1;
        setAsModified();

        // Broadcast achievement
        sendMessage("§6A cidade evoluiu para o§c level " + cityLevel + "§6! Agora será possível construir até §c" + maximumNumberOfCitizens() + " casas§6!");

        return LevelUpResponse.SUCCESSFULLY_LEVELED_UP;
    }

    private HashMap<ItemStack, Integer> getHashMapFromItemStacks(@Nullable final ItemStack[] itemStacks) {
        HashMap<ItemStack, Integer> items = new HashMap<>();

        // Check if itemStacks is null
        if (itemStacks != null)
            // Iterate through all of them
            for (ItemStack itemStack : itemStacks) {
                int itemAmount = itemStack.getAmount();
                // Reset to 1 the amount
                itemStack.setAmount(1);
                // Insert to map
                items.put(itemStack, itemAmount);
            }

        return items;
    }

    private ItemStack[] getItemStacksFromHashMap(@NotNull final HashMap<ItemStack, Integer> storedItems) {
        Set<Map.Entry<ItemStack, Integer>> entries = storedItems.entrySet();

        int i = 0;
        ItemStack[] itemStacks = new ItemStack[entries.size()];

        for (Map.Entry<ItemStack, Integer> entry : entries) {
            ItemStack itemStack = entry.getKey().clone();
            itemStack.setAmount(entry.getValue());
            itemStacks[i++] = itemStack;
        }

        return itemStacks;
    }

    @Override
    public int hashCode() {
        return cityName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CityStructure && cityName.equals(((CityStructure) obj).cityName)) || (obj instanceof String && cityName.equals(obj));
    }

    public enum TaxFeeChangeResponse {
        TAX_TOO_HIGH,
        TAX_TOO_LOW,
        OUT_OF_RANGE,
        SUCCESSFULLY_CHANGED_TAXES
    }

    public enum LevelUpResponse {
        NOT_ENOUGH_CITIZENS,
        NOT_ENOUGH_MONEY,
        SUCCESSFULLY_LEVELED_UP
    }
}
