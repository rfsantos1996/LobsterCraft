package com.jabyftw.pacocacraft.player;

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
public abstract class PlayerProfile {

    // Profile type
    private final ProfileType profileType;

    // protected so its subclasses have access
    protected UserProfile userProfile = null;
    protected boolean modified = false;

    public PlayerProfile(ProfileType profileType) {
        this.profileType = profileType;
    }

    public ProfileType getProfileType() {
        return profileType;
    }

    /**
     * Check if profile was modified and if it should be saved
     *
     * @return true if profile was modified
     */
    public boolean shouldBeSaved() {
        return modified;
    }
}