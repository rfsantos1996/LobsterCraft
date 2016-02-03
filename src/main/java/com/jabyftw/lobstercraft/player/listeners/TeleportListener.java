package com.jabyftw.lobstercraft.player.listeners;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.location.TeleportBuilder;
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
public class TeleportListener implements Listener {

    protected static TeleportBuilder.Teleport getPlayerTeleport(@NotNull Player player) {
        return LobsterCraft.playerHandlerService.getPlayerHandlerNoRestrictions(player).getPendingTeleport();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        TeleportBuilder.Teleport teleport;
        if (event.getEntity() instanceof Player && (teleport = getPlayerTeleport((Player) event.getEntity())) != null)
            teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((event.getDamager() instanceof Player && (teleport = getPlayerTeleport((Player) event.getDamager())) != null) || // Player is the damager
                (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player &&
                        (teleport = getPlayerTeleport((Player) ((Projectile) event.getDamager()).getShooter())) != null))  // Player is the shooter
            teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        TeleportBuilder.Teleport teleport;
        Location from = event.getFrom(), to = event.getTo();
        if ((from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())
                && (teleport = getPlayerTeleport(event.getPlayer())) != null)
            teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((teleport = getPlayerTeleport(event.getPlayer())) != null)
            teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((teleport = getPlayerTeleport(event.getPlayer())) != null)
            teleport.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((teleport = getPlayerTeleport(event.getPlayer())) != null)
            teleport.cancel();
    }

    @EventHandler(ignoreCancelled = true) // Can't be monitor
    public void onPlayerQuit(PlayerQuitEvent event) {
        TeleportBuilder.Teleport teleport;
        if ((teleport = getPlayerTeleport(event.getPlayer())) != null)
            teleport.cancel();
    }
}
