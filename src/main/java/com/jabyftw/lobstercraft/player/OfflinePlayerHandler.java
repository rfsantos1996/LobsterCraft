package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.economy.EconomyHistoryEntry;
import com.jabyftw.lobstercraft.economy.EconomyStructure;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.world.city.CityPosition;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.sql.*;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
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
public class OfflinePlayerHandler {

    protected PlayerHandler playerHandler = null;

    // Database information
    private long playerId = PlayerHandler.UNDEFINED_PLAYER;
    private volatile String playerName;
    private volatile String password = null;
    private volatile Long economyId = null;
    private volatile Long cityId = null;
    private CityPosition cityPosition;
    private long lastTimeOnline = 0;
    private long playTime = 0;
    private String lastIp = null;

    protected volatile DatabaseState databaseState = DatabaseState.NOT_ON_DATABASE;

    /**
     * Constructor for generation of a new player
     *
     * @param playerName logging in player's name
     */
    public OfflinePlayerHandler(@NotNull final String playerName) {
        this.playerName = playerName.toLowerCase();
    }

    public OfflinePlayerHandler(final long playerId, @NotNull final String playerName, @NotNull final String password,
                                @Nullable final Long economyId, @Nullable final Long cityId, @Nullable final Byte cityPositionId,
                                final long lastTimeOnline, final long playTime, @NotNull final String lastIp) {
        this.playerId = playerId;
        this.playerName = playerName.toLowerCase();
        this.password = password;
        this.economyId = economyId;
        this.cityId = cityId;
        this.cityPosition = cityPositionId == null ? null : CityPosition.fromId(cityPositionId);
        this.lastTimeOnline = lastTimeOnline;
        this.playTime = playTime;
        this.lastIp = lastIp;
        this.databaseState = DatabaseState.ON_DATABASE;
    }

    public long getPlayerId() {
        return playerId;
    }

    /**
     * This should run asynchronously as it'll update database
     *
     * @throws SQLException
     */
    public void registerPlayerId(@NotNull final String encryptedPassword, String lastIp) throws SQLException {
        // Check for overwrites
        if (this.playerId != PlayerHandler.UNDEFINED_PLAYER)
            throw new IllegalStateException("Can't overwrite defined playerId");

        // Set necessary variables for login
        this.lastIp = lastIp;
        this.password = encryptedPassword;

        this.playerId = insertPlayerHandle(this);

        // Update state
        this.databaseState = DatabaseState.ON_DATABASE;
    }

    public String getPlayerName() {
        return playerName;
    }

    /**
     * This should run asynchronously as it'll update database
     *
     * @param playerName new player name
     * @throws SQLException
     */
    public void setPlayerName(@NotNull final String playerName) throws SQLException {
        ConcurrentHashMap<String, OfflinePlayerHandler> offlinePlayers = LobsterCraft.playerHandlerService.offlinePlayers;

        if (!isRegistered())
            throw new IllegalStateException("Can't change a player name of an unregistered player");

        if (!offlinePlayers.remove(playerName, this))
            throw new IllegalStateException("Couldn't remove OfflinePlayer " + this.playerName + " from storage to update its player name.");

        this.playerName = playerName.toLowerCase();
        offlinePlayers.put(playerName, this);
        setAsModified();

        updatePlayerHandle(this);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(@NotNull final String encryptedPassword) {
        checkUnregisteredProfile();
        this.password = encryptedPassword;
    }

    public boolean isRegistered() {
        return password != null && getPlayerId() >= 0;
    }

    public boolean isOnline() {
        return playerHandler != null;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    public boolean hasEconomyStructure() {
        return economyId != null && economyId > 0;
    }

    public Long getEconomyId() {
        return economyId;
    }

    public Collection<EconomyHistoryEntry> getEconomyHistory(int page) throws SQLException {
        return EconomyStructure.getHistory(economyId, page);
    }

    public boolean hasCity() {
        return cityId != null && cityId > 0 && cityPosition != null;
    }

    public Long getCityId() {
        return cityId;
    }

    // TODO get CityStructure using Lobstercraft#cityService

    public CityPosition getCityPosition() {
        return cityPosition;
    }

    public long getLastTimeOnline() {
        return lastTimeOnline;
    }

    public void setLastTimeOnline(long lastTimeOnline) {
        checkUnregisteredProfile();
        this.lastTimeOnline = lastTimeOnline;
    }

    public long getPlayTime() {
        return playTime;
    }

    public void addPlayTime(long timeInMillis) {
        checkUnregisteredProfile();
        this.playTime += timeInMillis;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(@NotNull final String lastIp) {
        this.lastIp = lastIp;
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    public void setDatabaseState(@NotNull final DatabaseState databaseState) {
        this.databaseState = databaseState;
    }

    private void setAsModified() {
        //if (databaseState == DatabaseState.NOT_ON_DATABASE)
        //databaseState = DatabaseState.INSERT_TO_DATABASE;
        if (databaseState == DatabaseState.ON_DATABASE)
            databaseState = DatabaseState.UPDATE_DATABASE;
    }

    private void checkUnregisteredProfile() {
        if (!isRegistered())
            throw new IllegalStateException("Can't change an unregistered player profile.");
        setAsModified();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof OfflinePlayerHandler && playerId == ((OfflinePlayerHandler) obj).playerId && playerName.equals(((OfflinePlayerHandler) obj).playerName);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 17)
                .append(playerName)
                .append(playerId)
                .toHashCode();
    }

    private static long insertPlayerHandle(@NotNull OfflinePlayerHandler offlinePlayer) throws SQLException {
        // Retrieve connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO `minecraft`.`user_profiles` (`playerName`, `password`, `economy_economyId`, `city_cityId`, `cityPositionId`, `lastTimeOnline`, `playTime`, `lastIp`) VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
                Statement.RETURN_GENERATED_KEYS
        );

        // Set variables
        preparedStatement.setString(1, offlinePlayer.getPlayerName().toLowerCase()); // Lower case it just to make sure
        preparedStatement.setString(2, offlinePlayer.getPassword());
        if (offlinePlayer.hasEconomyStructure()) { // As much as an unregistered player may not have an economy structure, I'll check so nothing gets loose
            preparedStatement.setLong(3, offlinePlayer.getEconomyId());
        } else {
            preparedStatement.setNull(3, Types.BIGINT);
        }
        if (offlinePlayer.hasCity()) { // Same as economy structure
            preparedStatement.setLong(4, offlinePlayer.getCityId());
            preparedStatement.setByte(5, offlinePlayer.getCityPosition().getPositionId());
        } else {
            preparedStatement.setNull(4, Types.BIGINT);
            preparedStatement.setNull(5, Types.TINYINT);
        }
        preparedStatement.setLong(6, offlinePlayer.getLastTimeOnline());
        preparedStatement.setLong(7, offlinePlayer.getPlayTime());
        preparedStatement.setString(8, offlinePlayer.getLastIp());

        // Execute statement
        preparedStatement.execute();

        // Retrieve generated keys
        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

        if (!generatedKeys.next())
            throw new SQLException("Query didn't return any generated key");

        long playerId = generatedKeys.getLong("playerId");

        // Close everything
        generatedKeys.close();
        preparedStatement.close();
        connection.close();

        return playerId;
    }

    private static void updatePlayerHandle(@NotNull OfflinePlayerHandler offlinePlayer) throws SQLException {
        // Retrieve connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE `minecraft`.`user_profiles` SET `playerName` = ?, `password` = ?, `economy_economyId` = ?, `city_cityId` = ?," +
                        " `cityPositionId` = ?, `lastTimeOnline` = ?, `playTime` = ?, `lastIp` = ? WHERE `playerId` = ?;"
        );

        // Set up variables
        preparedStatement.setString(1, offlinePlayer.getPlayerName().toLowerCase()); // Lower case it just to make sure
        preparedStatement.setString(2, offlinePlayer.getPassword());

        if (offlinePlayer.hasEconomyStructure()) {
            preparedStatement.setLong(3, offlinePlayer.getEconomyId());
        } else {
            preparedStatement.setNull(3, Types.BIGINT);
        }

        if (offlinePlayer.hasCity()) {
            preparedStatement.setLong(4, offlinePlayer.getCityId());
            preparedStatement.setByte(5, offlinePlayer.getCityPosition().getPositionId());
        } else {
            preparedStatement.setNull(4, Types.BIGINT);
            preparedStatement.setNull(5, Types.TINYINT);
        }
        preparedStatement.setLong(6, offlinePlayer.getLastTimeOnline());
        preparedStatement.setLong(7, offlinePlayer.getPlayTime());
        preparedStatement.setString(8, offlinePlayer.getLastIp());
        preparedStatement.setLong(9, offlinePlayer.getPlayerId());

        // Execute and close statement
        preparedStatement.execute();
        preparedStatement.close();
        connection.close();

        // Set database state
        offlinePlayer.setDatabaseState(DatabaseState.ON_DATABASE);
    }
}
