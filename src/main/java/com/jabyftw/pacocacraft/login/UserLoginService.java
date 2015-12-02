package com.jabyftw.pacocacraft.login;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.login.commands.LoginCommand;
import com.jabyftw.pacocacraft.player.UserProfile;
import com.jabyftw.pacocacraft.util.ServerService;
import com.sun.istack.internal.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * Copyright (C) 2015  Rafael Sartori for PacocaCraft Plugin
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
public class UserLoginService implements ServerService {

    // User profile management
    private final ConcurrentHashMap<String, ForkJoinTask<UserProfile>> userProfileRequests = new ConcurrentHashMap<>();
    // TODO stored user profiles: set last time online on UserProfile and check its lifetime (if passed some time, save it and remove from cache)
    private final ConcurrentHashMap<String, UserProfile> storedUserProfiles = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        PacocaCraft.server.getPluginManager().registerEvents(new PlayerListener(this), PacocaCraft.pacocaCraft);
        PacocaCraft.server.getPluginCommand("login").setExecutor(new LoginCommand());
    }

    @Override
    public void onDisable() {
    }

    /**
     * Search on database for user profile, if don't exists, create default
     *
     * @param playerName player's name (caller should make sure this is lower cased)
     */
    public void requestProfile(@NotNull String playerName) {
        // Ignore for stored user profiles
        if(storedUserProfiles.containsKey(playerName)) return;

        // Obtain user profile from MySQL asynchronously (preferably, there are other plugins)
        ForkJoinTask<UserProfile> forkJoinTask = ForkJoinPool.commonPool().submit(() -> {
            // Create default (empty) profile
            UserProfile userProfile = null;

            try {
                // Get connection from pool and execute query
                Connection connection = PacocaCraft.dataSource.getConnection();
                {
                    // Prepare statement arguments
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            ""
                    );

                    // Execute statement
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if(!resultSet.next())
                        // If player doesn't exists, return default profile
                        return new UserProfile(playerName);

                    // Retrieve information
                    long playerId = resultSet.getLong("playerId");
                    String password = resultSet.getString("password");
                    long lastTimeOnline = resultSet.getLong("lastTimeOnline");

                    // Apply information to profile on a "loaded profile" constructor
                    userProfile = new UserProfile(playerName, playerId, password, lastTimeOnline);

                    // Close ResultSet and PreparedStatement
                    resultSet.close();
                    preparedStatement.close();
                }
                // Close connection and return profile
                connection.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }
            // Return profile. If null, it'll be analyzed later and player will be kicked for safety
            return userProfile;
        });
        userProfileRequests.put(playerName, forkJoinTask);
    }

    /**
     * Wait profile for arrival (join thread)
     *
     * @param playerName player's name (caller should make sure this is lower cased)
     */
    public void awaitProfile(@NotNull String playerName) {
        // Ignored for stored user profiles
        if(storedUserProfiles.containsKey(playerName)) return;

        // Wait for MySQL response and processing (quietly because major errors will be analyzed)
        userProfileRequests.get(playerName).quietlyJoin();
    }

    /**
     * Get loaded profile loaded at pre-login event and remove it from queue
     *
     * @param playerName player's name (caller should make sure this is lower cased)
     *
     * @return loaded (or default, if not found) user profile
     */
    public UserProfile getProfile(@NotNull String playerName) throws ExecutionException, InterruptedException {
        // Return stored or get from request
        return storedUserProfiles.getOrDefault(playerName, userProfileRequests.remove(playerName).get());
    }
}
