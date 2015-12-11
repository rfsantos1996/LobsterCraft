package com.jabyftw.pacocacraft.login;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.player.custom_events.PlayerDamageEntityEvent;
import com.jabyftw.pacocacraft.player.custom_events.PlayerDamagePlayerEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class LoginListener implements Listener {

    /*
     * NOTE: all priorities are set to Lowest so the events are removed already
     */

    private final static boolean FAST_MOVE_CHECK = PacocaCraft.config.getBoolean(ConfigValue.LOGIN_FAST_MOVEMENT_CHECK.getPath());

    // Allowed commands before log in
    private final static List<String> allowedCommands = PacocaCraft.config.getStringList(ConfigValue.LOGIN_BEFORE_LOGIN_ALLOWED_COMMANDS.getPath());

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent breakEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(breakEvent.getPlayer());
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() || playerHandler.isInvisible())
            breakEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent damageEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(damageEvent.getPlayer());
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() || playerHandler.isInvisible())
            damageEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockMultiPlace(BlockMultiPlaceEvent multiPlaceEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(multiPlaceEvent.getPlayer());
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() || playerHandler.isInvisible())
            multiPlaceEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent placeEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(placeEvent.getPlayer());
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() || playerHandler.isInvisible())
            placeEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent signChangeEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(signChangeEvent.getPlayer());
        // If player isn't logged on or isn't visible, he shouldn't be able to interact with blocks
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() || playerHandler.isInvisible())
            signChangeEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onHangingBreak(HangingBreakByEntityEvent hangingBreakEvent) {
        if(hangingBreakEvent.getEntity() instanceof Player) {
            PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(((Player) hangingBreakEvent.getEntity()));
            // If player isn't logged on or isn't visible, he shouldn't be able to interact with hanging items
            if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() || playerHandler.isInvisible())
                hangingBreakEvent.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerLeash(PlayerLeashEntityEvent leashEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(leashEvent.getPlayer());
        // If he isn't logged on or isn't visible, he shouldn't be able to leash entities
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() || playerHandler.isInvisible())
            leashEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerTargeted(EntityTargetLivingEntityEvent targetEvent) {
        if(targetEvent.getTarget() instanceof Player) {
            PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(((Player) targetEvent.getTarget()));
            if(!playerHandler.isDamageable())
                //targetEvent.setTarget(null);
                targetEvent.setCancelled(true); // Should be cancelled
        }
    }

    // NOTE: It isn't possible to cancel PlayerDeathEvent
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerDamaged(EntityDamageEvent damageEvent) {
        if(damageEvent.getEntity() instanceof Player) {
            PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(((Player) damageEvent.getEntity()));
            // If he isn't damageable, he shouldn't receive damage
            if(!playerHandler.isDamageable())
                damageEvent.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerDamageEvent(PlayerDamageEntityEvent damageEntityEvent) {
        if(checkDamager(damageEntityEvent.getDamaged(), PacocaCraft.getPlayerHandler(damageEntityEvent.getPlayer())))
            damageEntityEvent.setCancelled(true);
    }

    /**
     * Check damager if it is needed to cancel the event
     *
     * @param damaged damaged entity
     * @param damager player that hit the entity
     *
     * @return true if event is cancelled
     */
    private boolean checkDamager(Entity damaged, PlayerHandler damager) {
        // If damaged is not a player AND damager is logged in, allow damage (no matter if he is damageable)
        if(!(damaged instanceof Player) && damager.getProfile(UserProfile.class).isLoggedIn())
            return false;

        // This will deny not logged in players to hit stuff but still allowing not damageable players to damage entities
        return !damager.isDamageable();
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent inventoryOpenEvent) {
        // Safe cast, the only subinterface is Player
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(((Player) inventoryOpenEvent.getPlayer()));
        // If he isn't logged on, he shouldn't be able to open the inventory
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn())
            inventoryOpenEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryInteractEvent inventoryInteractEvent) {
        // Safe cast, the only subinterface is Player
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(((Player) inventoryInteractEvent.getWhoClicked()));
        // If he isn't logged on, he shouldn't be able to interact with the inventory
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn())
            inventoryInteractEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onFoodLevelChange(FoodLevelChangeEvent foodLevelChangeEvent) {
        // Safe cast, the only subinterface is Player
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(((Player) foodLevelChangeEvent.getEntity()));

        // If player isn't damageable, he should not lose food level
        if(!playerHandler.isDamageable())
            foodLevelChangeEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onEntityCombust(EntityCombustEvent combustEvent) {
        if(combustEvent.getEntity() instanceof Player) {
            PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(((Player) combustEvent.getEntity()));
            // If player isn't damageable, he should not be burned
            if(!playerHandler.isDamageable())
                combustEvent.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPortalEvent(EntityPortalEvent portalEvent) {
        if(portalEvent.getEntity() instanceof Player) {
            PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(((Player) portalEvent.getEntity()));
            // If player isn't logged in, he should not teleport
            if(!playerHandler.getProfile(UserProfile.class).isLoggedIn())
                portalEvent.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onShootBow(EntityShootBowEvent shootBowEvent) {
        if(shootBowEvent.getEntity() instanceof Player) {
            PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(((Player) shootBowEvent.getEntity()));
            // If player isn't logged in, he should not shoot bows (it is assured that if a non-logged player shoots, the projectile won't hit the damaged player)
            if(!playerHandler.getProfile(UserProfile.class).isLoggedIn())
                shootBowEvent.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onInteraction(PlayerInteractEvent interactEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(interactEvent.getPlayer());
        // If player isn't logged in, he should not interact
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn())
            interactEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onExpChange(PlayerExpChangeEvent expChangeEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(expChangeEvent.getPlayer());
        // If player isn't logged in, he should not receive exp
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn())
            expChangeEvent.setAmount(0);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent chatEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(chatEvent.getPlayer());
        // If player isn't logged in, he should not chat
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn()) // Must be synchronized or volatile
            chatEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent commandPreprocessEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(commandPreprocessEvent.getPlayer());
        // If player isn't logged in, he should not
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() && !allowedCommands.contains(commandPreprocessEvent.getMessage().split(" ")[0].replaceAll("/", "").toLowerCase()))
            commandPreprocessEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onItemDrop(PlayerDropItemEvent dropItemEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(dropItemEvent.getPlayer());
        // If player isn't logged in or invisible, he should not drop items
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() || playerHandler.isInvisible())
            dropItemEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onItemConsume(PlayerItemConsumeEvent itemConsumeEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(itemConsumeEvent.getPlayer());
        // If player isn't logged in or invisible, he should not consume items
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn())
            itemConsumeEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onItemDamage(PlayerItemDamageEvent damageEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(damageEvent.getPlayer());
        // If player isn't logged in or invisible, he should not damage items
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn())
            damageEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPickupItems(PlayerPickupItemEvent pickupItemEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(pickupItemEvent.getPlayer());
        // If player isn't logged in or invisible, he should not pickup items
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn())
            pickupItemEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent playerMoveEvent) {
        PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(playerMoveEvent.getPlayer());
        Location from = playerMoveEvent.getFrom(), to = playerMoveEvent.getTo();

        // If player isn't logged in and he changed block, cancel event and teleport player from 'to' location to 'from'
        if(!playerHandler.getProfile(UserProfile.class).isLoggedIn() && (!FAST_MOVE_CHECK || // If fast move check is false, it'll already teleport to from location
                (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()))) {
            playerMoveEvent.setCancelled(true);
            playerMoveEvent.getPlayer().teleport(from); // Do not use teleport profile as it may not be loaded
        }
    }
}
