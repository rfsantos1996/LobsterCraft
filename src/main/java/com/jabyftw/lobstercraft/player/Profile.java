package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.util.DatabaseState;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.sql.Connection;
import java.sql.SQLException;

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
abstract class Profile {

    protected final ProfileType profileType;

    protected final int playerId;
    protected OnlinePlayer onlinePlayer = null;
    protected DatabaseState databaseState = DatabaseState.NOT_ON_DATABASE;

    /**
     * Profile is created here for a player that is logging in.<br>
     * It'll be loaded through <i>loadProfileFromDatabase(Connection)</i><br>
     * It'll be applied through <i>applyProfile(OnlinePlayer)</i><br>
     * <p>
     * It'll be stored on player quit after <i>applyProfile(null)</i>.<br>
     * If the player doesn't rejoin soon, the profile will be saved through <i>saveToDatabase(Connection)</i><br>
     * If rejoins, the profile will be reapplied through <i>applyProfile(OnlinePlayer)</i>
     *
     * @param profileType  profile type set by the class
     * @param onlinePlayer profile's owner
     */
    protected Profile(@NotNull final ProfileType profileType, @NotNull final OnlinePlayer onlinePlayer) {
        if (!onlinePlayer.getOfflinePlayer().isRegistered())
            throw new IllegalArgumentException("Player isn't registered.");
        this.profileType = profileType;
        this.onlinePlayer = onlinePlayer;
        this.playerId = onlinePlayer.getOfflinePlayer().getPlayerId();
    }

    /*
     * Parameters
     */

    /**
     * Will load the profile from database and apply it to the player at player login.
     *
     * @param connection a valid MySQL connection
     * @param <T>        profile type
     * @return this instance
     * @throws SQLException in case something goes wrong
     * @see OnlinePlayer#loginPlayer()
     */
    @SuppressWarnings("unchecked")
    public final <T extends Profile> T loadProfileFromDatabase(@NotNull final Connection connection) throws SQLException {
        onLoadingFromDatabase(connection, onlinePlayer);
        applyProfile(onlinePlayer);
        return (T) this;
    }

    /**
     * Will link the profile to given player. This is done automatically when loadProfileFromDatabase don't throw exceptions.<br>
     * Note: the player must be registered if not null.
     *
     * @param onlinePlayer player to apply the profile at. If null, profile must be stored in case the player re-joins or saved.
     */
    public void applyProfile(@Nullable final OnlinePlayer onlinePlayer) {
        // Check if OnlinePlayer is the same
        if (onlinePlayer != null && onlinePlayer.getOfflinePlayer().isRegistered() && onlinePlayer.getOfflinePlayer().getPlayerId() == playerId) {
            this.onlinePlayer = onlinePlayer;
            synchronized (onlinePlayer.profiles) {
                this.onlinePlayer.profiles.put(profileType, this);
            }
            onProfileApplication();
        } else if (onlinePlayer == null) {
            onProfileDestruction();
            synchronized (this.onlinePlayer.profiles) {
                this.onlinePlayer.profiles.remove(profileType, this);
            }
            this.onlinePlayer = null;
        } else {
            throw new IllegalArgumentException("Player isn't the same for this profile.");
        }
    }

    /**
     * Will store the profile on MySQL.
     *
     * @param connection MySQL's connection
     * @return true if successfully saved, false if SQLException was thrown
     */
    public boolean saveToDatabase(@NotNull final Connection connection) {
        try {
            return onSavingToDatabase(connection);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /*
     * Abstract methods
     */

    /**
     * This method will set variables for the profile. Will be called after Profile is created through <i>loadProfileFromDatabase(Connection)</i>.<br>
     * Remember setting Profile.databaseState
     *
     * @param connection   MySQL connection
     * @param onlinePlayer given online player
     * @throws SQLException if something goes wrong
     */
    protected abstract void onLoadingFromDatabase(@NotNull final Connection connection, @NotNull final OnlinePlayer onlinePlayer) throws SQLException;

    /**
     * This method will set variables on profile's online player instance
     */
    protected abstract void onProfileApplication();

    /**
     * This method will reset variables on profile's online player instance.<br>
     * After this method, Profile.onlinePlayer will be null.
     */
    protected abstract void onProfileDestruction();

    /**
     * This method will set variables for the profile. Will be called when Profile is saved through <i>loadProfileFromDatabase(Connection)</i>.<br>
     * Remember setting Profile.databaseState.<br>
     * Note: this method will be filtered to profiles which databaseState returns true for shouldSave()
     *
     * @param connection MySQL connection
     * @return true if data was successfully saved
     * @throws SQLException if something goes wrong
     */
    protected abstract boolean onSavingToDatabase(@NotNull final Connection connection) throws SQLException;

    /*
     * Needed methods
     */

    public OnlinePlayer getOnlinePlayer() {
        return onlinePlayer;
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    protected void setAsModified() {
        if (databaseState == DatabaseState.NOT_ON_DATABASE)
            databaseState = DatabaseState.INSERT_TO_DATABASE;
        if (databaseState == DatabaseState.ON_DATABASE)
            databaseState = DatabaseState.UPDATE_DATABASE;
        // If is DELETE_DATABASE | INSERT_DATABASE | UPDATE_DATABASE, continue DELETE_DATABASE | INSERT_DATABASE | UPDATE_DATABASE
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3, 11)
                .append(profileType)
                .append(playerId)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Profile && ((Profile) obj).profileType == profileType && ((Profile) obj).playerId == playerId;
    }

    protected enum ProfileType {

        LOCATION_PROFILE(LocationProfile.class),
        INVENTORY_PROFILE(InventoryProfile.class),
        CHAT_PROFILE(ChatProfile.class);

        private final Class<? extends Profile> profileClass;

        ProfileType(@NotNull final Class<? extends Profile> profileClass) {
            this.profileClass = profileClass;
        }

        public Class<? extends Profile> getProfileClass() {
            return profileClass;
        }

        /**
         * Retrieve profile type from given class
         *
         * @param profileClass a class that extends profile
         * @return profile type enum, if found
         */
        public static ProfileType getProfileType(@NotNull final Class<? extends Profile> profileClass) {
            for (ProfileType profileType : values())
                if (profileType.profileClass.equals(profileClass))
                    return profileType;
            return null;
        }
    }
}
