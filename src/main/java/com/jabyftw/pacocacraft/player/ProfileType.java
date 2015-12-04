package com.jabyftw.pacocacraft.player;

import com.jabyftw.pacocacraft.location.TeleportProfile;
import com.jabyftw.pacocacraft.login.UserProfile;

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
public enum ProfileType {

    USER_PROFILE(UserProfile.class),
    TELEPORT_PROFILE(TeleportProfile.class),;

    private final Class<? extends BasePlayerProfile> profileClass;

    /**
     * NOTE: Login profile will be part of UserProfile
     *
     * @param profileClass profile class for future lookup at UserProfile
     *
     * @see PlayerHandler#applyProfile(BasePlayerProfile)
     */
    ProfileType(Class<? extends BasePlayerProfile> profileClass) {
        this.profileClass = profileClass;
    }

    public Class<? extends BasePlayerProfile> getProfileClass() {
        return profileClass;
    }

    public static ProfileType getProfileType(Class<? extends BasePlayerProfile> profileClass) {
        for(ProfileType profileType : ProfileType.values()) {
            if(profileType.getProfileClass().equals(profileClass))
                return profileType;
        }
        throw new NullPointerException("Profile isn't set on ProfileType Enum");
    }
}
