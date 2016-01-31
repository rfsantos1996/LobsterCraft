package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.Service;
import com.sun.istack.internal.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

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
public class ProfileService extends Service {

    private final HashMap<Long, ProfileStorage> playerProfiles = new HashMap<>();
    private final Object profilesLock = new Object();
    private ProfileUnloader profileUnloader;
    private Connection connection;

    @Override
    public boolean onEnable() {
        BukkitScheduler.runTaskTimerAsynchronously(
                profileUnloader = new ProfileUnloader(),
                0,
                LobsterCraft.config.getLong(ConfigValue.LOGIN_PROFILE_SAVING_PERIOD.toString()) // ticks
        );
        return true;
    }

    private boolean needUnloading() {
        boolean needUnloading;
        synchronized (profilesLock) {
            needUnloading = !playerProfiles.isEmpty();
        }
        return needUnloading;
    }

    @Override
    public void onDisable() {
        while (needUnloading())
            profileUnloader.run();
    }

    public void storeProfiles(long playerId, @NotNull final Collection<Profile> profileCollection) {
        synchronized (profilesLock) {
            playerProfiles.put(playerId, new ProfileStorage(profileCollection));
        }
    }

    private void checkConnection() throws SQLException {
        boolean connectionInvalid = false;
        // Create connection if it is dead or something
        if (connection == null || (connectionInvalid = !connection.isValid(1))) {
            if (connectionInvalid) connection.close();
            connection = LobsterCraft.dataSource.getConnection();
        }
    }

    /**
     * Retrieve player profiles. This will search on database if not found on storage.
     * This must be ran asynchronously as it'll probably search on database
     *
     * @param playerId player's id to search for
     * @return null, if something went wrong; a set of profiles, otherwise
     */
    public FutureTask<Set<Profile>> retrieveProfiles(long playerId) {
        return new FutureTask<>((Callable<Set<Profile>>) () -> {
            synchronized (profilesLock) {
                ProfileStorage profileStorage = playerProfiles.get(playerId);
                // If stored profile wasn't null
                if (profileStorage != null)
                    return profileStorage.profileTypes;
            }

            HashSet<Profile> profiles = new HashSet<>();
            checkConnection();

            // Get all profiles listening to every error
            try {
                for (ProfileType profileType : ProfileType.values())
                    profiles.add(profileType.getProfile(connection, playerId));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            return profiles;
        });
    }

    private class ProfileUnloader implements Runnable {

        private final long TIME_TO_SAVE_PROFILES = TimeUnit.SECONDS.toMillis(LobsterCraft.config.getLong(ConfigValue.LOGIN_PROFILE_SAVING_TIMEOUT.toString()));

        @Override
        public void run() {
            synchronized (profilesLock) {
                // If storage isn't empty
                if (!playerProfiles.isEmpty()) {
                    Iterator<ProfileStorage> iterator = playerProfiles.values().iterator();
                    try {
                        // Make sure connection is fine
                        checkConnection();

                        // Iterate through all items
                        while (iterator.hasNext()) {
                            ProfileStorage profileStorage = iterator.next();

                            // If time has exceed the time to remove the profiles, save them
                            if (Math.abs(System.currentTimeMillis() - profileStorage.timeWhenStored) > TIME_TO_SAVE_PROFILES || LobsterCraft.serverClosing) {
                                // Save all needed saving profiles (this should ben asynchronous)
                                for (Profile profile : profileStorage.profileTypes) {
                                    if (profile.getDatabaseState().shouldSave())
                                        profile.getProfileType().saveProfile(connection, profile);
                                }

                                // Remove the profiles from storage
                                iterator.remove();
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class ProfileStorage {

        protected final HashSet<Profile> profileTypes = new HashSet<>();
        protected final long timeWhenStored = System.currentTimeMillis();

        public ProfileStorage(@NotNull final Collection<Profile> profileCollection) {
            profileTypes.addAll(profileCollection);
        }
    }
}
