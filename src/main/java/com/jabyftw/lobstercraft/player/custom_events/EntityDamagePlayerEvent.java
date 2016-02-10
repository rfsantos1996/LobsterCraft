package com.jabyftw.lobstercraft.player.custom_events;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
public class EntityDamagePlayerEvent extends EntityDamageEntityEvent {

    private static final HandlerList handlers = new HandlerList();

    public EntityDamagePlayerEvent(@NotNull Player damaged, @NotNull Entity damager, @NotNull EntityDamageByEntityEvent event, @Nullable ProjectileSource shooter) {
        super(damaged, damager, event, shooter);
    }

    public EntityDamagePlayerEvent(@NotNull Player damaged, @NotNull Entity damager, @NotNull EntityDamageByEntityEvent event) {
        this(damaged, damager, event, null);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayerDamaged() {
        return getDamaged();
    }

    @Override
    public Player getDamaged() {
        return (Player) super.getDamaged();
    }
}
