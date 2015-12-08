package com.jabyftw.pacocacraft.player;

import com.jabyftw.pacocacraft.location.TeleportProfile;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

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
public class PlayerMoment {

    private final Player player;

    // Variables
    private final float saturation, exhaustion, fallDistance;
    private final boolean allowFlight, canPickupItems;
    private final int foodLevel, totalExperience, remainingAir;
    private final double health;
    private final Collection<PotionEffect> activePotionEffects;
    private final ItemStack[] inventoryContents, armorContents;
    private final Location momentLocation;

    public PlayerMoment(@NotNull Player player) {
        this.player = player;
        // Set floats
        saturation = player.getSaturation();
        exhaustion = player.getExhaustion();
        fallDistance = player.getFallDistance();

        // Set booleans
        allowFlight = player.getAllowFlight();
        canPickupItems = player.getCanPickupItems();

        // Set integers
        foodLevel = player.getFoodLevel();
        totalExperience = player.getTotalExperience();
        remainingAir = player.getRemainingAir();

        // Set others
        health = player.getHealth();
        activePotionEffects = player.getActivePotionEffects();
        momentLocation = player.getLocation();

        // Store player's inventory
        inventoryContents = player.getInventory().getContents();
        armorContents = player.getInventory().getArmorContents();
    }

    /**
     * Restore player information
     */
    public void restorePlayerMoment() {
        // Teleport player back to its pre-login location
        TeleportProfile.teleportInstantaneously(player, momentLocation, false);

        // Restore everything
        player.addPotionEffects(activePotionEffects);
        player.setFoodLevel(foodLevel);
        player.setExhaustion(exhaustion);
        player.setTotalExperience(totalExperience);
        player.setSaturation(saturation);
        player.setFallDistance(fallDistance);
        player.setHealth(health);
        player.setRemainingAir(remainingAir);

        // Set booleans
        player.setAllowFlight(allowFlight);
        player.setFlying(false); // don't care about previously flying boolean
        player.setCanPickupItems(canPickupItems);

        // Restore stored inventory if it isn't null
        player.getInventory().setContents(inventoryContents);

        // Restore armor inventory if it isn't null
        player.getInventory().setContents(armorContents);
    }

    /**
     * Clear effects, restore player's health, pending damages etc
     * NOTE: THIS WILL CLEAR PLAYER'S INVENTORY!
     */
    public static void setIdealStatus(@NotNull Player player) {
        for(PotionEffect activePotionEffect : player.getActivePotionEffects())
            player.removePotionEffect(activePotionEffect.getType());

        // Set levels
        player.setFoodLevel(20);
        player.setSaturation(0);
        player.setExhaustion(0);
        player.setTotalExperience(0);
        player.setFallDistance(0);
        player.setHealth(player.getMaxHealth());
        player.setRemainingAir(player.getMaximumAir());

        // Set booleans
        player.setCanPickupItems(false);
        // Actually allow flight if the spawn is a bit off (+1 block)
        player.setAllowFlight(true);
        player.setFlying(true);

        // Empty player inventory
        player.getInventory().clear();
    }
}
