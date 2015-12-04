package com.jabyftw.pacocacraft.login;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.login.commands.LoginCommand;
import com.jabyftw.pacocacraft.login.commands.RegisterCommand;
import com.jabyftw.pacocacraft.util.ServerService;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.*;

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

    // Command sent to public (register is needed to be aliased on login)
    public static RegisterCommand registerCommand;

    // User profile management
    private final ConcurrentHashMap<String, ForkJoinTask<UserProfile>> userProfileRequests = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(new JoinListener(this), PacocaCraft.pacocaCraft);
        Bukkit.getServer().getPluginCommand("login").setExecutor(new LoginCommand());
        Bukkit.getServer().getPluginCommand("register").setExecutor((registerCommand = new RegisterCommand()));
    }

    @Override
    public void onDisable() {
    }

    /**
     * Search on database for UserProfile; if don't exists, create default
     *
     * @param rawPlayerName player's name
     */
    public void requestUserProfile(@NotNull String rawPlayerName) {
        final String playerName = rawPlayerName.toLowerCase();

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
     * @param playerName player's name
     */
    public void waitUserProfile(@NotNull String playerName) {
        // Wait for MySQL response and processing (quietly because major errors will be analyzed)
        userProfileRequests.get(playerName.toLowerCase()).quietlyJoin();
    }

    /**
     * Get loaded profile loaded at pre-login event and remove it from queue
     *
     * @param playerName player's name
     *
     * @return loaded/stored user profile or null if none found (but probably will throw exceptions)
     */
    public UserProfile getUserProfile(@NotNull String playerName) throws ExecutionException, InterruptedException {
        // Get UserProfile from request
        return userProfileRequests.get(playerName.toLowerCase()).get();
    }
}
