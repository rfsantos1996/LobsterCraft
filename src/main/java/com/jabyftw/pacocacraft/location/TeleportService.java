package com.jabyftw.pacocacraft.location;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.Permissions;
import com.jabyftw.pacocacraft.util.ServerService;
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
public class TeleportService implements ServerService {

    private static final long TIME_TO_TELEPORT_TICKS = ConfigValue.TELEPORT_TIME_WAITING.<Long>getValue() * 20L;

    @Override
    public void onEnable() {
        PacocaCraft.logger.info("Enabled " + getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
    }

    public static void teleport(@NotNull PlayerHandler player, @NotNull Location location, boolean registerLastLocation, boolean forceInstantaneous) {
        // Check if player can teleport instantaneously
        if(PacocaCraft.permission.has(player.getPlayer(), Permissions.TELEPORT_INSTANTANEOUSLY) || forceInstantaneous) {
            teleportInstantaneously(player, location, registerLastLocation);
            return;
        }

        TeleportProfile teleportProfile = player.getProfile(TeleportProfile.class);

        // Schedule teleport warning player
        teleportProfile.setCurrentTeleport(BukkitScheduler.runTaskLater(PacocaCraft.pacocaCraft, () -> {
            // Teleport player if still online (it is cancelled upon quit, but check before teleporting)
            if(player.getPlayer().isOnline()) teleportInstantaneously(player, location, registerLastLocation);

            // Set as finished (remove from teleport listener)
            teleportProfile.setCurrentTeleport(null, false);
        }, TIME_TO_TELEPORT_TICKS), true);
    }

    /**
     * Teleport player to given Bukkit Location
     * TeleportProfile will be checked as this is a static method
     *
     * @param playerHandler        desired player's player handler
     * @param location             Bukkit's 'to' location
     * @param registerLastLocation if last location should be recorded
     *
     * @return if player was successfully teleported
     */
    public static boolean teleportInstantaneously(@NotNull PlayerHandler playerHandler, @NotNull Location location, boolean registerLastLocation) {
        // Check if caller wants to register last location
        if(registerLastLocation) {
            TeleportProfile profile = playerHandler.getProfile(TeleportProfile.class);

            // Check if profile was loaded before saving last location
            if(profile != null) {
                profile.setLastLocation(playerHandler.getPlayer().getLocation());
            } else {
                playerHandler.getPlayer().sendMessage("§4Ocorreu um erro:§c histórico localização não carregado!");
                return false;
            }
        }
        return playerHandler.getPlayer().teleport(location);
    }
}
