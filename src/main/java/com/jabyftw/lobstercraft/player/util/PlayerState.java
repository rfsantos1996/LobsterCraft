package com.jabyftw.lobstercraft.player.util;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.location.TeleportBuilder;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

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
public class PlayerState {

    private final PlayerHandler playerHandler;
    private final Player player;

    // Player variables
    private final Location playerLocation;
    private final Collection<PotionEffect> activePotionEffects;
    private final ItemStack[] armorContents, contents;
    private final boolean allowFlight, flying, canPickupItems, healthScaled;
    private final int totalExperience, foodLevel, remainingAir, maximumAir, level, fireTicks, noDamageTicks;
    private final float exhaustion, saturation, fallDistance, exp;
    private final double health, healthScale, maxHealth;

    public PlayerState(@NotNull PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
        this.player = playerHandler.getPlayer();

        // Store stuff
        // Location
        playerLocation = player.getLocation();
        // Potion effects
        activePotionEffects = player.getActivePotionEffects();
        // Inventory
        armorContents = player.getInventory().getArmorContents();
        contents = player.getInventory().getContents();
        // booleans
        allowFlight = player.getAllowFlight();
        flying = player.isFlying();
        canPickupItems = player.getCanPickupItems();
        healthScaled = player.isHealthScaled();
        // integers
        totalExperience = player.getTotalExperience();
        foodLevel = player.getFoodLevel();
        remainingAir = player.getRemainingAir();
        maximumAir = player.getMaximumAir();
        level = player.getLevel();
        fireTicks = player.getFireTicks();
        noDamageTicks = player.getNoDamageTicks();
        // floats
        exhaustion = player.getExhaustion();
        saturation = player.getSaturation();
        fallDistance = player.getFallDistance();
        exp = player.getExp();
        // double
        health = player.getHealth();
        healthScale = player.getHealthScale();
        maxHealth = player.getMaxHealth();
    }

    public void restorePlayerState() {
        // Teleport player
        TeleportBuilder.getBuilder(playerHandler)
                .setLocation(playerLocation)
                .setInstantaneousTeleport(true)
                .execute();
        // Restore potion effects
        player.addPotionEffects(activePotionEffects);
        // Restore inventory
        player.getInventory().setArmorContents(armorContents);
        player.getInventory().setContents(contents);
        // restore booleans
        player.setAllowFlight(allowFlight);
        player.setFlying(flying);
        player.setCanPickupItems(canPickupItems);
        player.setHealthScaled(healthScaled);
        // restore integers
        player.setTotalExperience(totalExperience);
        player.setFoodLevel(foodLevel);
        player.setRemainingAir(remainingAir);
        player.setMaximumAir(maximumAir);
        player.setLevel(level);
        player.setFireTicks(fireTicks);
        player.setNoDamageTicks(noDamageTicks);
        // restore floats
        player.setExhaustion(exhaustion);
        player.setSaturation(saturation);
        player.setFallDistance(fallDistance);
        player.setExp(exp);
        // restore doubles
        player.setHealth(health);
        player.setHealthScale(healthScale);
        player.setMaxHealth(maxHealth);
    }

    public void clearPlayer() {
        // Restore some variables
        player.setFoodLevel(20);
        player.setSaturation(0);
        player.setExhaustion(0);
        player.setTotalExperience(0);
        player.setFallDistance(0);
        // Restore health and air
        player.setHealthScaled(false);
        player.setMaxHealth(20);
        player.setHealth(player.getMaxHealth());
        player.setMaximumAir(10);
        player.setRemainingAir(player.getMaximumAir());
        // Remove potion effects
        for (PotionEffect potionEffect : player.getActivePotionEffects())
            player.removePotionEffect(potionEffect.getType());
        // Clear inventory
        player.getInventory().clear();
    }
}
