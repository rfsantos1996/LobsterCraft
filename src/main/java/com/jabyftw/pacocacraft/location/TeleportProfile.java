package com.jabyftw.pacocacraft.location;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.BasePlayerProfile;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.player.PlayerProfile;
import com.jabyftw.pacocacraft.player.ProfileType;
import com.jabyftw.pacocacraft.login.UserProfile;
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
    private Location lastLocation = null;

    public TeleportProfile(long playerId) {
        super(ProfileType.TELEPORT_PROFILE, playerId);
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    @Override
    public void onPlayerHandleApply(@NotNull PlayerHandler playerHandler) {
    }

    @Override
    public void onPlayerHandleDestruction() {
    }

    /**
     * Teleport player to given Bukkit Location
     * TeleportProfile will be checked as this is a static method
     *
     * @param player               desired player
     * @param location             Bukkit's 'to' location
     * @param registerLastLocation if last location should be recorded
     *
     * @return if player was successfully teleported
     */
    public static boolean teleportInstantaneously(@NotNull Player player, @NotNull Location location, boolean registerLastLocation) {
        // Check if caller wants to register last location
        if(registerLastLocation) {
            PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(player);
            TeleportProfile profile = playerHandler.getProfile(TeleportProfile.class);

            // Check if profile was loaded before saving last location
            if(profile != null) {
                profile.lastLocation = player.getLocation();
            } else {
                player.sendMessage("§4Ocorreu um erro:§c histórico localização não carregado!");
                return false;
            }
        }
        return player.teleport(location);
    }

    /**
     * Teleport player to given Storable Location
     *
     * @param player               desired player
     * @param storableLocation     'to' location
     * @param registerPastLocation if last location should be recorded
     *
     * @return if player was successfully teleported
     */
    public static boolean teleportInstantaneously(@NotNull Player player, @NotNull StorableLocation storableLocation, boolean registerPastLocation) {
        return teleportInstantaneously(player, StorableLocation.toLocation(storableLocation), registerPastLocation);
    }
}
