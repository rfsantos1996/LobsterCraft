package com.jabyftw.lobstercraft.player.listeners;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.custom_events.PlayerDamagePlayerEvent;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
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
public class PlayerListener implements Listener {

    private final HashMap<Long, HashSet<UUID>> safeItems = new HashMap<>();
    private final HashSet<UUID> unsafeItems = new HashSet<>();

    private boolean isSafeItem(UUID itemId) {
        for (HashSet<UUID> integers : safeItems.values())
            if (integers.contains(itemId)) return true;
        return false;
    }

    public void removeAllSafeItems() {
        for (World world : Bukkit.getWorlds())
            for (Entity entity : world.getEntities())
                if (entity.getType() == EntityType.DROPPED_ITEM && isSafeItem(entity.getUniqueId()))
                    entity.remove();
    }

    private PlayerHandler getSafePlayer(@NotNull final Player player) {
        PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler(player);
        if (playerHandler.isSafePlayer())
            return playerHandler;
        return null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPVP(PlayerDamagePlayerEvent event) {
        if (LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayerDamager()).isSafePlayer() || LobsterCraft.playerHandlerService.getPlayerHandler(event.getDamaged()).isSafePlayer()
                || event.getPlayerDamager().getGameMode() != GameMode.SURVIVAL || event.getDamaged().getGameMode() != GameMode.SURVIVAL) {
            event.getPlayerDamager().sendMessage("§cNão é possível lutar PVP com este jogador");
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemDropMonitor(PlayerDropItemEvent event) {
        // Cancel creative gamemode drops
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            if (event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() != Material.AIR) {
                event.getPlayer().setItemInHand(null);
                event.getPlayer().updateInventory();
            }
            // Remove item instead of cancelling the event
            event.getItemDrop().remove();
            //event.setCancelled(true);
            return;
        }

        PlayerHandler playerHandler;
        if ((playerHandler = getSafePlayer(event.getPlayer())) != null) {
            safeItems.putIfAbsent(playerHandler.getPlayerId(), new HashSet<>());
            safeItems.get(playerHandler.getPlayerId()).add(event.getItemDrop().getUniqueId());
        } else {
            unsafeItems.add(event.getItemDrop().getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onItemPickup(PlayerPickupItemEvent event) {
        PlayerHandler playerHandler = getSafePlayer(event.getPlayer());

        if (playerHandler != null && unsafeItems.contains(event.getItem().getUniqueId())) {
            event.setCancelled(true);
        } else if (playerHandler == null && isSafeItem(event.getItem().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDeath(PlayerDeathEvent event) {
        PlayerHandler safePlayer = getSafePlayer(event.getEntity());

        // Override death message
        event.setDeathMessage("");

        // If it is a safe player
        if (safePlayer != null) {
            // Set variables for the event
            event.setDroppedExp(0);
            event.setKeepInventory(true);
            event.getDrops().clear();

            PlayerInventory inventory = event.getEntity().getInventory();

            // Make sure dropAsSafeItem doesn't throw NullPointerException
            safeItems.putIfAbsent(safePlayer.getPlayerId(), new HashSet<>());

            // Drop items as safe items
            for (ItemStack itemStack : inventory.getArmorContents())
                dropAsSafeItem(event.getEntity().getLocation(), safePlayer.getPlayerId(), itemStack);
            for (ItemStack itemStack : inventory.getContents())
                dropAsSafeItem(event.getEntity().getLocation(), safePlayer.getPlayerId(), itemStack);
            LobsterCraft.logger.info("Dropped safe inventory");

            // Clear inventory
            inventory.clear();
        }
    }

    private void dropAsSafeItem(@NotNull Location location, long playerId, @Nullable ItemStack itemStack) {
        if (itemStack != null && itemStack.getType() != Material.AIR)
            // Set as safe item
            safeItems.get(playerId).add(location.getWorld().dropItemNaturally(location, itemStack).getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onItemMerge(ItemMergeEvent event) {
        boolean sourceIsSafe = isSafeItem(event.getEntity().getUniqueId()),
                targetIsSafe = isSafeItem(event.getTarget().getUniqueId());

        // If both are of the same type, ignore
        if ((sourceIsSafe && targetIsSafe) || (!sourceIsSafe && !targetIsSafe))
            return;

        // Cancel if there is an incompatibility
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemDespawn(ItemDespawnEvent event) {
        UUID uuid = event.getEntity().getUniqueId();

        // Remove possible uuid
        for (HashSet<UUID> uuids : safeItems.values())
            uuids.remove(uuid);
        unsafeItems.remove(uuid);
    }
}
