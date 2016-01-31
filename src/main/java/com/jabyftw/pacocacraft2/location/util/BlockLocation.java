package com.jabyftw.pacocacraft2.location.util;

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
    private final byte x, z;
    private final short y; // [0, 255] (not 256)

    // Late loading variables
    private final ChunkLocation chunkLocation;

    public BlockLocation(@NotNull final ChunkLocation chunkLocation, byte x, short y, byte z) {
        this.chunkLocation = chunkLocation;

        this.x = x;
        this.z = z;
        if (y >= 256)
            throw new IllegalStateException("y can't be greater than 256 or it would collide on database.");
        this.y = y;
    }

    // block location - (chunk coordinates * 16) if negative, subtract 1
    /*public BlockLocation(long chunkId, int x, int y, int z) {
        this.chunkId = chunkId;
        this.chunkX = (x >> 4);
        this.chunkZ = (z >> 4);

        this.x = (byte) (x - (chunkX * 16) - (x < 0 ? 1 : 0)); // this will give the relative to chunk coordinates
        this.z = (byte) (z - (chunkZ * 16) - (z < 0 ? 1 : 0));
        if(y >= 256)
            throw new IllegalStateException("y can't be greater than 256 or it would collide on database.");
        this.y = (short) y; // No problems, y won't get out of range
    }*/

    public ChunkLocation getChunkLocation() {
        return chunkLocation;
    }

    public int getX() {
        return x + (chunkLocation.getChunkX() + (chunkLocation.getChunkX() < 0 ? 1 : 0)) * 16; // add one if coordinate is negative, see below
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z + (chunkLocation.getChunkZ() + (chunkLocation.getChunkZ() < 0 ? 1 : 0)) * 16;
    }

    public double distance(@NotNull final BlockLocation blockLocation) {
        return Math.sqrt(distanceSquared(blockLocation));
    }

    public double distanceSquared(@NotNull final BlockLocation blockLocation) {
        return NumberConversions.square(blockLocation.getX() - getX()) +
                NumberConversions.square(blockLocation.y - y) + // As y don't need transformation, the raw value should be fine
                NumberConversions.square(blockLocation.getZ() - getZ());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 25)
                .append(chunkLocation.hashCode())
                .append(x)
                .append(y)
                .append(z)
                .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof BlockLocation &&
                ((BlockLocation) obj).chunkLocation.equals(chunkLocation) &&
                ((BlockLocation) obj).x == x &&
                ((BlockLocation) obj).y == y &&
                ((BlockLocation) obj).z == z;
    }

    public enum BlockDatabaseState {
        ON_DATABASE,
        UPDATE_DATABASE,
        INSERT_DATABASE,
        DELETE_DATABASE,
        NOT_ON_DATABASE // do nothing
    }
}

