package com.jabyftw.lobstercraft.economy;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

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
public class EconomyStructure {

    private static final double LOWEST_AMOUNT_ALLOWED = -Math.abs(LobsterCraft.config.getDouble(ConfigValue.MONEY_PLAYER_MINIMUM_AMOUNT_POSSIBLE.getPath()));

    // Database information
    private final StructureType structureType;
    private final Long structureOwnerId;
    private volatile long economyId = PlayerHandler.UNDEFINED_PLAYER;

    private DatabaseState databaseState = DatabaseState.NOT_ON_DATABASE;
    private double moneyAmount;

    public EconomyStructure(long economyId, @NotNull final StructureType structureType, @Nullable final Long structureOwnerId, double moneyAmount) {
        this.structureType = structureType;
        if (structureType.requireId() && structureOwnerId == null)
            throw new IllegalArgumentException("Expected a structureId != null.");
        this.structureOwnerId = structureOwnerId;
        this.economyId = economyId;
        this.moneyAmount = moneyAmount; // Do not check for lowest amount allowed. Even if the configuration changed, the player must pay
        this.databaseState = DatabaseState.ON_DATABASE;
    }

    public EconomyStructure(@NotNull final StructureType structureType, @Nullable final Long structureOwnerId) {
        this.structureType = structureType;
        if (structureType.requireId() && structureOwnerId == null)
            throw new IllegalArgumentException("Expected a structureId != null.");
        this.structureOwnerId = structureOwnerId;
        this.moneyAmount = structureType.getStartingMoney();
    }

    public synchronized double getMoneyAmount() {
        return moneyAmount;
    }

    // The method MUST be synchronized with the object
    public synchronized MoneyReceiveResponse receiveAmount(final double amount, @NotNull final String reason) {
        // Check reason length for database
        if (!Util.checkStringLength(reason, 3, 64))
            return MoneyReceiveResponse.INVALID_REASON_LENGTH;

        // Check if amount is valid, actually deny negative values
        if (amount <= 0.0D)
            return MoneyReceiveResponse.INVALID_AMOUNT;

        // Register history
        BukkitScheduler.runTaskAsynchronously(() -> registerHistory(moneyAmount, amount, reason));

        // Update amount
        moneyAmount += amount;
        setAsModified();

        return MoneyReceiveResponse.SUCCESSFULLY_RECEIVED;
    }

    // The method MUST be synchronized with the object
    public synchronized SpentMoneyResponse spendAmount(double amount, @NotNull final String reason, boolean allowNegative) {
        // Check reason length for database
        if (!Util.checkStringLength(reason, 3, 64))
            return SpentMoneyResponse.INVALID_REASON_LENGTH;

        // Check if amount is valid
        if (amount == 0.0D)
            return SpentMoneyResponse.INVALID_AMOUNT;

        // Ignore the signal
        final double finalAmount = Math.abs(amount);

        // Subtract the money amount, do not care about the signal
        double afterChangeAmount = moneyAmount - finalAmount;

        // Check if have enough money
        if ((allowNegative && afterChangeAmount < LOWEST_AMOUNT_ALLOWED) || (!allowNegative && afterChangeAmount < 0.0D))
            return SpentMoneyResponse.NOT_ENOUGH_MONEY;

        BukkitScheduler.runTaskAsynchronously(() -> registerHistory(moneyAmount, -finalAmount, reason));

        // Update amount
        moneyAmount = afterChangeAmount;
        setAsModified();

        return SpentMoneyResponse.SUCCESSFULLY_SPENT;
    }

    public long getEconomyId() {
        return economyId;
    }

    public StructureType getStructureType() {
        return structureType;
    }

    public Long getStructureOwnerId() {
        return structureOwnerId;
    }

    private boolean wasRegistered() {
        return economyId > 0;
    }

    private Collection<EconomyHistoryEntry> getHistory(int page) throws SQLException {
        return getHistory(economyId, page);
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    protected void setAsModified() {
        if (databaseState == DatabaseState.NOT_ON_DATABASE)
            databaseState = DatabaseState.INSERT_TO_DATABASE;
        if (databaseState == DatabaseState.ON_DATABASE)
            databaseState = DatabaseState.UPDATE_DATABASE;
    }

    /**
     * Must run asynchronously
     *
     * @param economyId economy structure's Id
     * @param page      page number, must be greater than 0
     * @return a list containing all history for this page
     * @throws SQLException             in case something goes wrong searching the database
     * @throws IllegalArgumentException if economy structure is less than zero (aka. structure not registered)
     */
    public static Collection<EconomyHistoryEntry> getHistory(long economyId, int page) throws SQLException {
        if (page <= 0) throw new IllegalArgumentException("Page can't be less or equal zero");
        if (economyId <= 0) throw new IllegalArgumentException("EconomyStructure must have been registered");
        ArrayList<EconomyHistoryEntry> historyEntries = new ArrayList<>();

        // Retrieve connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT * FROM minecraft.economy_history WHERE `economy_economyId` = ? LIMIT " + (page * 8) + ", " + ((page + 1) * 8) + ";"
        );

        // Set variable
        preparedStatement.setLong(1, economyId);


        // Execute statement
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through all results
        while (resultSet.next()) {
            historyEntries.add(new EconomyHistoryEntry(
                    resultSet.getString("reason"),
                    resultSet.getLong("entryDate"),
                    resultSet.getDouble("beforeAmount"),
                    resultSet.getDouble("deltaAmount")
            ));
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();
        connection.close();

        return historyEntries;
    }

    /**
     * Must run asynchronously
     *
     * @param beforeChangeAmount the amount of money before the change
     * @param deltaAmount        the amount that changed, negative for money spent and positive for money received
     * @param reason             the reason that will identify the transaction
     */
    private void registerHistory(double beforeChangeAmount, double deltaAmount, String reason) {
        try {
            // Retrieve connection
            Connection connection = LobsterCraft.dataSource.getConnection();

            // Check if structure is registered
            if (economyId == PlayerHandler.UNDEFINED_PLAYER || databaseState == DatabaseState.INSERT_TO_DATABASE)
                // Register structure before updating
                economyId = saveEconomyStructure(connection, this);

            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`economy_history` (`economy_economyId`, `reason`, `entryDate`, `beforeAmount`, `deltaAmount`) VALUES (?, ?, ?, ?, ?);"
            );

            // Set variables
            preparedStatement.setLong(1, economyId);
            preparedStatement.setString(2, reason);
            preparedStatement.setLong(3, System.currentTimeMillis());
            preparedStatement.setDouble(4, beforeChangeAmount);
            preparedStatement.setDouble(5, deltaAmount);

            // Execute and close statement
            preparedStatement.execute();
            preparedStatement.close();

            // Close connection
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static long saveEconomyStructure(@NotNull final Connection connection, @NotNull final EconomyStructure economyStructure) throws SQLException {
        boolean inserting = economyStructure.databaseState == DatabaseState.INSERT_TO_DATABASE;

        // Prepare statement
        PreparedStatement preparedStatement;
        if (inserting)
            preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`economy_structures` (`structureType`, `structureId`, `moneyAmount`) VALUES (?, ?, ?);",
                    Statement.RETURN_GENERATED_KEYS
            );
        else
            preparedStatement = connection.prepareStatement(
                    "UPDATE `minecraft`.`economy_structures` SET `structureType` = ?, `structureId` = ?, `moneyAmount` = ? WHERE `economyId` = ?;"
            );

        // Set variables
        preparedStatement.setByte(1, economyStructure.getStructureType().getTypeId());
        preparedStatement.setLong(2, economyStructure.getStructureOwnerId());
        preparedStatement.setDouble(3, economyStructure.getMoneyAmount());
        if (!inserting)
            preparedStatement.setLong(4, economyStructure.getEconomyId());

        // Execute statement
        preparedStatement.execute();

        if (inserting) {
            // Retrieve generated keys
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            if (generatedKeys.next())
                // Update structure's economyId
                economyStructure.economyId = generatedKeys.getLong("economyId");
            else
                throw new SQLException("Failed to retrieve GeneratedKey for economy structure.");

            // Close generated key
            generatedKeys.close();
        }

        // Close statement
        preparedStatement.close();

        economyStructure.databaseState = DatabaseState.ON_DATABASE;
        return economyStructure.getEconomyId();
    }

    public static EconomyStructure retrieveProfile(@NotNull final Connection connection, @NotNull final StructureType structureType, @Nullable Long structureId, boolean createNewIfNoneFound) throws SQLException {
        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT * FROM `minecraft`.`economy_structures` WHERE `structureType` = ?"
                        + (structureType.requireId() ? " AND `structureId` = ?;" : ";")
        );

        // Set variables
        preparedStatement.setByte(1, structureType.getTypeId());
        if (structureType.requireId())
            preparedStatement.setLong(2, structureId);

        // Execute statement
        ResultSet resultSet = preparedStatement.executeQuery();

        EconomyStructure economyStructure;
        // Check for results
        if (resultSet.next()) {
            Long structureIdFromDatabase = resultSet.getLong("structureId");

            // Correct structureId returned
            if (resultSet.wasNull()) {
                structureIdFromDatabase = null;
                // Don't allow null values on structures that require not null
                if (structureType.requireId()) throw new SQLException("Expected a structureId");
            }

            // Check if it matches on apply
            economyStructure = new EconomyStructure(
                    resultSet.getLong("economyId"),
                    StructureType.fromId(resultSet.getByte("structureType")),
                    structureIdFromDatabase,
                    resultSet.getDouble("moneyAmount")
            );
        } else if (createNewIfNoneFound) {
            economyStructure = new EconomyStructure(structureType, structureId);
        } else {
            economyStructure = null;
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();

        return economyStructure;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 83)
                .append(structureType.getTypeId())
                .append(structureOwnerId)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EconomyStructure && ((EconomyStructure) obj).structureType == structureType && ((EconomyStructure) obj).structureOwnerId.equals(structureOwnerId);
    }

    public enum StructureType {

        PLAYER_STRUCTURE((byte) 1, true, ConfigValue.MONEY_PLAYER_STARTING_MONEY),
        CITY_STRUCTURE((byte) 2, true, ConfigValue.MONEY_CITY_STARTING_MONEY),
        WORLD_STRUCTURE((byte) 3, false, ConfigValue.MONEY_WORLD_STARTING_MONEY);

        private final byte type;
        private final boolean requireId;
        private final double startingMoney;

        StructureType(byte type, boolean requireId, @NotNull final ConfigValue startingMoneyConfiguration) {
            this.type = type;
            this.requireId = requireId;
            this.startingMoney = LobsterCraft.config.getDouble(startingMoneyConfiguration.getPath());
        }

        public byte getTypeId() {
            return type;
        }

        public boolean requireId() {
            return requireId;
        }

        public double getStartingMoney() {
            return startingMoney;
        }

        public static StructureType fromId(byte structureTypeId) {
            for (StructureType type : values())
                if (type.getTypeId() == structureTypeId)
                    return type;
            return null;
        }
    }

    public enum MoneyReceiveResponse {
        INVALID_REASON_LENGTH,
        INVALID_AMOUNT,
        SUCCESSFULLY_RECEIVED
    }

    public enum SpentMoneyResponse {
        INVALID_REASON_LENGTH,
        INVALID_AMOUNT,
        NOT_ENOUGH_MONEY,
        SUCCESSFULLY_SPENT
    }
}
