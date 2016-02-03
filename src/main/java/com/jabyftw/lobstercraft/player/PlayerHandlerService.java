package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.listeners.CustomEventsListener;
import com.jabyftw.lobstercraft.player.listeners.JoinListener;
import com.jabyftw.lobstercraft.player.listeners.LoginListener;
import com.jabyftw.lobstercraft.player.listeners.TeleportListener;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.Service;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
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

    // Used for stored profiles (in case player re-logs in)
    private final HashMap<String, PlayerStorage> playerHandlerStorage = new HashMap<>();
    private final Object storageLock = new Object();
    private Connection connection;
    private PlayerHandlerUnloader playerHandlerUnloader;

    @Override
    public boolean onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(new LoginListener(), LobsterCraft.lobsterCraft);
        Bukkit.getServer().getPluginManager().registerEvents(new JoinListener(), LobsterCraft.lobsterCraft);
        Bukkit.getServer().getPluginManager().registerEvents(new TeleportListener(), LobsterCraft.lobsterCraft);
        Bukkit.getServer().getPluginManager().registerEvents(new CustomEventsListener(), LobsterCraft.lobsterCraft); // handles custom events

        BukkitScheduler.runTaskTimerAsynchronously(
                playerHandlerUnloader = new PlayerHandlerUnloader(),
                0,
                LobsterCraft.config.getLong(ConfigValue.LOGIN_PROFILE_SAVING_PERIOD.toString()) // ticks
        );
        return true;
    }

    private boolean needUnloading() {
        boolean needUnloading;
        synchronized (storageLock) {
            needUnloading = !playerHandlerStorage.isEmpty();
        }
        return needUnloading;
    }

    @Override
    public void onDisable() {
        while (needUnloading())
            playerHandlerUnloader.run();
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

    public PlayerHandler getPlayerHandlerNoRestrictions(@NotNull final Player player) {
        return playerHandlers.get(player);
    }

    public PlayerHandler getPlayerHandler(@NotNull final Player player) {
        PlayerHandler playerHandler = getPlayerHandlerNoRestrictions(player);
        return playerHandler.isLoggedIn() ? playerHandler : null;
    }

    private void checkConnection() throws SQLException {
        boolean connectionInvalid = false;
        // Create connection if it is dead or something
        if (connection == null || (connectionInvalid = !connection.isValid(1))) {
            if (connectionInvalid) connection.close();
            connection = LobsterCraft.dataSource.getConnection();
        }
    }

    public void storeProfile(@NotNull PlayerHandler playerHandler) {
        synchronized (storageLock) {
            playerHandlerStorage.put(playerHandler.getPlayerName(), new PlayerStorage(playerHandler)); // This will store changed player names (success, I guess, I do not need a work-around for this)
        }
    }

    /**
     * Get PlayerHandler instance according to the newest instance: database or waitingQueue (where players are stored in case they re-log in)
     *
     * @param profileName the requested profile's name
     * @return a future task that will return the desired PlayerHandler instance
     */
    public FutureTask<PlayerHandler> requestProfile(@NotNull final String profileName) {
        return new FutureTask<>(() -> {
            String lowerCasedName = profileName.toLowerCase();

            synchronized (storageLock) {
                // Check if player was on storage
                if (playerHandlerStorage.containsKey(lowerCasedName))
                    //LobsterCraft.logger.info("Getting stored player handler => " + playerHandler.getDatabaseState().name());
                    return playerHandlerStorage.remove(lowerCasedName).playerHandler;
            }

            // Else, search player on database
            PlayerHandler playerHandler = null;
            try {
                // Make the connection if non-valid
                checkConnection();

                // Prepare statement
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.user_profiles WHERE playerName=? LIMIT 0, 1;");
                preparedStatement.setString(1, lowerCasedName);

                // Execute statement
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    String playerName = resultSet.getString("playerName");

                    // Create player handler
                    playerHandler = new PlayerHandler(
                            resultSet.getLong("playerId"),
                            playerName,
                            resultSet.getString("password"),
                            resultSet.getLong("lastTimeOnline"),
                            resultSet.getLong("playTime"),
                            resultSet.getString("lastIp")
                    );
                    //LobsterCraft.logger.info("Jogador no banco de dados: " + lowerCasedName);
                } else {
                    playerHandler = new PlayerHandler(lowerCasedName);
                    //LobsterCraft.logger.info("Novo jogador: " + lowerCasedName + " (database não contém)");
                }

                // Close everything except connection
                resultSet.close();
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return playerHandler;
        });
    }

    private class PlayerHandlerUnloader implements Runnable {

        private final long TIME_TO_SAVE_PROFILES = TimeUnit.SECONDS.toMillis(LobsterCraft.config.getLong(ConfigValue.LOGIN_PROFILE_SAVING_TIMEOUT.toString()));

        @Override
        public void run() {
            synchronized (storageLock) {
                if (!playerHandlerStorage.isEmpty()) {
                    Iterator<PlayerStorage> iterator = playerHandlerStorage.values().iterator();

                    // Iterate through all items
                    while (iterator.hasNext()) {
                        PlayerStorage playerStorage = iterator.next();

                        // Check if needs saving
                        if (Math.abs(System.currentTimeMillis() - playerStorage.timeWhenStored) > TIME_TO_SAVE_PROFILES || LobsterCraft.serverClosing) {
                            PlayerHandler playerHandler = playerStorage.playerHandler;

                            //LobsterCraft.logger.info("Database state before saving is " + playerHandler.getDatabaseState().name());

                            // Save profile if needed
                            if (playerHandler.getDatabaseState().shouldSave())
                                try {
                                    // Check for our connection
                                    checkConnection();

                                    // Save PlayerHandler instance
                                    PlayerHandler.savePlayerHandle(connection, playerHandler);
                                    //LobsterCraft.logger.info("Saved player " + playerHandler.getPlayerName());
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    continue; // Do not remove it yet
                                }

                            // Remove from list
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }

    private class PlayerStorage {

        private final PlayerHandler playerHandler;
        private final long timeWhenStored = System.currentTimeMillis();

        private PlayerStorage(@NotNull final PlayerHandler playerHandler) {
            this.playerHandler = playerHandler;
        }

    }
}
