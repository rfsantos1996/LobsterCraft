package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.custom_events.EntityDamagePlayerEvent;
import com.jabyftw.lobstercraft.player.custom_events.PlayerDamageEntityEvent;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
class TeleportListener implements Listener {

    private static TeleportBuilder.Teleport getPlayerTeleport(@NotNull Player player) {
        return LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(player, null).pendingTeleport;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamagePlayerEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((teleport = getPlayerTeleport(event.getPlayerDamaged())) != null) teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamageOther(PlayerDamageEntityEvent event) {
        TeleportBuilder.Teleport teleport;
        // This SHOULD include player versus player because PlayerDamagePlayerEvent extends PlayerDamageEntityEvent
        if ((teleport = getPlayerTeleport(event.getPlayerDamager())) != null) teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        TeleportBuilder.Teleport teleport;
        Location from = event.getFrom(), to = event.getTo();
        // Check if player changed blocks first (this is a fast check)
        if ((from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())
                // Then check if player has a teleport
                && (teleport = getPlayerTeleport(event.getPlayer())) != null)
            teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((teleport = getPlayerTeleport(event.getPlayer())) != null) teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((teleport = getPlayerTeleport(event.getPlayer())) != null) teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((teleport = getPlayerTeleport(event.getPlayer())) != null) teleport.cancel();
    }

    @EventHandler // Can't be monitor (player will be deleted)
    public void onPlayerQuit(PlayerQuitEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((teleport = getPlayerTeleport(event.getPlayer())) != null) teleport.cancel();
    }
}
