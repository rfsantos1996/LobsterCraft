package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.custom_events.PlayerDamagePlayerEvent;
import com.jabyftw.lobstercraft.services.services_event.ServerClosingEvent;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.*;
import org.bukkit.entity.Item;
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
import java.util.Iterator;
import java.util.UUID;

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
class SafePlayerActionsListener implements Listener {

    private final HashMap<Integer, HashSet<UUID>> safeItems_player = new HashMap<>();
    private final HashSet<UUID>
            safeItems_uuid = new HashSet<>(),
            unsafeItems_uuid = new HashSet<>();

    public SafePlayerActionsListener() {
        for (World world : Bukkit.getWorlds())
            for (Item item : world.getEntitiesByClass(Item.class))
                unsafeItems_uuid.add(item.getUniqueId());
    }

    private OnlinePlayer getSafeModePlayer(@NotNull final Player player) {
        OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(player, null);
        if (onlinePlayer.isSafePlayer()) return onlinePlayer;
        else return null;
    }

    private void dropAsSafeItem(@NotNull Location location, int playerId, @Nullable ItemStack itemStack) {
        if (itemStack != null && itemStack.getType() != Material.AIR) {
            safeItems_player.putIfAbsent(playerId, new HashSet<>());
            UUID uuid = location.getWorld().dropItemNaturally(location, itemStack).getUniqueId();
            // Add to lists
            safeItems_player.get(playerId).add(uuid);
            safeItems_uuid.add(uuid);
        }
    }

    private void dropAsUnsafeItem(@NotNull Location location, @Nullable ItemStack itemStack) {
        if (itemStack != null && itemStack.getType() != Material.AIR)
            // Add to lists
            unsafeItems_uuid.add(location.getWorld().dropItemNaturally(location, itemStack).getUniqueId());
    }

    private boolean isSafeItem(@NotNull UUID uuid) {
        return safeItems_uuid.contains(uuid);
    }

    /*
     * Damage handling
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerVersusPlayer(PlayerDamagePlayerEvent event) {
        if (getSafeModePlayer(event.getPlayerDamager()) != null || getSafeModePlayer(event.getDamaged()) != null) {
            event.getPlayerDamager().sendMessage("§cNão é possível lutar com este jogador");
            event.setCancelled(true);
        }
    }

    /*
     * Item handling
     */

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerClosing(ServerClosingEvent event) {
        for (World world : Bukkit.getWorlds())
            for (Item item : world.getEntitiesByClass(Item.class))
                // Unsafe items (player drops) will be kept and re-inserted on server start up
                if (isSafeItem(item.getUniqueId())) item.remove();
    }

    /*
     * Item instance being removed
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
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
    public void onItemDespawnMonitor(ItemDespawnEvent event) {
        UUID uuid = event.getEntity().getUniqueId();

        // Remove possible uuid
        if (!unsafeItems_uuid.remove(uuid) && safeItems_uuid.remove(uuid))
            for (HashSet<UUID> uuidSet : safeItems_player.values())
                if (uuidSet.remove(uuid)) return;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onItemPickup(PlayerPickupItemEvent event) {
        OnlinePlayer onlinePlayer = getSafeModePlayer(event.getPlayer());
        UUID uuid = event.getItem().getUniqueId();

        if (onlinePlayer != null && unsafeItems_uuid.contains(uuid)) {
            event.setCancelled(true);
            LobsterCraft.logger.config("Couldn't pick up unsafe item");
        } else if (onlinePlayer == null && isSafeItem(uuid)) {
            event.setCancelled(true);
            LobsterCraft.logger.config("Couldn't pick up safe item");
        } else
            // Item is being picked up, remove from the set
            if (!unsafeItems_uuid.remove(uuid) && safeItems_uuid.remove(uuid))
                for (HashSet<UUID> uuidSet : safeItems_player.values())
                    if (uuidSet.remove(uuid)) return;
    }

    /*
     * Item instance being inserted
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onItemDrop(PlayerDropItemEvent event) {
        // Cancel creative game mode drops
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            ItemStack itemInMainHand = event.getPlayer().getInventory().getItemInMainHand();

            // Remove item from main hand (he threw it, let's remove from his hand but not drop it)
            if (itemInMainHand != null && itemInMainHand.getType() != Material.AIR) {
                event.getPlayer().getInventory().setItemInMainHand(null);
                //noinspection deprecation
                event.getPlayer().updateInventory();
            }
            // Remove item instead of cancelling the event
            event.getItemDrop().remove();
            event.setCancelled(true);
            return;
        }

        OnlinePlayer onlinePlayer;
        if ((onlinePlayer = getSafeModePlayer(event.getPlayer())) != null) {
            safeItems_player.putIfAbsent(onlinePlayer.getOfflinePlayer().getPlayerId(), new HashSet<>());
            safeItems_player.get(onlinePlayer.getOfflinePlayer().getPlayerId()).add(event.getItemDrop().getUniqueId());
            safeItems_uuid.add(event.getItemDrop().getUniqueId());
            LobsterCraft.logger.config("Dropped safe item");
        } else {
            // Common player drops should not be picked up by safe players
            unsafeItems_uuid.add(event.getItemDrop().getUniqueId());
            LobsterCraft.logger.config("Dropped unsafe item");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        OnlinePlayer safePlayer = getSafeModePlayer(event.getEntity());

        // Override death message
        event.setDeathMessage("");

        PlayerInventory inventory = event.getEntity().getInventory();
        // Set variables for the event
        event.setDroppedExp(0);
        event.setKeepInventory(false);
        event.getDrops().clear();

        if (safePlayer != null) {
            // Drop items as safe items
            for (ItemStack itemStack : inventory.getArmorContents())
                dropAsSafeItem(event.getEntity().getLocation(), safePlayer.getOfflinePlayer().getPlayerId(), itemStack);
            for (ItemStack itemStack : inventory.getContents())
                dropAsSafeItem(event.getEntity().getLocation(), safePlayer.getOfflinePlayer().getPlayerId(), itemStack);
            LobsterCraft.logger.config("Dropped safe inventory");
        } else {
            for (ItemStack itemStack : inventory.getArmorContents())
                dropAsUnsafeItem(event.getEntity().getLocation(), itemStack);
            for (ItemStack itemStack : inventory.getContents())
                dropAsUnsafeItem(event.getEntity().getLocation(), itemStack);
            LobsterCraft.logger.config("Dropped unsafe inventory");
        }

        // Clear inventory
        inventory.clear();
    }
}
