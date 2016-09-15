package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OfflinePlayer;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.services.services_event.PlayerChangesCityOccupationEvent;
import com.jabyftw.lobstercraft.services.services_event.PlayerJoinsCityEvent;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.InventoryHolder;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;

import java.sql.*;
import java.util.*;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Email address: rafael.sartori96@gmail.com
 */
public class CityStructure {

    /*
     * Leveling constants
     */
    private final static byte MAXIMUM_CITY_LEVEL = 10;
    private final static int
            INITIAL_NUMBER_OF_ITEMS_INVENTORY = LobsterCraft.configuration.getInt(ConfigurationValues.CITY_LEVELING_INITIAL_INVENTORY_ITEMS.toString()),
            MAXIMUM_NUMBER_OF_ITEMS_INVENTORY = LobsterCraft.configuration.getInt(ConfigurationValues.CITY_LEVELING_MAXIMUM_AMOUNT_OF_ITEM_INVENTORY.toString()),
            INITIAL_NUMBER_OF_ITEMS_STORE = LobsterCraft.configuration.getInt(ConfigurationValues.CITY_LEVELING_INITIAL_STORE_ITEMS.toString()),
            MAXIMUM_NUMBER_OF_ITEMS_STORE = LobsterCraft.configuration.getInt(ConfigurationValues.CITY_LEVELING_MAXIMUM_AMOUNT_OF_ITEM_STORE.toString()),
            INITIAL_NUMBER_OF_CITIZENS = LobsterCraft.configuration.getInt(ConfigurationValues.CITY_LEVELING_INITIAL_PLAYER_AMOUNT.toString());
    private final static double
            NUMBER_OF_ITEMS_STORE_PER_LEVEL = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_LEVELING_STORE_ITEMS_PER_LEVEL.toString()),
            NUMBER_OF_ITEMS_INVENTORY_PER_LEVEL = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_LEVELING_INVENTORY_ITEMS_PER_LEVEL.toString()),
            NUMBER_OF_CITIZENS_PER_LEVEL = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_LEVELING_PLAYER_PER_LEVEL.toString()),
            INITIAL_PROTECTION_RANGE = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_LEVELING_INITIAL_RANGE.toString()),
            ADDITIONAL_RANGE_PER_LEVEL = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_LEVELING_RANGE_PER_LEVEL.toString()),
            INITIAL_UPGRADE_COST = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_LEVELING_INITIAL_COST.toString()),
            FACTOR_OF_INCREASE_OF_COST = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_LEVELING_COST_MULTIPLIER.toString());

    /*
     * Configuration constants
     */
    protected final static double
            HOUSE_PROTECTION_RADIUS_SQUARED = NumberConversions.square(LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_HOUSE_PROTECTION_DISTANCE.toString())),
            BUILDER_MAXIMUM_RATIO = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_BUILDER_RATIO.toString()),
            MAXIMUM_TAX = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_MAXIMUM_TAX.toString()),
            MINIMUM_TAX = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_MINIMUM_TAX.toString());

    /*
     * Database information
     */
    private final short cityId;
    private final String cityName;
    protected byte cityLevel;
    protected double moneyAmount;
    protected long lastTaxPayDate;
    protected double taxFee;
    private final BlockLocation centerLocation;
    private final CityInventory cityInventory;
    private final CityStore cityStore;

    /*
     * Run time variables
     */
    protected OfflinePlayer cityManager;
    protected HashSet<OfflinePlayer> cityBuilders = new HashSet<>();
    protected DatabaseState databaseState; // shouldn't be "INSERT_TO_DATABASE"
    // "Protected" for saving
    protected final HashMap<BlockLocation, CityHouse> cityHouses = new HashMap<>();

    /*
     * COPIED FROM OLD CITY STRUCTURE
     *
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

    /**
     * Create the CityStructure from database information.
     *
     * @param cityId             city's id
     * @param cityName           city's name (will be lower cased)
     * @param cityLevel          city's level
     * @param moneyAmount        city's money amount (will be used to pay taxes, can be given to the players)
     * @param lastTaxPayDate     city's last time that paid taxes to the server
     * @param taxFee             city's tax fee
     * @param centerLocation     city's centre
     * @param cityInventoryItems city's shared inventory
     * @param cityStoreItems     city's store items
     * @throws IllegalStateException in case we can't
     */
    public CityStructure(short cityId, @NotNull String cityName, byte cityLevel, double moneyAmount, long lastTaxPayDate, double taxFee,
                         @NotNull BlockLocation centerLocation, @Nullable ItemStack[] cityInventoryItems, @Nullable ItemStack[] cityStoreItems)
            throws IllegalStateException {
        this.cityId = cityId;
        this.cityName = cityName.toLowerCase();
        this.cityLevel = cityLevel > MAXIMUM_CITY_LEVEL ? MAXIMUM_CITY_LEVEL : cityLevel; // min(cityLevel, MAXIMUM_CITY_LEVEL)
        this.moneyAmount = moneyAmount;
        this.lastTaxPayDate = lastTaxPayDate;
        this.taxFee = taxFee;
        this.centerLocation = centerLocation;
        this.cityInventory = new CityInventory(InventoryHolder.mergeItems(cityInventoryItems));
        this.cityStore = new CityStore(InventoryHolder.mergeItems(cityStoreItems));

        // Check if city exceeded occupations
        for (OfflinePlayer offlinePlayer : getOfflinePlayers()) {
            if (offlinePlayer.getCityOccupation() == CityOccupation.BUILDER) {
                if (this.cityBuilders.size() < getMaximumNumberOfBuilders()) {
                    this.cityBuilders.add(offlinePlayer);
                } else {
                    ChangeOccupationResponse result;
                    // Change player occupation to citizen, exceeded number of builders
                    if ((result = changeOccupation(offlinePlayer, CityOccupation.CITIZEN)) != ChangeOccupationResponse.SUCCESSFULLY_CHANGED)
                        throw new IllegalStateException(Util.appendStrings("Couldn't remove exceeding builder (playerId=", offlinePlayer.getPlayerId(),
                                ") from city (cityId=", cityId, "): ", result.name()));
                }
            } else if (offlinePlayer.getCityOccupation() == CityOccupation.MANAGER) {
                if (this.cityManager == null) {
                    this.cityManager = offlinePlayer;
                } else {
                    ChangeOccupationResponse result;
                    // Change player occupation to citizen, there must only be ONE manager
                    if ((result = changeOccupation(offlinePlayer, CityOccupation.CITIZEN)) != ChangeOccupationResponse.SUCCESSFULLY_CHANGED)
                        throw new IllegalStateException(Util.appendStrings("Couldn't remove second manager (playerId=", offlinePlayer.getPlayerId(),
                                ") from city (cityId=", cityId, "): ", result.name()));
                }
            }
        }

        // Set database state
        this.databaseState = DatabaseState.ON_DATABASE;
    }

    /*
     * House handling
     */

    /**
     * Create a house for the citizens.<br>
     * Note: this should run asynchronously.
     *
     * @param blockLocation house location given by the city manager
     * @return a response for the CommandSender
     */
    public HouseCreationResponse createHouse(@NotNull final BlockLocation blockLocation) throws SQLException {
        // Check if there are more houses than current number of citizens + 1 (if level isn't the maximum)
        if (cityHouses.size() >= getMaximumNumberOfCitizens() + (cityLevel == MAXIMUM_CITY_LEVEL ? 0 : 2))
            return HouseCreationResponse.TOO_MANY_HOUSES_REGISTERED;

        // Check minimum and maximum distance between city center
        // Note: the corner of the house should be the corner of current protection range
        if (blockLocation.distanceSquared(centerLocation) > (getProtectionRange() - HOUSE_PROTECTION_RADIUS_SQUARED))
            return HouseCreationResponse.TOO_FAR_FROM_CENTER;
        else if (blockLocation.distanceSquared(centerLocation) < HOUSE_PROTECTION_RADIUS_SQUARED)
            return HouseCreationResponse.TOO_CLOSE_TO_THE_CENTER;

        // Check minimum distance between other houses
        for (BlockLocation existingBlockLocation : cityHouses.keySet())
            if (blockLocation.distanceSquared(existingBlockLocation) <= HOUSE_PROTECTION_RADIUS_SQUARED)
                return HouseCreationResponse.TOO_CLOSE_TO_OTHER_HOUSE;

        int houseId;
        // Insert to database
        {
            Connection connection = LobsterCraft.dataSource.getConnection();
            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`city_house_locations` (`city_cityId`, `worlds_worldId`, `centerChunkX`, `centerChunkZ`, `centerX`, `centerY`, `centerZ`) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?);",
                    Statement.RETURN_GENERATED_KEYS
            );

            // Set variables
            preparedStatement.setShort(1, cityId);
            preparedStatement.setByte(2, blockLocation.getChunkLocation().getWorldId());
            preparedStatement.setInt(3, blockLocation.getChunkLocation().getChunkX());
            preparedStatement.setInt(4, blockLocation.getChunkLocation().getChunkZ());
            preparedStatement.setByte(5, blockLocation.getRelativeX());
            preparedStatement.setShort(6, blockLocation.getY());
            preparedStatement.setByte(7, blockLocation.getRelativeZ());

            // Execute statement, get generated key
            preparedStatement.execute();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            // Check if id exists
            if (!generatedKeys.next()) throw new SQLException("Generated key not generated!");

            // Get house key
            houseId = generatedKeys.getInt("houseId");
            if (houseId <= 0) throw new SQLException("House id must be greater than 0");
        }

        // Create variable
        CityHouse cityHouse = new CityHouse(
                houseId,
                cityId,
                blockLocation
        );

        // Insert house and return
        cityHouses.put(cityHouse, cityHouse);
        return HouseCreationResponse.SUCCESSFULLY_CREATED_HOUSE;
    }

    /**
     * Deletes house from database and from the city. This <b>CAN'T BE</b> recovered, all blocks <b>WILL be deleted</b> even when re-created.
     *
     * @param house house instance
     * @return true if house was successfully removed
     */
    public boolean deleteHouse(@NotNull final CityHouse house) {
        if (cityHouses.remove(house, house)) {
            // Add house to soon-to-be excluded list (this will delete house and its blocks during shutdown)
            LobsterCraft.servicesManager.cityService.deletedHouses.add(house.getHouseId());
            return true;
        }
        return false;
    }

    /*
     * Citizen handling
     */

    public Set<OfflinePlayer> getOfflinePlayers() {
        return LobsterCraft.servicesManager.playerHandlerService.getOfflinePlayersPlayersForCity(cityId);
    }

    public Set<OnlinePlayer> getOnlinePlayers(@Nullable OnlinePlayer.OnlineState onlineState) {
        return LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayersForCity(cityId, onlineState);
    }

    /**
     * @param playerId player's id
     * @return the player's house id. Null if none
     */
    public CityHouse getHouseFromPlayer(int playerId) {
        for (CityHouse cityHouse : cityHouses.values())
            if (cityHouse.getPlayerId() != null && cityHouse.getPlayerId() == playerId)
                return cityHouse;
        return null;
    }

    public void broadcastMessage(@Nullable OnlinePlayer.OnlineState onlineState, @NotNull final String string) {
        for (OnlinePlayer onlinePlayer : getOnlinePlayers(onlineState)) {
            onlinePlayer.getPlayer().sendMessage(string);
        }
    }

    /**
     * Insert player on the city
     *
     * @param offlinePlayer player to join the city
     * @return a response for the CommandSender
     */
    public JoinCityResponse joinCity(@NotNull final OfflinePlayer offlinePlayer) {
        // Check if player is registered
        if (!offlinePlayer.isRegistered())
            return JoinCityResponse.PLAYER_NOT_REGISTERED;

        // Check if player is already a citizen
        if (offlinePlayer.getCityId() != null)
            return JoinCityResponse.ALREADY_CITIZEN;

        // Check if city has empty "slots"
        if (getOfflinePlayers().size() >= getMaximumNumberOfCitizens())
            return JoinCityResponse.CITY_OVERPOPULATED;

        // Call event
        PlayerJoinsCityEvent playerJoinsCityEvent = new PlayerJoinsCityEvent(offlinePlayer, this);
        // Note: this will, on monitor priority, if everything is succeeded, change offlinePlayer's attribute, so we MUST NOT change anything after this call:
        Bukkit.getPluginManager().callEvent(playerJoinsCityEvent);
        // DO NOTHING HERE THAT MIGHT CHANGE THE RESULT
        return playerJoinsCityEvent.getResult();
    }

    /**
     * Change player's occupation on the city. If player is becoming a MANAGER, the previous manager (if any) will be set to CITIZEN.
     *
     * @param offlinePlayer player to change occupation
     * @param occupation    player's future occupation
     * @return a response to the CommandSender
     */
    public ChangeOccupationResponse changeOccupation(@NotNull final OfflinePlayer offlinePlayer, @NotNull final CityOccupation occupation) {
        // Check if player is from this city
        if (offlinePlayer.getCityId() == null || offlinePlayer.getCityId() != cityId)
            return ChangeOccupationResponse.PLAYER_FROM_ANOTHER_CITY;

        // Check if player is becoming a BUILDER
        if (occupation == CityOccupation.BUILDER && cityBuilders.size() >= getMaximumNumberOfBuilders())
            return ChangeOccupationResponse.TOO_MANY_BUILDERS;

        // Check if player is leaving manager
        if (offlinePlayer.getCityOccupation() == CityOccupation.MANAGER)
            return ChangeOccupationResponse.CANT_CHANGE_FROM_MANAGER;

        // Check if a new player is joining manager position
        if (occupation == CityOccupation.MANAGER && this.cityManager != null)
            // Change previous manager's occupation to CITIZEN
            if (changeOccupation(this.cityManager, CityOccupation.CITIZEN) != ChangeOccupationResponse.SUCCESSFULLY_CHANGED)
                return ChangeOccupationResponse.CANT_RECOVER_MANAGER_POSITION;

        // Call event
        PlayerChangesCityOccupationEvent changesCityOccupationEvent = new PlayerChangesCityOccupationEvent(offlinePlayer, this, occupation);
        // Note: this will, on monitor priority, if everything is succeeded, change offlinePlayer's attribute, so we MUST NOT change anything after this call:
        Bukkit.getPluginManager().callEvent(changesCityOccupationEvent);

        // DO NOTHING HERE THAT MIGHT CHANGE THE RESULT
        if (changesCityOccupationEvent.getResult() == ChangeOccupationResponse.SUCCESSFULLY_CHANGED) {
            if (occupation == CityOccupation.MANAGER)
                this.cityManager = offlinePlayer;
            else if (occupation == CityOccupation.BUILDER)
                this.cityBuilders.add(offlinePlayer);
        }

        return changesCityOccupationEvent.getResult();
    }

    /**
     * Change player's house.
     *
     * @param offlinePlayer player to change occupation
     * @param house         player selected house
     * @return a response to the CommandSender
     */
    public JoinHouseResponse joinHouse(@NotNull final OfflinePlayer offlinePlayer, @Nullable final CityHouse house) {
        // Check if player is from this city
        if (offlinePlayer.getCityId() == null || offlinePlayer.getCityId() != cityId)
            return JoinHouseResponse.PLAYER_FROM_ANOTHER_CITY;

        // Check if player already has a house
        if (house != null) {
            for (CityHouse cityHouse : cityHouses.values())
                if (cityHouse.getPlayerId() != null && Objects.equals(cityHouse.getPlayerId(), offlinePlayer.getPlayerId()))
                    return JoinHouseResponse.ALREADY_HAVE_A_HOUSE;

            // Check if house is occupied
            if (house.getPlayerId() != null)
                return JoinHouseResponse.HOUSE_ALREADY_OCCUPIED;

            // Change owner
            house.playerId = null;
            house.databaseState = DatabaseState.UPDATE_DATABASE;
            return JoinHouseResponse.SUCCESSFULLY_CHANGED_HOUSE;
        } else {
            for (CityHouse cityHouse : cityHouses.values())
                if (cityHouse.getPlayerId() != null && Objects.equals(cityHouse.getPlayerId(), offlinePlayer.getPlayerId())) {
                    // Change owner
                    cityHouse.playerId = null;
                    cityHouse.databaseState = DatabaseState.UPDATE_DATABASE;
                    return JoinHouseResponse.SUCCESSFULLY_CHANGED_HOUSE;
                }
            return JoinHouseResponse.DOES_NOT_HAVE_A_HOUSE;
        }
    }

    /*
     * Leveling
     */

    public int getMaximumNumberOfCitizens() {
        return INITIAL_NUMBER_OF_CITIZENS + NumberConversions.ceil(NUMBER_OF_CITIZENS_PER_LEVEL * (cityLevel - 1));
    }

    public int getNumberOfStoreItems() {
        return Math.min(INITIAL_NUMBER_OF_ITEMS_STORE + NumberConversions.ceil(NUMBER_OF_ITEMS_STORE_PER_LEVEL * (cityLevel - 1)), MAXIMUM_NUMBER_OF_ITEMS_STORE);
    }

    public int getNumberOfInventoryItems() {
        return Math.min(
                INITIAL_NUMBER_OF_ITEMS_INVENTORY + NumberConversions.ceil(NUMBER_OF_ITEMS_INVENTORY_PER_LEVEL * (cityLevel - 1)),
                MAXIMUM_NUMBER_OF_ITEMS_INVENTORY
        );
    }

    public int getMaximumNumberOfBuilders() {
        return NumberConversions.ceil(getMaximumNumberOfCitizens() * BUILDER_MAXIMUM_RATIO);
    }

    public double getProtectionRange() {
        return Math.min(
                INITIAL_PROTECTION_RANGE + NumberConversions.ceil(ADDITIONAL_RANGE_PER_LEVEL * (cityLevel - 1)),
                ProtectionType.CITY_HOUSES_PROTECTION.getSearchDistance()
        );
    }

    public double getLevelingCost() {
        return INITIAL_UPGRADE_COST * Math.pow(FACTOR_OF_INCREASE_OF_COST, (cityLevel - 1));
    }

    /*
     * Money handling
     */

    public TaxChangeResponse setTaxFee(double taxFee) {
        if (taxFee > MAXIMUM_TAX)
            return TaxChangeResponse.TAX_TOO_HIGH;
        else if (taxFee < MINIMUM_TAX)
            return TaxChangeResponse.TAX_TOO_LOW;

        this.databaseState = DatabaseState.UPDATE_DATABASE;
        broadcastMessage(OnlinePlayer.OnlineState.LOGGED_IN, Util.appendStrings("§6As taxas mudaram de §c", Util.formatTaxes(this.taxFee), "§6 para §c", Util.formatTaxes(taxFee)));
        this.taxFee = taxFee;
        return TaxChangeResponse.TAX_CHANGED;
    }

    /*
     * Getters
     */

    public short getCityId() {
        return cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public BlockLocation getCenterLocation() {
        return centerLocation;
    }

    public CityInventory getCityInventory() {
        return cityInventory;
    }

    public CityStore getCityStore() {
        return cityStore;
    }

    /*
     * Overridden methods
     */

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof CityStructure && ((CityStructure) obj).getCityId() == cityId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 21)
                .append(cityId)
                .append(cityName)
                .toHashCode();
    }

    /*
     * Utility
     */

    private static ItemStack[] getItemStackArray(@NotNull final Map<ItemStack, Integer> items) {
        ItemStack[] itemArray = new ItemStack[items.size()];

        int index = 0;
        // Iterate through all items
        for (Map.Entry<ItemStack, Integer> entry : items.entrySet()) {
            ItemStack itemStackClone = entry.getKey().clone();
            // Set item amount
            itemStackClone.setAmount(entry.getValue());
            // Store item on array
            itemArray[index] = itemStackClone;
            index++;
        }

        return itemArray;
    }

    /*
     * Some classes
     */

    public enum HouseCreationResponse {
        SUCCESSFULLY_CREATED_HOUSE,
        TOO_MANY_HOUSES_REGISTERED,
        TOO_FAR_FROM_CENTER,
        TOO_CLOSE_TO_THE_CENTER,
        TOO_CLOSE_TO_OTHER_HOUSE
    }

    public enum JoinCityResponse {
        SUCCESSFULLY_JOINED_CITY,
        PLAYER_NOT_REGISTERED,
        CITY_OVERPOPULATED,
        ALREADY_CITIZEN
    }

    public enum ChangeOccupationResponse {
        SUCCESSFULLY_CHANGED,
        TOO_MANY_BUILDERS,
        CANT_CHANGE_FROM_MANAGER,
        PLAYER_FROM_ANOTHER_CITY,
        CANT_RECOVER_MANAGER_POSITION
    }

    public enum JoinHouseResponse {
        SUCCESSFULLY_CHANGED_HOUSE,
        PLAYER_FROM_ANOTHER_CITY,
        ALREADY_HAVE_A_HOUSE,
        DOES_NOT_HAVE_A_HOUSE,
        HOUSE_ALREADY_OCCUPIED
    }

    public enum TaxChangeResponse {
        TAX_TOO_HIGH,
        TAX_TOO_LOW,
        TAX_CHANGED
    }

    private class InventoryClickHandler implements InventoryHolder.ClickEventHandler {

        @Override
        public void onOptionClick(InventoryHolder.OptionClickEvent event) {
            InventoryClickEvent clickEvent = event.getBukkitClickEvent();
            // Limit insertions by current city maximum
        }
    }

    private class StoreClickHandler implements InventoryHolder.ClickEventHandler {

        @Override
        public void onOptionClick(InventoryHolder.OptionClickEvent event) {
            InventoryClickEvent clickEvent = event.getBukkitClickEvent();
            // Limit insertions by current city maximum
        }
    }

    protected class CityInventory extends InventoryHolder {

        protected final HashMap<ItemStack, Integer> items;

        /**
         * @param items an map with merged ItemStacks
         * @see InventoryHolder#mergeItems(ItemStack[])
         */
        public CityInventory(HashMap<ItemStack, Integer> items) {
            super(Util.appendStrings(cityName, " - inventario p.%page%"), new InventoryClickHandler(), MAXIMUM_NUMBER_OF_ITEMS_INVENTORY);
            this.items = items;
        }

        public ItemStack[] getItemStackArray() {
            return CityStructure.getItemStackArray(items);
        }
    }

    protected class CityStore extends InventoryHolder {

        protected final HashMap<ItemStack, Integer> items;

        /**
         * @param items an map with merged ItemStacks
         * @see InventoryHolder#mergeItems(ItemStack[])
         */
        public CityStore(HashMap<ItemStack, Integer> items) {
            super(Util.appendStrings(cityName, " - loja p.%page%"), new StoreClickHandler(), MAXIMUM_NUMBER_OF_ITEMS_STORE);
            this.items = items;
        }

        public ItemStack[] getItemStackArray() {
            return CityStructure.getItemStackArray(items);
        }
    }

    public static class CityHouse extends BlockLocation {

        // Database primary key
        private final int houseId;
        private final short cityId;

        // Other variables
        protected Integer playerId;
        protected DatabaseState databaseState; // Can't be "INSERT_TO_DATABASE"

        public CityHouse(int houseId, short cityId, @Nullable Integer playerId, @NotNull ChunkLocation chunkLocation, byte x, short y, byte z) {
            super(chunkLocation, x, y, z);
            this.houseId = houseId;
            this.cityId = cityId;

            this.playerId = playerId;
            this.databaseState = DatabaseState.ON_DATABASE;
        }

        public CityHouse(int houseId, short cityId, @NotNull BlockLocation blockLocation) {
            super(blockLocation);
            this.houseId = houseId;
            this.cityId = cityId;

            this.databaseState = DatabaseState.ON_DATABASE;
        }

        public int getHouseId() {
            return houseId;
        }

        public Integer getPlayerId() {
            return playerId;
        }

        public short getCityId() {
            return cityId;
        }

        public CityStructure getCityStructure() {
            return LobsterCraft.servicesManager.cityService.getCity(cityId);
        }
    }
}
