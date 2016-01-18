package com.jabyftw.pacocacraft.location.util;

import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.util.NumberConversions;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
public class BlockLocation {

    // Database variables
    private final long chunkId;
    private final byte x, z;
    private final short y; // [0, 255] (not 256)
    private long ownerId = -1;

    // Late loading variables
    private final int chunkX, chunkZ;

    public BlockLocation(long chunkId, byte x, short y, byte z, int chunkX, int chunkZ) {
        this.chunkId = chunkId;
        this.x = x;
        this.z = z;
        if(y >= 256)
            throw new IllegalStateException("y can't be greater than 256 or it would collide on database.");
        this.y = y;

        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    // block location - (chunk coordinates * 16) if negative, subtract 1
    public BlockLocation(long chunkId, int x, int y, int z) {
        this.chunkId = chunkId;
        this.chunkX = (x >> 4);
        this.chunkZ = (z >> 4);

        this.x = (byte) (x - (chunkX * 16) - (x < 0 ? 1 : 0)); // this will give the relative to chunk coordinates
        this.z = (byte) (z - (chunkZ * 16) - (z < 0 ? 1 : 0));
        if(y >= 256)
            throw new IllegalStateException("y can't be greater than 256 or it would collide on database.");
        this.y = (short) y; // No problems, y won't get out of range
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getX() {
        return x + chunkX + (chunkX < 0 ? 1 : 0); // add one if coordinate is negative, see below
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z + chunkZ + (chunkZ < 0 ? 1 : 0);
    }

    public double distance(@NotNull final BlockLocation blockLocation) {
        return Math.sqrt(distanceSquared(blockLocation));
    }

    public double distanceSquared(@NotNull final BlockLocation blockLocation) {
        return NumberConversions.square(blockLocation.x - x) + NumberConversions.square(blockLocation.y - y) + NumberConversions.square(blockLocation.z - z);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 25)
                .append(chunkId)
                .append(x)
                .append(y)
                .append(z)
                .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof BlockLocation &&
                ((BlockLocation) obj).chunkId == chunkId &&
                ((BlockLocation) obj).x == x &&
                ((BlockLocation) obj).y == y &&
                ((BlockLocation) obj).z == z;
    }
}

