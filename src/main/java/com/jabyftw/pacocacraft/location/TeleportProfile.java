package com.jabyftw.pacocacraft.location;

import com.jabyftw.pacocacraft.player.PlayerProfile;
import com.jabyftw.pacocacraft.player.ProfileType;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
public class TeleportProfile extends PlayerProfile {

    // Player usage
    private Location lastLocation;

    public TeleportProfile() {
        super(ProfileType.TELEPORT_PROFILE);
    }

    /**
     * Teleport player to given Bukkit Location
     *
     * @param location             Bukkit's 'to' location
     * @param registerPastLocation if last location should be recorded
     *
     * @return if player was successfully teleported
     */
    public boolean teleportInstantaneously(@NotNull Location location, boolean registerPastLocation) {
        Player player = userProfile.getPlayer();
        if(registerPastLocation) this.lastLocation = player.getLocation();
        return player.teleport(location);
    }

    /**
     * Teleport player to given Storable Location
     *
     * @param storableLocation     'to' location
     * @param registerPastLocation if last location should be recorded
     *
     * @return if player was successfully teleported
     */
    public boolean teleportInstantaneously(@NotNull StorableLocation storableLocation, boolean registerPastLocation) {
        return teleportInstantaneously(StorableLocation.toLocation(storableLocation), registerPastLocation);
    }
}
