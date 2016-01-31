package com.jabyftw.lobstercraft.world.util.location_util;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;

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
public class OreBlockLocation {

    private final long playerId;
    private Location location;
    private long breakDate;

    public OreBlockLocation(@NotNull final PlayerHandler playerHandler, @NotNull final Location location) {
        this.playerId = playerHandler.getPlayerId();
        setLocation(location);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        this.breakDate = System.currentTimeMillis();
    }

    public long getBreakDate() {
        return breakDate;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof OreBlockLocation && ((OreBlockLocation) obj).location.equals(location))
                || (obj instanceof Location && location.equals(obj));
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }
}
