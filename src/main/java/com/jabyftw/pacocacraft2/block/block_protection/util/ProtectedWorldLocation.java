package com.jabyftw.pacocacraft2.block.block_protection.util;

import com.jabyftw.pacocacraft2.location.util.BlockLocation;
import com.jabyftw.pacocacraft2.location.util.ChunkLocation;
import com.sun.istack.internal.NotNull;

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
public class ProtectedWorldLocation extends BlockLocation {

    public static final long UNDEFINED_OWNER = -2;

    private final long originalOwnerId;
    private long ownerId = UNDEFINED_OWNER;

    public ProtectedWorldLocation(@NotNull final ChunkLocation chunkLocation, byte x, short y, byte z) {
        super(chunkLocation, x, y, z);
        this.originalOwnerId = UNDEFINED_OWNER;
    }

    public ProtectedWorldLocation(@NotNull final ChunkLocation chunkLocation, byte x, short y, byte z, long ownerId) {
        super(chunkLocation, x, y, z);
        this.originalOwnerId = ownerId;
        this.ownerId = ownerId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public ProtectedWorldLocation setOwnerId(long ownerId) {
        this.ownerId = ownerId < 0 ? UNDEFINED_OWNER : ownerId;
        return this;
    }

    public BlockDatabaseState getDatabaseState() {
        // If nothing changed
        if (originalOwnerId == ownerId && ownerId != UNDEFINED_OWNER)
            return BlockDatabaseState.ON_DATABASE;

        // If wasn't on database on start
        if (originalOwnerId == UNDEFINED_OWNER)
            if (ownerId == UNDEFINED_OWNER) // If owner still isn't anybody
                // Do not insert it
                return BlockDatabaseState.NOT_ON_DATABASE;
            else                           // If owner is somebody
                // Insert it
                return BlockDatabaseState.INSERT_DATABASE;

        // Was on database on the start: if is now undefined, delete it; else update it
        return (ownerId == UNDEFINED_OWNER ? BlockDatabaseState.DELETE_DATABASE : BlockDatabaseState.UPDATE_DATABASE);
    }
}
