package com.jabyftw.lobstercraft.services.services_event;

import com.jabyftw.lobstercraft.world.BuildingMode;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.world.BlockLocation;
import com.jabyftw.lobstercraft.world.WorldService;
import com.sun.istack.internal.NotNull;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
public class PlayerChangesBuildingModeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final OnlinePlayer onlinePlayer;
    private final BuildingMode buildingMode;
    private final BlockLocation blockLocation;
    private final WorldService.RelativeBlockPosition blockPosition;

    private boolean
            warnPlayer = true,
            cancelled = false;

    public PlayerChangesBuildingModeEvent(@NotNull final OnlinePlayer onlinePlayer, @NotNull final BuildingMode buildingMode, @NotNull final BlockLocation blockLocation,
                                          @NotNull final WorldService.RelativeBlockPosition blockPosition) {
        this.onlinePlayer = onlinePlayer;
        this.buildingMode = buildingMode;
        this.blockLocation = blockLocation;
        this.blockPosition = blockPosition;
    }

    public OnlinePlayer getOnlinePlayer() {
        return onlinePlayer;
    }

    public BuildingMode getBuildingMode() {
        return buildingMode;
    }

    public BlockLocation getBlockLocation() {
        return blockLocation;
    }

    public WorldService.RelativeBlockPosition getBlockPosition() {
        return blockPosition;
    }

    public void warnPlayer(boolean warnPlayer) {
        this.warnPlayer = warnPlayer;
    }

    public boolean shouldWarnPlayer() {
        return warnPlayer;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
