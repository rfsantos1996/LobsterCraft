package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.custom_events.PlayerDamageEntityEvent;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.List;

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
@SuppressWarnings("DefaultAnnotationParam")
class PreLoginListener implements Listener {

    // Some configurations
    private final List<String> allowedCommands = LobsterCraft.configuration.getStringList(ConfigurationValues.LOGIN_PRE_SIGN_IN_ALLOWED_COMMANDS.toString());
    private final boolean FAST_MOVE_CHECK = LobsterCraft.configuration.getBoolean(ConfigurationValues.LOGIN_USE_FAST_MOVEMENT_EVENT_CHECK.toString());

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if (!onlinePlayer.isLoggedIn() || onlinePlayer.isInvisible())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if (!playerHandler.isLoggedIn() || playerHandler.isInvisible())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if (!playerHandler.isLoggedIn() || playerHandler.isInvisible())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if (!playerHandler.isLoggedIn() || playerHandler.isInvisible())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if (!playerHandler.isLoggedIn() || playerHandler.isInvisible())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getEntity(), null);
            // If player isn't logged on or isn't visible, he shouldn't be able to interact with hanging items
            if (!playerHandler.isLoggedIn() || playerHandler.isInvisible())
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerLeash(PlayerLeashEntityEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If he isn't logged on or isn't visible, he shouldn't be able to leash entities
        if (!playerHandler.isLoggedIn() || playerHandler.isInvisible())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerTargeted(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player) {
            OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getTarget(), null);
            if (!playerHandler.isDamageable())
                //event.setTarget(null);
                event.setCancelled(true); // Should be cancelled
        }
    }

    // NOTE: It isn't possible to cancel PlayerDeathEvent
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getEntity(), null);
            // If he isn't damageable, he shouldn't receive damage
            if (!playerHandler.isDamageable())
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerDamageEvent(PlayerDamageEntityEvent event) {
        if (checkDamager(event.getDamaged(), LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayerDamager(), null)))
            event.setCancelled(true);
    }

    /**
     * Check damager if it is needed to cancel the event
     *
     * @param damaged damaged entity
     * @param damager player that hit the entity
     * @return true if event is cancelled
     */
    private boolean checkDamager(@NotNull final Entity damaged, @NotNull final OnlinePlayer damager) {
        // If damaged is not a player AND damager is logged in, allow damage (no matter if he is damageable)
        if (!(damaged instanceof Player) && damager.isLoggedIn())
            return false;

        // This will deny not logged in players to hit stuff but still allowing not damageable players to damage entities
        return !damager.isDamageable();
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        // Safe cast, the only sub-interface is Player
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getPlayer(), null);
        // If he isn't logged on, he shouldn't be able to open the inventory
        if (!playerHandler.isLoggedIn())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryInteractEvent event) {
        // Safe cast, the only sub-interface is Player
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getWhoClicked(), null);
        // If he isn't logged on, he shouldn't be able to interact with the inventory
        if (!playerHandler.isLoggedIn())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        // Safe cast, the only subinterface is Player
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getEntity(), null);

        // If player isn't damageable, he should not lose food level
        if (!playerHandler.isDamageable())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onEntityCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Player) {
            OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getEntity(), null);
            // If player isn't damageable, he should not be burned
            if (!playerHandler.isDamageable())
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPortalEvent(EntityPortalEvent event) {
        if (event.getEntity() instanceof Player) {
            OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getEntity(), null);
            // If player isn't logged in, he should not teleport
            if (!playerHandler.isLoggedIn())
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getEntity(), null);
            // If player isn't logged in, he should not shoot bows (it is assured that if a non-logged player shoots, the projectile won't hit the damaged player)
            if (!playerHandler.isLoggedIn())
                event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onInteraction(PlayerInteractEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged in, he should not interact
        if (!playerHandler.isLoggedIn())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onExpChange(PlayerExpChangeEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged in, he should not receive exp
        if (!playerHandler.isLoggedIn())
            event.setAmount(0);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged in, he should not chat
        if (!playerHandler.isLoggedIn()) // Must be synchronized or volatile
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged in, he should not
        if (!playerHandler.isLoggedIn() && !allowedCommands.contains(event.getMessage().split(" ")[0].replaceAll("/", "").toLowerCase()))
            event.setCancelled(true);
        //LobsterCraft.logger.info("Player " + event.getPlayer().getName() + " can't use command: not logged in");
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged in or invisible, he should not drop items
        if (!playerHandler.isLoggedIn() || playerHandler.isInvisible())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged in or invisible, he should not consume items
        if (!playerHandler.isLoggedIn())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onItemDamage(PlayerItemDamageEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged in or invisible, he should not damage items
        if (!playerHandler.isLoggedIn())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPickupItems(PlayerPickupItemEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        // If player isn't logged in or invisible, he should not pickup items
        if (!playerHandler.isLoggedIn())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);
        Location from = event.getFrom(), to = event.getTo();

        // If player isn't logged in and he changed block, cancel event and teleport player from 'to' location to 'from'
        if (!playerHandler.isLoggedIn() && (!FAST_MOVE_CHECK || // If fast move check is false, it'll already teleport to from location
                (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()))) {
            event.setCancelled(true);
            event.getPlayer().teleport(from); // Do not use teleport profile as it may not be loaded
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        OnlinePlayer playerHandler = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), null);

        // Check if player protection was loaded
        if (!playerHandler.isLoggedIn() || playerHandler.isInvisible())
            event.setCancelled(true);
    }
}
