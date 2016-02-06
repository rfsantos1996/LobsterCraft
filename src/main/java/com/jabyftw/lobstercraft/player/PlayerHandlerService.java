package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.listeners.CustomEventsListener;
import com.jabyftw.lobstercraft.player.listeners.JoinListener;
import com.jabyftw.lobstercraft.player.listeners.LoginListener;
import com.jabyftw.lobstercraft.player.listeners.TeleportListener;
import com.jabyftw.lobstercraft.player.util.NameChangeEntry;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Service;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public final List<String> blockedNames = LobsterCraft.config.getStringList(ConfigValue.LOGIN_BLOCKED_PLAYER_NAMES.toString());

    @Override
    public boolean onEnable() {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        { // Register everything
            pluginManager.registerEvents(new LoginListener(), LobsterCraft.lobsterCraft);
            pluginManager.registerEvents(new JoinListener(), LobsterCraft.lobsterCraft);
            pluginManager.registerEvents(new TeleportListener(), LobsterCraft.lobsterCraft);
            pluginManager.registerEvents(new CustomEventsListener(), LobsterCraft.lobsterCraft); // handles custom events
        }

        try {
            // Retrieve connection
            Connection connection = LobsterCraft.dataSource.getConnection();

            cacheOfflinePlayers(connection);
            cachePlayerNameChanges(connection);

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

            offlinePlayers.put(
                    playerName,
                    new OfflinePlayerHandler(
                            resultSet.getLong("playerId"),
                            playerName,
                            resultSet.getString("password"),
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
}
