package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.player.inventory.InventoryProfile;
import com.jabyftw.lobstercraft.player.location.LocationProfile;
import com.sun.istack.internal.NotNull;

import java.sql.Connection;

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
@SuppressWarnings("unchecked")
public enum ProfileType {

    INVENTORY_PROFILE(InventoryProfile.class) {
        @Override
        public <T extends Profile> T getProfile(@NotNull final Connection connection, final long playerId) throws Exception {
            return (T) InventoryProfile.retrieveProfile(connection, playerId);
        }

        @Override
        public <T extends Profile> boolean saveProfile(@NotNull final Connection connection, final T profile) {
            return InventoryProfile.saveProfile(connection, (InventoryProfile) profile);
        }
    },
    LOCATION_PROFILE(LocationProfile.class) {
        @Override
        public <T extends Profile> T getProfile(@NotNull Connection connection, long playerId) throws Exception {
            return (T) LocationProfile.retrieveProfile(connection, playerId);
        }

        @Override
        public <T extends Profile> boolean saveProfile(@NotNull Connection connection, T profile) {
            return LocationProfile.saveProfile(connection, (LocationProfile) profile);
        }
    },;

    private final Class<? extends Profile> profileClass;

    ProfileType(@NotNull final Class<? extends Profile> profileClass) {
        this.profileClass = profileClass;
    }

    /**
     * Get the profile type from given profile. ProfileType must be registered or it'll throw an IllegalStateException!
     *
     * @param profile profile to search type for
     * @param <T>     must extend Profile
     * @return the profile type
     * @throws IllegalStateException if profile isn't registered
     */
    public static <T extends Profile> ProfileType getType(@NotNull final T profile) throws IllegalStateException {
        for (ProfileType profileType : values()) {
            if (profileType.profileClass.isAssignableFrom(profile.getClass()))
                return profileType;
        }

        throw new IllegalStateException("Profile isn't registered: " + profile.getClass().getSimpleName());
    }

    public static <T extends Profile> ProfileType getType(@NotNull final Class<T> profileClass) {
        for (ProfileType profileType : values())
            if (profileType.profileClass.isAssignableFrom(profileClass))
                return profileType;
        return null;
    }

    /**
     * Retrieve the profile from database or search for a new one.
     *
     * @param connection provided connection (for performance issues)
     * @param playerId   given profile Id to search for
     * @param <T>        given profile must extends our implementation
     * @return the profile loaded
     * @throws Exception
     */
    public abstract <T extends Profile> T getProfile(@NotNull final Connection connection, long playerId) throws Exception;

    /**
     * Save the profile to database. Just occur when the profile has a database state that needs saving.
     * This is executed after player handler is destroyed and profile was queued for a while.
     * All profiles are deleted afterwards.
     *
     * @param connection provided connection
     * @param profile    given profile
     * @param <T>        profile must extend our implementation
     * @return true if the profile was successfully saved
     */
    public abstract <T extends Profile> boolean saveProfile(@NotNull final Connection connection, T profile);
}
