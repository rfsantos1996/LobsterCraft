package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.listeners.JoinListener;
import com.jabyftw.lobstercraft.player.listeners.LoginListener;
import com.jabyftw.lobstercraft.player.util.BannedPlayerEntry;
import com.jabyftw.lobstercraft.player.util.NameChangeEntry;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Service;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
public class PlayerHandlerService extends Service {

    // OBS: must be Concurrent as it is used on several different threads
    protected final ConcurrentHashMap<Player, PlayerHandler> playerHandlers = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, OfflinePlayerHandler> offlinePlayers = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, NameChangeEntry> nameChangeEntries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, HashSet<BannedPlayerEntry>> bannedPlayersEntries = new ConcurrentHashMap<>();

    public final List<String> blockedNames = LobsterCraft.config.getStringList(ConfigValue.LOGIN_BLOCKED_PLAYER_NAMES.toString());

    @Override
    public boolean onEnable() {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        { // Register everything
            pluginManager.registerEvents(new LoginListener(), LobsterCraft.lobsterCraft);
            pluginManager.registerEvents(new JoinListener(), LobsterCraft.lobsterCraft);
        }

        try {
            // Retrieve connection
            Connection connection = LobsterCraft.dataSource.getConnection();

            cacheOfflinePlayers(connection);
            cachePlayerNameChanges(connection);
            cachePlayerBanEntries(connection);

            // Close connection
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onDisable() {
        try {
            saveChangedPlayers();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<PlayerHandler> getOnlinePlayers() {
        HashSet<PlayerHandler> playerSet = new HashSet<>();

        // Filter logged players
        for (PlayerHandler playerHandler : playerHandlers.values()) {
            if (playerHandler.isLoggedIn())
                playerSet.add(playerHandler);
        }

        return playerSet;
    }

    public Set<PlayerHandler> getVisiblePlayers() {
        HashSet<PlayerHandler> playerSet = new HashSet<>();

        // Filter logged players
        for (PlayerHandler playerHandler : playerHandlers.values()) {
            if (playerHandler.isLoggedIn() && !playerHandler.isInvisible())
                playerSet.add(playerHandler);
        }

        return playerSet;
    }

    public PlayerHandler getPlayerHandlerNoRestrictions(@NotNull final Player player) {
        return playerHandlers.get(player);
    }

    public PlayerHandler getPlayerHandler(@NotNull final Player player) {
        PlayerHandler playerHandler = getPlayerHandlerNoRestrictions(player);
        return playerHandler.isLoggedIn() ? playerHandler : null;
    }

    public Collection<OfflinePlayerHandler> getOfflinePlayers() {
        return offlinePlayers.values();
    }

    public OfflinePlayerHandler getOfflinePlayer(String playerName) {
        OfflinePlayerHandler offlinePlayerHandler = new OfflinePlayerHandler(playerName.toLowerCase());
        OfflinePlayerHandler currentValue = offlinePlayers.putIfAbsent(playerName.toLowerCase(), offlinePlayerHandler);

        return currentValue == null ? offlinePlayerHandler : currentValue;
    }

    public OfflinePlayerHandler getOfflinePlayer(long playerId) {
        if (playerId < 0) throw new IllegalArgumentException("PlayerId can't be less than zero.");

        for (OfflinePlayerHandler playerHandler : offlinePlayers.values())
            if (playerHandler.getPlayerId() == playerId)
                return playerHandler;

        return null;
    }

    public NameChangeEntry getNameChangeEntry(long playerId) {
        return nameChangeEntries.get(playerId);
    }

    public Collection<NameChangeEntry> getNameChangeEntries() {
        return nameChangeEntries.values();
    }

    public Set<BannedPlayerEntry> getBanEntriesFromPlayer(long playerId) {
        bannedPlayersEntries.putIfAbsent(playerId, new HashSet<>());
        return bannedPlayersEntries.get(playerId);
    }

    public BanResponse kickPlayer(@NotNull OfflinePlayerHandler offlinePlayerHandler, @Nullable Long responsibleId,
                                  @NotNull final BannedPlayerEntry.BanType banType, @NotNull final String reason,
                                  @Nullable final Long bannedDuration) {

        if (!offlinePlayerHandler.isRegistered()) return BanResponse.PLAYER_NOT_REGISTERED;

        long playerId = offlinePlayerHandler.getPlayerId();
        long recordDate = System.currentTimeMillis();

        Long unbanDate;
        if (banType != BannedPlayerEntry.BanType.PLAYER_TEMPORARILY_BANNED) // Only temporary banned requires this argument
            unbanDate = null;
        else if (bannedDuration != null)
            unbanDate = recordDate + bannedDuration;
        else return BanResponse.INVALID_BAN_TYPE;

        if (!Util.checkStringLength(reason, 4, 120))
            return BanResponse.INVALID_REASON_LENGTH;

        try {
            // Retrieve connection
            Connection connection = LobsterCraft.dataSource.getConnection();

            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`ban_records` (`user_playerId`, `responsibleId`, `banType`, `recordDate`, `reason`, `unbanDate`) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );

            // Set variables
            preparedStatement.setLong(1, playerId);
            if (responsibleId == null)
                preparedStatement.setNull(2, Types.BIGINT);
            else
                preparedStatement.setLong(2, responsibleId);
            preparedStatement.setByte(3, banType.getType());
            preparedStatement.setLong(4, recordDate);
            preparedStatement.setString(5, reason);
            if (unbanDate == null)
                preparedStatement.setNull(6, Types.BIGINT);
            else
                preparedStatement.setLong(6, unbanDate);

            // Execute statement
            preparedStatement.execute();

            // Retrieve generated keys
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            // Throw error if there is no generated key
            if (!generatedKeys.next()) throw new SQLException("There is no generated key");

            // Create entry
            BannedPlayerEntry bannedPlayerEntry = new BannedPlayerEntry(
                    generatedKeys.getLong("recordId"),
                    playerId,
                    responsibleId,
                    banType,
                    reason,
                    recordDate,
                    unbanDate
            );

            // Add entry to storage
            bannedPlayersEntries.putIfAbsent(playerId, new HashSet<>());
            bannedPlayersEntries.get(playerId).add(bannedPlayerEntry);

            // Close everything
            generatedKeys.close();
            preparedStatement.close();
            connection.close();

            if (offlinePlayerHandler.isOnline())
                BukkitScheduler.runTask(() -> offlinePlayerHandler.getPlayerHandler().getPlayer().kickPlayer(bannedPlayerEntry.getKickMessage()));

            return BanResponse.SUCCESSFULLY_BANNED;
        } catch (SQLException e) {
            e.printStackTrace();
            return BanResponse.ERROR_OCCURRED;
        }
    }

    private void saveChangedPlayers() throws SQLException {
        long start = System.nanoTime();
        long numberOfPlayersUpdated = 0;

        // Retrieve connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE `minecraft`.`user_profiles` SET `playerName` = ?, `password` = ?, `lastTimeOnline` = ?, `playTime` = ?, `lastIp` = ? WHERE `playerId` = ?;"
        );

        // Iterate through all players
        for (OfflinePlayerHandler playerHandler : offlinePlayers.values())
            // Filter the ones needing updates
            if (playerHandler.getDatabaseState() == DatabaseState.UPDATE_DATABASE) {
                preparedStatement.setString(1, playerHandler.getPlayerName());
                preparedStatement.setString(2, playerHandler.getPassword());
                preparedStatement.setLong(3, playerHandler.getLastTimeOnline());
                preparedStatement.setLong(4, playerHandler.getPlayTime());
                preparedStatement.setString(5, playerHandler.getLastIp());
                preparedStatement.setLong(6, playerHandler.getPlayerId());

                // Add batch
                preparedStatement.addBatch();

                // Update their database state
                playerHandler.setDatabaseState(DatabaseState.ON_DATABASE);
                numberOfPlayersUpdated++;
            }

        // Execute and close statement
        if (numberOfPlayersUpdated > 0)
            preparedStatement.executeBatch();
        preparedStatement.close();

        // Close connection
        connection.close();

        if (numberOfPlayersUpdated > 0)
            LobsterCraft.logger.info("Took us " + new DecimalFormat("0.000").format((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)) +
                    "ms to update " + numberOfPlayersUpdated + " players.");
    }

    private void cacheOfflinePlayers(@NotNull final Connection connection) throws SQLException {
        long start = System.nanoTime();

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.user_profiles;");

        // Execute query
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through all results
        while (resultSet.next()) {
            String playerName = resultSet.getString("playerName").toLowerCase();

            // Check for the nullity of some answers
            Long economyId = resultSet.getLong("economy_economyId");
            if (resultSet.wasNull()) economyId = null;

            Long cityId = resultSet.getLong("city_cityId"); // TODO probably will be city_cityId, re-check every usage on database
            if (resultSet.wasNull()) cityId = null;

            Byte cityPositionId = resultSet.getByte("cityPositionId");
            if (resultSet.wasNull()) cityPositionId = null;

            offlinePlayers.put(
                    playerName,
                    new OfflinePlayerHandler(
                            resultSet.getLong("playerId"),
                            playerName,
                            resultSet.getString("password"),
                            economyId,
                            cityId,
                            cityPositionId,
                            resultSet.getLong("lastTimeOnline"),
                            resultSet.getLong("playTime"),
                            resultSet.getString("lastIp")
                    )
            );
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();

        LobsterCraft.logger.info("Took us " + new DecimalFormat("0.000").format((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)) +
                "ms to retrieve " + offlinePlayers.size() + " players.");
    }

    private void cachePlayerNameChanges(@NotNull final Connection connection) throws SQLException {
        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.player_name_changes;");

        // Execute query
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through all entries
        while (resultSet.next()) {
            long playerId = resultSet.getLong("user_playerId");

            nameChangeEntries.put(
                    playerId,
                    new NameChangeEntry(
                            playerId,
                            resultSet.getString("oldPlayerName"),
                            resultSet.getLong("changeDate")
                    )
            );
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();
    }

    private void cachePlayerBanEntries(@NotNull final Connection connection) throws SQLException {
        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.ban_records WHERE unbanDate IS NULL OR unbanDate > " + System.currentTimeMillis() + ";");

        // Execute query
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through all entries
        while (resultSet.next()) {
            long playerId = resultSet.getLong("user_playerId");
            Long unbanDate = resultSet.getLong("unbanDate");
            if (resultSet.wasNull()) unbanDate = null;

            bannedPlayersEntries.putIfAbsent(playerId, new HashSet<>());
            bannedPlayersEntries.get(playerId).add(
                    new BannedPlayerEntry(
                            resultSet.getLong("recordId"),
                            playerId,
                            resultSet.getLong("responsibleId"),
                            BannedPlayerEntry.BanType.getBanType(resultSet.getByte("banType")),
                            resultSet.getString("reason"),
                            resultSet.getLong("recordDate"),
                            unbanDate
                    )
            );
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();
    }

    public enum BanResponse {
        SUCCESSFULLY_BANNED,
        PLAYER_NOT_REGISTERED,
        INVALID_REASON_LENGTH,
        INVALID_BAN_TYPE,
        ERROR_OCCURRED
    }
}
