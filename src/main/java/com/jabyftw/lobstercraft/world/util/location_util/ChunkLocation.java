package com.jabyftw.lobstercraft.world.util.location_util;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

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
public class ChunkLocation {

    private final long worldId;
    private final int chunkX, chunkZ;

    public ChunkLocation(long worldId, int chunkX, int chunkZ) {
        this.worldId = worldId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        if (worldId <= 0) throw new IllegalArgumentException("worldId can't be less than zero!");
    }

    public ChunkLocation(@NotNull final Chunk chunk) {
        this(LobsterCraft.worldService.getIdFromWorld(chunk.getWorld()), chunk.getX(), chunk.getZ());
    }

    public static int numberOfChunksLoaded(int range) {
        if (range < 1) throw new IndexOutOfBoundsException("Range must be greater than zero!");
        int arraySize = Math.abs(-range) + range + 1;
        return arraySize * arraySize;
    }

    public World getWorld() {
        return LobsterCraft.worldService.getWorldFromId(worldId);
    }

    public long getWorldId() {
        return worldId;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    /**
     * We are getting an array of chunks centered on the given chunk location:
     * A C C
     * C X C
     * C C B
     * This would be an example with range sized at 1
     *
     * @param range the range of chunks, must be greater than 0
     * @return list of chunks, including the centered one
     */
    public Set<ChunkLocation> getNearChunks(int range) {
        // Check if the range is valid
        if (range < 1) throw new IndexOutOfBoundsException("Range must be greater than zero!");

        HashSet<ChunkLocation> chunkLocations = new HashSet<>(numberOfChunksLoaded(range));

        // As we can see in the documentation, we will get a chunk from A (C.x - range, C.z - range) to B (C.x + range, C.z + range)
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                chunkLocations.add(new ChunkLocation(worldId, chunkX + dx, chunkZ + dz));
            }
        }

        return chunkLocations;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(25, 39)
                .append(worldId)
                .append(chunkX)
                .append(chunkZ)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ChunkLocation
                && ((ChunkLocation) obj).worldId == worldId
                && ((ChunkLocation) obj).chunkX == chunkX
                && ((ChunkLocation) obj).chunkZ == chunkZ;
    }

    @Override
    public String toString() {
        return "x=" + getChunkX() + ", " +
                "z=" + getChunkZ() + ", " +
                "world=" + getWorld().getName();
    }
}
