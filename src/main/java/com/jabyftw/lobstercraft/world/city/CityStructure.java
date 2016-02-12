package com.jabyftw.lobstercraft.world.city;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.economy.EconomyStructure;
import com.jabyftw.lobstercraft.player.OfflinePlayerHandler;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.world.util.location_util.BlockLocation;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
    private static final int NUMBER_OF_PLAYERS_NEEDED = LobsterCraft.config.getInt(ConfigValue.LOCATION_TELEPORT_TIME_BETWEEN_ACCEPT_TRIGGERS.getPath());
    private static final double PLAYER_MULTIPLIER = LobsterCraft.config.getDouble(ConfigValue.LOCATION_TELEPORT_TIME_BETWEEN_ACCEPT_TRIGGERS.getPath());

    // Configuration for money costs on update
    private static final double STARTING_CITY_MONEY = LobsterCraft.config.getDouble(ConfigValue.MONEY_CITY_STARTING_MONEY.getPath());
    private static final double MONEY_MULTIPLIER = LobsterCraft.config.getDouble(ConfigValue.LOCATION_TELEPORT_TIME_BETWEEN_ACCEPT_TRIGGERS.getPath());

    private final long cityId;
    private final String cityName;
    private final EconomyStructure economyStructure;
    private final HashMap<ItemStack, Integer> cityInventory, storeItems; // Store as an array of items with their amount

    private BlockLocation cityCenter;
    private volatile int cityLevel;

    public CityStructure(long cityId, @NotNull final String cityName, @NotNull final EconomyStructure economyStructure,
                         @Nullable final ItemStack[] cityInventory, @Nullable final ItemStack[] storeItems,
                         @NotNull final BlockLocation cityCenter, int cityLevel) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.economyStructure = economyStructure;
        this.cityInventory = getHashMapFromItemStacks(cityInventory);
        this.storeItems = getHashMapFromItemStacks(storeItems);

        this.cityCenter = cityCenter;
        this.cityLevel = cityLevel;
    }

    /*
     * EconomyStructure {
     *      long economyId
     *      EconomyType type
     *      double moneyAmount
     *      MoneyResponse spendMoney(double amount, String reason, boolean allowNegative) => SUCCESSFULLY_SPENT, INVALID_AMOUNT, NOT_ENOUGH_MONEY, MAXIMUM_NEGATIVE_AMOUNT_REACHED, INVALID_REASON, ERROR_OCCURRED
     *      MoneyResponse receiveMoney(double amount, String reason) => SUCCESSFULLY_SPENT, INVALID_AMOUNT, INVALID_REASON (64 characters), ERROR_OCCURRED
     * }
     */

    /*
     * List of center of house locations (stored on a different table)
     * City center location
     * City name (allow space, no special characters but underline and -; 24 characters)
     * City level (affected by number of citizens, fees acquired - city money)
     * City official's occupation (must be 1 manager at a time, varying on builders) => got from OfflinePlayerHandler
     * City Inventory (will share items)
     * City EconomyStructure {
     *      After certain level => open store, people have to visit the city to buy items
     *      After certain level => non-standard items will be allowed
     *      City money => percentage of player's received money will go to the city
     *      After some days the city will have to pay their part to the server (restoring the money that the server lends the player through Jobs, level ups -> fixed level, skills)
     *      Can go negative 3 times
     * }
     * City Store (will sell an amount of shared items on city's price - corrected via world's economy [commanded by an administrator] AND via city's deficit [city can loan money - approved by the manager])
     * City id
     * City OfflinePlayerHandlers => got from OfflinePlayerHandler
     *
     * Note: OfflinePlayerHandler will store the city Id and city position with cityPositionDate (may be a manager, builder or just citizen), exp, level, player class (Miner, Archer, Fighter, Builder and Woodcutter)
     */
    /*
     * World EconomyStructure {
     *      world money => give money to players through jobs, receives an amount when a player is registered
     *      Receives an amount when city pays its fees
     * }
     */

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

    private synchronized LevelUpResponse checkForLevelUp() {
        // Check for required money
        if (economyStructure.getMoneyAmount() < moneyNeededForUpdate())
            return LevelUpResponse.NOT_ENOUGH_MONEY;

        // Check for required players
        if (getCitizens().size() < numberOfCitizensNeeded())
            return LevelUpResponse.NOT_ENOUGH_CITIZENS;

        // Added level
        cityLevel += 1;

        // Broadcast achievement
        sendMessage("§6A cidade evoluiu para o§c level " + cityLevel + "§6! Agora será possível construir até §c" + maximumNumberOfCitizens() + " casas§6!");

        return LevelUpResponse.SUCCESSFULLY_LEVELED_UP;
    }

    public double moneyNeededForUpdate() {
        return cityLevel * MONEY_MULTIPLIER * STARTING_CITY_MONEY;
    }

    public int numberOfCitizensNeeded() {
        return NumberConversions.ceil(cityLevel * PLAYER_MULTIPLIER * NUMBER_OF_PLAYERS_NEEDED);
    }

    public int maximumNumberOfCitizens() {
        return NumberConversions.ceil(numberOfCitizensNeeded() * PLAYER_MULTIPLIER) - 1;
    }

    public long getCityId() {
        return cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public synchronized EconomyStructure.SpentMoneyResponse spendAmount(double amount, @NotNull final String reason, boolean allowNegative) {
        EconomyStructure.SpentMoneyResponse spentMoneyResponse = economyStructure.spendAmount(amount, reason, allowNegative);
        checkForLevelUp();
        return spentMoneyResponse;
    }

    public synchronized EconomyStructure.MoneyReceiveResponse receiveAmount(double amount, @NotNull final String reason) {
        EconomyStructure.MoneyReceiveResponse moneyReceiveResponse = economyStructure.receiveAmount(amount, reason);
        checkForLevelUp();
        return moneyReceiveResponse;
    }

    public BlockLocation getCityCenter() {
        return cityCenter;
    }

    public synchronized int getCityLevel() {
        return cityLevel;
    }

    @Override
    public int hashCode() {
        return cityName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CityStructure && cityName.equals(((CityStructure) obj).cityName)) || (obj instanceof String && cityName.equals(obj));
    }

    public enum LevelUpResponse {
        NOT_ENOUGH_CITIZENS,
        NOT_ENOUGH_MONEY,
        SUCCESSFULLY_LEVELED_UP
    }
}
