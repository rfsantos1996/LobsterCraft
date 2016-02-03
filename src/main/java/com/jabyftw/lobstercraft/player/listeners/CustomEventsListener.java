package com.jabyftw.lobstercraft.player.listeners;

import com.jabyftw.lobstercraft.player.custom_events.EntityDamageEntityEvent;
import com.jabyftw.lobstercraft.player.custom_events.EntityDamagePlayerEvent;
import com.jabyftw.lobstercraft.player.custom_events.PlayerDamageEntityEvent;
import com.jabyftw.lobstercraft.player.custom_events.PlayerDamagePlayerEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.projectiles.ProjectileSource;

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
public class CustomEventsListener implements Listener {

    /**
     * Check damage for custom events (PlayerDamagePlayerEvent, PlayerDamageEntityEvent, EntityDamagePlayerEvent, EntityDamageEntityEvent)
     * It'll look up for the final entity cause
     *
     * @param event entity damage entity event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        Event customEvent;

        if (event.getEntity() instanceof Player) {                // Something against player (?)

            if (event.getDamager() instanceof Player) {           // Player against player (contact)
                customEvent = new PlayerDamagePlayerEvent(
                        (Player) event.getEntity(),
                        (Player) event.getDamager(),
                        event
                );
            } else if (event.getDamager() instanceof Projectile) {
                ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();

                if (shooter instanceof Player) {                   // Player against player (projectile)
                    customEvent = new PlayerDamagePlayerEvent(
                            (Player) event.getEntity(),
                            event.getDamager(),
                            event,
                            (Player) shooter
                    );
                } else {                                          // Entity against player (projectile)
                    customEvent = new EntityDamagePlayerEvent(
                            (Player) event.getEntity(),
                            event.getDamager(),
                            event,
                            shooter
                    );
                }
            } else {                                              // Entity against player (contact)
                customEvent = new EntityDamagePlayerEvent(
                        (Player) event.getEntity(),
                        event.getDamager(),
                        event
                );
            }

        } else if (event.getDamager() instanceof Player) {        // Player against entity (contact)
            customEvent = new PlayerDamageEntityEvent(
                    event.getEntity(),
                    (Player) event.getDamager(),
                    event
            );

        } else if (event.getDamager() instanceof Projectile) {   // Something against entity (projectile)

            ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();
            if (shooter instanceof Player) {                     // Player against entity (projectile)
                customEvent = new PlayerDamageEntityEvent(
                        event.getEntity(),
                        event.getDamager(),
                        event,
                        (Player) shooter
                );
            } else {                                            // Entity against entity (projectile)
                customEvent = new EntityDamageEntityEvent(
                        event.getEntity(),
                        event.getDamager(),
                        event,
                        shooter
                );
            }

        } else {                                                // Entity against entity (contact)
            customEvent = new EntityDamageEntityEvent(
                    event.getEntity(),
                    event.getDamager(),
                    event
            );
        }

        // Call event
        pluginManager.callEvent(customEvent);

        // Cancel event if requested
        if (((Cancellable) customEvent).isCancelled())
            event.setCancelled(true);
    }
}
