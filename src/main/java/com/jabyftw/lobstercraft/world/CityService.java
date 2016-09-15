package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.services.Service;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;

import java.io.IOException;
import java.sql.*;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

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
public class CityService extends Service {

    /*
     * Configuration constants
     */
    public final static double CITY_BUILD_COST = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_CREATION_COST.toString());
    public final static double SERVER_TAX_FEE = LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_SERVER_TAX_FEE.toString());
    private final static long SERVER_TAX_PERIOD_MILLIS = TimeUnit.DAYS.toMillis(
            LobsterCraft.configuration.getLong(ConfigurationValues.CITY_SERVER_TAX_PERIOD_DAYS.toString())
    );
    private final static double MINIMUM_DISTANCE_BETWEEN_CITIES_SQUARED = Math.max(ProtectionType.CITY_HOUSES_PROTECTION.getSearchDistanceSquared() * 1.05D,
            NumberConversions.square(LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_MINIMUM_DISTANCE_BETWEEN_CITIES.toString()))
    );
    private final static double MAXIMUM_DISTANCE_BETWEEN_CITIES_SQUARED = NumberConversions.square(
            LobsterCraft.configuration.getDouble(ConfigurationValues.CITY_MAXIMUM_DISTANCE_BETWEEN_CITIES.toString())
    );

    // This Set is used to delete blocks from deleted houses
    protected final ConcurrentSkipListSet<Integer> deletedHouses = new ConcurrentSkipListSet<>();

    /*
     * City storage
     */
    private final ConcurrentHashMap<Short, CityStructure> cityStorage_id = new ConcurrentHashMap<>();

    public CityService() throws SQLException, IOException, ClassNotFoundException {
        cacheCityStorage();
        Bukkit.getScheduler().runTaskTimer(
                LobsterCraft.plugin,
                () -> {
                    for (CityStructure cityStructure : cityStorage_id.values()) {
                        if ((System.currentTimeMillis() - cityStructure.lastTaxPayDate) > SERVER_TAX_PERIOD_MILLIS) {
                            // TODO: spend money (the amount gained since last tax pay * tax fee)
                        }
                    }
                },
                100L,
                10 * 60 * 20L // every 10 minutes
        );
    }

    @Override
    public void onDisable() {
        try {
            saveCityStorage();
        } catch (SQLException | IOException exception) {
            exception.printStackTrace();
        }
    }

    /*
     * City handling
     */

    public CityStructure getCity(short cityId) {
        return cityStorage_id.get(cityId);
    }

    protected Collection<CityStructure> getCityStructures() {
        return cityStorage_id.values();
    }

    public CityStructure matchCity(@NotNull String cityName) {
        if (cityName.length() < 3)
            return null;

        CityStructure mostEqual = null;
        int equalSize = 3;

        for (CityStructure cityStructure : cityStorage_id.values()) {
            int thisSize = Util.getEqualityOfNames(cityName.toCharArray(), cityStructure.getCityName().toCharArray());

            if (thisSize >= equalSize) {
                mostEqual = cityStructure;
                equalSize = thisSize;
            }
        }
        return mostEqual;
    }

    /**
     * The city must have an unique name.<br>
     * <b>Note:</b> this method should run asynchronously - it'll insert the city on database.<br>
     * Note: the player must pay CityService#CITY_BUILD_COST from the player
     *
     * @param cityName           city's name, from 4 to 24 not-special characters (space allowed)
     * @param cityCenterLocation city's center location
     * @return a response for the CommandSender
     * @throws SQLException in case something goes wrong on the database
     */
    public CreateCityResponse createCity(@NotNull final String cityName, @NotNull final BlockLocation cityCenterLocation) throws SQLException {
        // Check if name is valid
        if (!cityName.matches("[A-Za-z 0-9]{" + 4 + "," + 24 + "}"))
            return CreateCityResponse.NAME_NOT_VALID;

        // Check if name is available
        if (matchCity(cityName) != null)
            return CreateCityResponse.NAME_MATCHED_OTHER_CITY;

        // TODO need more checking?

        // Check distance between cities (or between city and spawn, in case there is no city)
        if (cityStorage_id.isEmpty()) {
            BlockLocation blockLocation = new BlockLocation(cityCenterLocation.getWorld().getSpawnLocation());
            double distanceSquared = cityCenterLocation.distanceSquared(blockLocation);
            // Check for minimum and maximum distance
            if (distanceSquared < MINIMUM_DISTANCE_BETWEEN_CITIES_SQUARED)
                return CreateCityResponse.TOO_CLOSE_TO_NEAREST_CITY;
            else if (distanceSquared > MAXIMUM_DISTANCE_BETWEEN_CITIES_SQUARED)
                return CreateCityResponse.TOO_FAR_FROM_NEAREST_CITY;
        } else {
            for (CityStructure cityStructure : cityStorage_id.values()) {
                double distanceSquared = cityCenterLocation.distanceSquared(cityStructure.getCenterLocation());
                // Check for minimum and maximum distance
                if (distanceSquared < MINIMUM_DISTANCE_BETWEEN_CITIES_SQUARED)
                    return CreateCityResponse.TOO_CLOSE_TO_NEAREST_CITY;
                else if (distanceSquared > MAXIMUM_DISTANCE_BETWEEN_CITIES_SQUARED)
                    return CreateCityResponse.TOO_FAR_FROM_NEAREST_CITY;
            }
        }

        // Default values
        short cityId;
        byte cityLevel = 1;
        double cityMoneyAmount = CITY_BUILD_COST * (1.0D - SERVER_TAX_FEE); // receive tax from server
        long lastTaxPayDate = System.currentTimeMillis(); // creation date, so the city don't need to pay taxes on their first minute alive
        double taxFee = (CityStructure.MAXIMUM_TAX + CityStructure.MINIMUM_TAX) / 2.0D; // the average between minimum and maximum

        // Insert city on database
        {
            Connection connection = LobsterCraft.dataSource.getConnection();
            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`city_storage` (`cityName`, `cityLevel`, `moneyAmount`, `lastTaxPayDate`, `taxFee`, `worlds_worldId`, `centerChunkX`, " +
                            "`centerChunkZ`, `centerX`, `centerY`, `centerZ`, `cityInventory`, `cityStoreItems`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                    Statement.RETURN_GENERATED_KEYS
            );

            // Set variables
            preparedStatement.setString(1, cityName);
            preparedStatement.setByte(2, cityLevel);
            preparedStatement.setDouble(3, cityMoneyAmount);
            preparedStatement.setLong(4, lastTaxPayDate);
            preparedStatement.setDouble(5, taxFee);
            preparedStatement.setByte(6, cityCenterLocation.getChunkLocation().getWorldId());
            preparedStatement.setInt(7, cityCenterLocation.getChunkLocation().getChunkX());
            preparedStatement.setInt(8, cityCenterLocation.getChunkLocation().getChunkZ());
            preparedStatement.setByte(9, cityCenterLocation.getRelativeX());
            preparedStatement.setShort(10, cityCenterLocation.getY());
            preparedStatement.setByte(11, cityCenterLocation.getRelativeZ());
            preparedStatement.setNull(12, Types.BLOB);
            preparedStatement.setNull(13, Types.BLOB);

            // Execute statement, retrieve returned key (id)
            preparedStatement.execute();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            // Check if we have a generated key
            if (!generatedKeys.next()) throw new SQLException("Key not generated.");

            // Set variable
            cityId = generatedKeys.getShort("cityId");
            if (cityId <= 0) throw new SQLException("Invalid cityId! Can't be less or equal zero");

            // Close everything
            generatedKeys.close();
            preparedStatement.close();
            connection.close();
        }

        // Create CityStructure
        CityStructure cityStructure = new CityStructure(
                cityId,
                cityName,
                cityLevel,
                cityMoneyAmount,
                lastTaxPayDate,
                taxFee,
                cityCenterLocation,
                null,
                null
        );

        // Insert city structure
        cityStorage_id.put(cityId, cityStructure);
        return CreateCityResponse.SUCCESSFULLY_CREATED_CITY;
    }

    /*
     * Database handling
     */

    private void cacheCityStorage() throws SQLException, IOException, ClassNotFoundException {
        // Retrieve connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        // Load cities
        {
            // Prepare statement and execute query
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.city_storage;");
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through results
            while (resultSet.next()) {
                String cityNameLowered = resultSet.getString("cityName").toLowerCase();
                short cityId = resultSet.getShort("cityId");

                ChunkLocation chunkLocation = new ChunkLocation(
                        resultSet.getByte("worlds_worldId"),
                        resultSet.getInt("centerChunkX"),
                        resultSet.getInt("centerChunkZ")
                );

                // Delete ignored worlds
                if (chunkLocation.worldIsIgnored()) {
                    resultSet.deleteRow();
                    continue;
                }

                BlockLocation centerLocation = new BlockLocation(
                        chunkLocation,
                        resultSet.getByte("centerX"),
                        resultSet.getShort("centerY"),
                        resultSet.getByte("centerZ")
                );

                ItemStack[] cityInventory = null;
                byte[] cityInventoryBytes = resultSet.getBytes("cityInventory");
                if (!resultSet.wasNull()) cityInventory = Util.byteArrayToItemStacks(cityInventoryBytes);

                ItemStack[] cityStoreItems = null;
                byte[] cityStoreItemsBytes = resultSet.getBytes("cityInventory");
                if (!resultSet.wasNull()) cityStoreItems = Util.byteArrayToItemStacks(cityStoreItemsBytes);

                // Create city structure
                CityStructure cityStructure = new CityStructure(
                        cityId,
                        cityNameLowered,
                        resultSet.getByte("cityLevel"),
                        resultSet.getDouble("moneyAmount"),
                        resultSet.getLong("lastTaxPayDate"),
                        resultSet.getDouble("taxFee"),
                        centerLocation,
                        cityInventory,
                        cityStoreItems
                );

                // Insert city structure
                cityStorage_id.put(cityId, cityStructure);
            }

            // Close statement
            resultSet.close();
            preparedStatement.close();
        }

        // Load city houses
        {
            // Prepare and execute statement
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.city_house_locations;");
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through constructions
            while (resultSet.next()) {
                short cityId = resultSet.getShort("");
                Integer playerId = resultSet.getInt("user_playerId");
                if (resultSet.wasNull()) playerId = null;

                ChunkLocation chunkLocation = new ChunkLocation(
                        resultSet.getByte("worlds_worldId"),
                        resultSet.getInt("centerChunkX"),
                        resultSet.getInt("centerChunkZ")
                );

                // Delete row from ignored worlds
                if (chunkLocation.worldIsIgnored()) {
                    resultSet.deleteRow();
                    continue;
                }

                int houseId = resultSet.getInt("houseId");
                // Check if city was deleted
                if (getCity(cityId) == null) {
                    deletedHouses.add(houseId);
                    resultSet.deleteRow();
                    continue;
                }

                // Create house
                CityStructure.CityHouse cityHouse = new CityStructure.CityHouse(
                        houseId,
                        cityId,
                        playerId,
                        chunkLocation,
                        resultSet.getByte("centerX"),
                        resultSet.getShort("centerY"),
                        resultSet.getByte("centerZ")
                );

                // Add house to city
                getCity(cityId).cityHouses.put(cityHouse, cityHouse);
            }

            // Close everything
            resultSet.close();
            preparedStatement.close();
        }

        // Close connection
        connection.close();
    }

    private void saveCityStorage() throws SQLException, IOException {
        // Retrieve connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        // Prepare statement and execute query
        PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE `minecraft`.`city_storage` SET `cityLevel` = ?, `moneyAmount` = ?, `lastTaxPayDate` = ?, `taxFee` = ?, `cityInventory` = ?, " +
                        "`cityStoreItems` = ? WHERE `cityId` = ?;"
        );
        PreparedStatement houseUpdateStatement = connection.prepareStatement(
                "UPDATE `minecraft`.`city_house_locations` SET `user_playerId` = ? WHERE `houseId` = ? AND `city_cityId` = ?;"
        );

        // Set variables
        for (CityStructure cityStructure : cityStorage_id.values()) {
            if (cityStructure.databaseState.shouldSave()) {
                // Set variables
                preparedStatement.setByte(1, cityStructure.cityLevel);
                preparedStatement.setDouble(2, cityStructure.moneyAmount);
                preparedStatement.setLong(3, cityStructure.lastTaxPayDate);
                preparedStatement.setDouble(4, cityStructure.taxFee);
                preparedStatement.setBytes(5, Util.itemStacksToByteArray(cityStructure.getCityInventory().getItemStackArray()));
                preparedStatement.setBytes(6, Util.itemStacksToByteArray(cityStructure.getCityStore().getItemStackArray()));
                preparedStatement.setShort(7, cityStructure.getCityId());

                // Add batch
                preparedStatement.addBatch();

                // Update CityStructure
                cityStructure.databaseState = DatabaseState.ON_DATABASE;
            }

            for (CityStructure.CityHouse cityHouse : cityStructure.cityHouses.values()) {
                if (cityHouse.databaseState.shouldSave()) {
                    // Set variables
                    houseUpdateStatement.setObject(1, cityHouse.getPlayerId(), Types.INTEGER);
                    houseUpdateStatement.setInt(2, cityHouse.getHouseId());
                    houseUpdateStatement.setShort(3, cityHouse.getCityId());

                    // Add batch
                    houseUpdateStatement.addBatch();

                    // Update CityHouse
                    cityHouse.databaseState = DatabaseState.ON_DATABASE;
                }
            }
        }

        // Execute and close everything
        preparedStatement.executeBatch();
        preparedStatement.close();
        houseUpdateStatement.executeBatch();
        houseUpdateStatement.close();
        connection.close();
    }

    /*
     * Some classes
     */

    public enum CreateCityResponse {
        SUCCESSFULLY_CREATED_CITY,
        NAME_MATCHED_OTHER_CITY,
        NAME_NOT_VALID,
        TOO_FAR_FROM_NEAREST_CITY,
        TOO_CLOSE_TO_NEAREST_CITY
    }
}
