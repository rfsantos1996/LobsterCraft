package com.jabyftw.pacocacraft.block.xray_protection;

import com.sun.istack.internal.NotNull;
import org.bukkit.Location;

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
public class OreLocation {

    private final long playerId;
    private Location location;
    private long date;

    public OreLocation(long playerId, @NotNull Location location) {
        this.playerId = playerId;
        setLocation(location);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        date = System.currentTimeMillis();
    }

    public long getDate() {
        return date;
    }

    @Override
    public boolean equals(Object object) {
        return object != null && ((object instanceof OreLocation && ((OreLocation) object).location.equals(location)) || (object instanceof Location && object.equals(location)));
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }
}
