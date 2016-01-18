package com.jabyftw.pacocacraft.login;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.login.commands.ChangePasswordCommand;
import com.jabyftw.pacocacraft.login.commands.ChangeUsernameCommand;
import com.jabyftw.pacocacraft.login.commands.LoginCommand;
import com.jabyftw.pacocacraft.login.commands.RegisterCommand;
import com.jabyftw.pacocacraft.player.PlayerService;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.ServerService;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
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
    private final ConcurrentHashMap<String, UserProfile> userProfileMap = new ConcurrentHashMap<>();
    private UserProfileSavingTask profilesSavingTask;
    private BukkitTask storedProfilesTask;

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginCommand("login").setExecutor(new LoginCommand());
        Bukkit.getServer().getPluginCommand("changepass").setExecutor(new ChangePasswordCommand());
        Bukkit.getServer().getPluginCommand("changeuser").setExecutor(new ChangeUsernameCommand());
        Bukkit.getServer().getPluginCommand("register").setExecutor((registerCommand = new RegisterCommand()));

        Bukkit.getServer().getPluginManager().registerEvents(new JoinListener(this), PacocaCraft.pacocaCraft);
        Bukkit.getServer().getPluginManager().registerEvents(new LoginListener(), PacocaCraft.pacocaCraft);

        storedProfilesTask = BukkitScheduler.runTaskTimerAsynchronously(
                (profilesSavingTask = new UserProfileSavingTask()),
                PlayerService.TIME_BETWEEN_PROFILE_SAVES_TICKS,
                PlayerService.TIME_BETWEEN_PROFILE_SAVES_TICKS
        );

        PacocaCraft.logger.info("Enabled " + getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
        storedProfilesTask.cancel();
        // Force run if server is closing
        while(!userProfileMap.isEmpty()) {
            PacocaCraft.logger.info("Trying to save all user profiles; If stuck, tell developer");
            profilesSavingTask.run();
        }
    }

    /**
     * Search on database for UserProfile; if don't exists, create default
     *
     * @param rawPlayerName player's name
     */
    public void requestUserProfile(@NotNull String rawPlayerName) {
        final String playerName = rawPlayerName.toLowerCase();

        // Check if UserProfile isn't stored
        if(userProfileMap.containsKey(playerName))
            return;

        // Obtain user profile from MySQL asynchronously (preferably, there are other plugins)
        ForkJoinTask<UserProfile> forkJoinTask = ForkJoinPool.commonPool().submit(() -> {
            UserProfile userProfile = null;

            // Try to fetch user profile (erros will be caught by ForkJoinTask)
            userProfile = UserProfile.fetchUserProfile(playerName);

            // If none found, create default
            if(userProfile == null)
                return new UserProfile(playerName);

            // Return profile. If null, it'll be analyzed later and player will be kicked for safety
            return userProfile;
        });
        userProfileRequests.put(playerName, forkJoinTask);
    }

    /**
     * Wait profile for arrival (join thread)
     *
     * @param rawPlayerName player's name
     */
    public void waitUserProfile(@NotNull String rawPlayerName) {
        String playerName = rawPlayerName.toLowerCase();

        // Check if UserProfile isn't stored
        if(userProfileMap.containsKey(playerName))
            return;

        ForkJoinTask<UserProfile> joinTask = userProfileRequests.get(playerName);
        if(joinTask != null)
            try {
                // Wait for MySQL response and processing
                joinTask.join();
            } catch(Exception e) {
                e.printStackTrace();
                PacocaCraft.logger.warning("Couldn't fetch profile for " + rawPlayerName);
            }
    }

    /**
     * Get loaded profile loaded at pre-login event and remove it from queue
     *
     * @param rawPlayerName player's name
     *
     * @return loaded/stored user profile or null if none found (but probably will throw exceptions)
     */
    public UserProfile getUserProfile(@NotNull String rawPlayerName) throws ExecutionException, InterruptedException {
        String playerName = rawPlayerName.toLowerCase();

        // Get UserProfile from request or storage
        UserProfile userProfile = userProfileMap.getOrDefault(playerName, userProfileRequests.get(playerName).get());

        // Set profile as not stored
        userProfile.setStoredSince(-1);

        return userProfile;
    }

    /**
     * Store profile until player needs it or it's lifetime passes waiting
     * <b>NOTE:</b> it'll be saved (if needed) asynchronously
     *
     * @param userProfile desired user profile to be stored
     */
    public void storeProfile(@NotNull UserProfile userProfile) {
        userProfile.setStoredSince(System.currentTimeMillis());
        userProfileMap.put(userProfile.getPlayerName().toLowerCase(), userProfile);
    }

    protected class UserProfileSavingTask implements Runnable {

        private final long PROFILE_LIFETIME_MILLIS = TimeUnit.SECONDS.toMillis(PacocaCraft.config.getLong(ConfigValue.LOGIN_PROFILE_WAITING_TIME.getPath()));

        @Override
        public void run() {
            Iterator<UserProfile> iterator = userProfileMap.values().iterator();

            // Iterate while have next
            while(iterator.hasNext()) {
                UserProfile profile = iterator.next();

                // Check if profile passed its lifetime or server is closing
                if((System.currentTimeMillis() - profile.getStoredSince()) >= PROFILE_LIFETIME_MILLIS || PacocaCraft.isServerClosing()) {

                    // Save profile if needed
                    if(profile.shouldBeSaved())
                        try {
                            UserProfile.saveUserProfile(profile);
                        } catch(SQLException e) {
                            e.printStackTrace();
                            PacocaCraft.logger.warning("Failed to save profile " + profile.getPlayerName());
                        }

                    // Remove from map
                    iterator.remove();
                }
            }
        }
    }
}
