package com.jabyftw.pacocacraft.block.block_protection;

import com.jabyftw.pacocacraft.location.util.ChunkLocation;
import com.sun.istack.internal.NotNull;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

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
public class ChunkController {

    // Note: size, addAll, retainAll, containsAll, equals and toArray are not atomic
    private final ConcurrentLinkedDeque<ChunkLocation> pendingLoadings = new ConcurrentLinkedDeque<>();

    // TODO loading queue => get the chunk, load its Id [can be made with single statement] and create the list for the blocks, which will be loaded later
    // TODO map of stored chunks, list of blocks
    // TODO methods to get near blocks from a location
    // TODO methods to load chunks near the requested chunk (minimum: all 8 around the central one, get a method to get the chunks near based on a layer and queue response time, minimum chunks being 1 "range")

    protected ChunkController() {
    }

    /**
     * We are getting an array of chunks centered on the given chunk location:
     * A C C
     * C X C
     * C C B
     * This would be an example with range sized at 1
     *
     * @param chunkLocation given chunk location to the search be centered around
     * @param range         the range of chunks, must be greater than 0
     *
     * @return array of chunks, including the centered one
     */
    protected ChunkLocation[] getNearChunks(@NotNull final ChunkLocation chunkLocation, int range) {
        // Check if the range is valid
        if(range < 1) throw new IndexOutOfBoundsException("Range must be greater than zero!");

        // Variables
        int index = 0;
        ChunkLocation[] chunkLocations = new ChunkLocation[numberOfChunksLoaded(range)];

        // As we can see in the documentation, we will get a chunk from A (C.x - range, C.z - range) to B (C.x + range, C.z + range)
        for(int dx = -range; dx <= range; dx++) {
            for(int dz = -range; dz <= range; dz++) {
                chunkLocations[index++] = new ChunkLocation(chunkLocation.getWorldId(), chunkLocation.getChunkX() + dx, chunkLocation.getChunkZ() + dz);
            }
        }

        return chunkLocations;
    }

    protected int numberOfChunksLoaded(int range) {
        if(range < 1) throw new IndexOutOfBoundsException("Range must be greater than zero!");
        int arraySize = Math.abs(-range) + range + 1;
        return arraySize * arraySize;
    }
}
