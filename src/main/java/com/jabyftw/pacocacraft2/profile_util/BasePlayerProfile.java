package com.jabyftw.pacocacraft2.profile_util;

import com.jabyftw.pacocacraft2.player.PlayerService;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

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
public abstract class BasePlayerProfile {

    // Profile basic information
    private final ProfileType profileType;
    // protected so its subclasses have access
    protected volatile DatabaseState databaseState = DatabaseState.NOT_ON_DATABASE; // Volatile because it is used asynchronously on UserProfile and I don't want to synchronize every method
    protected long storedSince = -1;
    private PlayerHandler playerHandler = null;

    public BasePlayerProfile(ProfileType profileType) {
        this.profileType = profileType;
    }

    public static DatabaseState setDatabaseState(DatabaseState currentState) {
        return currentState == DatabaseState.ON_DATABASE ? DatabaseState.UPDATE_DATABASE : DatabaseState.INSERT_DATABASE;
    }

    /**
     * Abstract method executed when PlayerHandle apply this profile (after this player handler is not null)
     */
    public abstract void onPlayerHandleApply(@NotNull PlayerHandler playerHandler);

    /**
     * Abstract method executed when PlayerHandle is destroyed (after this player handler is null)
     */
    public abstract void onPlayerHandleDestruction();

    /**
     * Check if profile was modified and if it should be saved
     *
     * @return true if profile was modified
     */
    public boolean shouldBeSaved() {
        return databaseState == DatabaseState.UPDATE_DATABASE || databaseState == DatabaseState.INSERT_DATABASE;
    }

    protected void setDatabaseState() {
        this.databaseState = this.databaseState == DatabaseState.ON_DATABASE ? DatabaseState.UPDATE_DATABASE : DatabaseState.INSERT_DATABASE;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    protected void setPlayerHandler(@Nullable PlayerHandler playerHandler) {
        // Check for state exceptions
        if ((playerHandler != null && this.playerHandler != null) || (playerHandler == null && this.playerHandler == null))
            throw new IllegalStateException("Player handler being set wrongly!");

        // Call abstract methods
        if (playerHandler != null) {
            // Apply before
            this.playerHandler = playerHandler;
            onPlayerHandleApply(playerHandler);
        } else {
            onPlayerHandleDestruction();
            // Apply after
            this.playerHandler = null;
        }
    }

    public ProfileType getProfileType() {
        return profileType;
    }

    public long getStoredSince() {
        return storedSince;
    }

    /**
     * Set the date that the profile was stored (used to check its lifetime on PlayerHandlerService)
     *
     * @param storedSince the date of storage; -1 means it isn't stored
     * @see PlayerService#storeProfile(PlayerProfile) for more information
     */
    public void setStoredSince(long storedSince) {
        this.storedSince = storedSince;
    }
}
