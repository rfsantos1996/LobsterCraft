package com.jabyftw.pacocacraft.player;

import com.jabyftw.pacocacraft.login.UserProfile;
import com.sun.istack.internal.NotNull;

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
    private PlayerHandler playerHandler = null;

    // protected so its subclasses have access
    protected boolean modified = false;

    public BasePlayerProfile(ProfileType profileType) {
        this.profileType = profileType;
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
        return modified;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    protected void setPlayerHandler(PlayerHandler playerHandler) {
        // Check for state exceptions
        if((playerHandler != null && this.playerHandler != null) || (playerHandler == null && this.playerHandler == null))
            throw new IllegalStateException("Player handler being set wrongly!");

        // Call abstract methods
        if(playerHandler != null) {
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
}
