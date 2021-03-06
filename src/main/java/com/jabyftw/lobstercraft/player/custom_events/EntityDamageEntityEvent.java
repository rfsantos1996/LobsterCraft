package com.jabyftw.lobstercraft.player.custom_events;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Copyright (C) 2015  Rafael Sartori for LobsterCraft Plugin
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
public class EntityDamageEntityEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    // Constants
    private final Entity damaged;
    private final Entity damager;
    private final EntityDamageByEntityEvent event;

    private final ProjectileSource shooter;

    // Variables
    private boolean cancelled;

    public EntityDamageEntityEvent(@NotNull Entity damaged, @NotNull Entity damager, @NotNull EntityDamageByEntityEvent event, @Nullable ProjectileSource shooter) {
        this.damaged = damaged;
        this.damager = damager;
        this.event = event;
        this.shooter = shooter;
    }

    public EntityDamageEntityEvent(@NotNull Entity damaged, @NotNull Entity damager, @NotNull EntityDamageByEntityEvent event) {
        this(damaged, damager, event, null);
    }

    public Entity getDamaged() {
        return damaged;
    }

    public Entity getDamager() {
        return damager;
    }

    public EntityDamageByEntityEvent getEvent() {
        return event;
    }

    public ProjectileSource getShooter() {
        return shooter;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
