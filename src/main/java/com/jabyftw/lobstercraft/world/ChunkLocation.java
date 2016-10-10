package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang.builder.HashCodeBuilder;
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
class ChunkLocation implements Comparable {

    private final byte worldId;
    private final int chunkX, chunkZ;

    public ChunkLocation(byte worldId, int chunkX, int chunkZ) {
        if (worldId <= 0) throw new IllegalArgumentException("'worldId' isn't valid");
        this.worldId = worldId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public ChunkLocation(@NotNull final Chunk chunk) {
        this(LobsterCraft.servicesManager.worldService.getWorldId(chunk.getWorld()), chunk.getX(), chunk.getZ());
    }

    /**
     * We are getting an array of chunks centered on the given chunk location:<br>
     * A C C<br>
     * C X C<br>
     * C C B<br>
     * This would be an example with range sized at 1:<br>
     * A (X.x - range, X.z - range)<br>
     * B (X.x + range, X.z + range)
     *
     * @param range the range of chunks, must be greater than 0
     * @return list of chunks, including the centered one
     */
    public Set<ChunkLocation> getNearChunks(int range) {
        // Check if the range is valid
        if (range < 1) throw new IndexOutOfBoundsException("Range must be greater than zero!");
        HashSet<ChunkLocation> chunkLocations = new HashSet<>(Util.getNumberOfChunksAround(range));

        // As we can see in the documentation, we will get a chunk from A to B
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                chunkLocations.add(new ChunkLocation(worldId, chunkX + dx, chunkZ + dz));
            }
        }

        return chunkLocations;
    }

    public boolean worldIsIgnored() {
        return LobsterCraft.servicesManager.worldService.getWorld(worldId) != null;
    }

    /*
     * Getters
     */

    public World getWorld() {
        return LobsterCraft.servicesManager.worldService.getWorld(worldId);
    }

    public byte getWorldId() {
        return worldId;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(5, 17)
                .append(worldId)
                .append(chunkX)
                .append(chunkZ)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof ChunkLocation &&
                ((ChunkLocation) obj).worldId == worldId &&
                ((ChunkLocation) obj).chunkX == chunkX &&
                ((ChunkLocation) obj).chunkZ == chunkZ;
    }

    @Override
    public int compareTo(Object o) {
        return hashCode() - o.hashCode();
    }

    @Override
    public String toString() {
        return Util.appendStrings("x=", getChunkX(), ", z=", getChunkZ(), ", world='", getWorld().getName(), "'");
    }
}
