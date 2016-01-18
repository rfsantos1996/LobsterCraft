package com.jabyftw.pacocacraft.location.util;

import org.apache.commons.lang3.builder.HashCodeBuilder;

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
public class ChunkLocation {

    private final long worldId;
    private final int chunkX, chunkZ;

    public ChunkLocation(long worldId, int chunkX, int chunkZ) {
        this.worldId = worldId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
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

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof ChunkLocation &&
                ((ChunkLocation) obj).worldId == worldId &&
                ((ChunkLocation) obj).chunkX == chunkX &&
                ((ChunkLocation) obj).chunkZ == chunkZ;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 23)
                .append(worldId)
                .append(chunkX)
                .append(chunkZ)
                .toHashCode();
    }
}
