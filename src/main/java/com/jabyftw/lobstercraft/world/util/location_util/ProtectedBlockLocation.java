package com.jabyftw.lobstercraft.world.util.location_util;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.world.util.ProtectionType;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;

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
public abstract class ProtectedBlockLocation extends BlockLocation {

    protected long originalId, currentId;

    public ProtectedBlockLocation(@NotNull final ChunkLocation chunkLocation, byte x, short y, byte z) {
        super(chunkLocation, x, y, z);
    }

    public ProtectedBlockLocation(@NotNull final BlockLocation blockLocation) {
        super(blockLocation);
    }

    public ProtectedBlockLocation(@NotNull final Location location) {
        super(location);
    }

    public abstract ProtectionType getType();

    public abstract void setUndefinedOwner();

    public final DatabaseState getDatabaseState() {
        // If nothing changed and it isn't undefined => it is still on database
        if (this.originalId == currentId && currentId != PlayerHandler.UNDEFINED_PLAYER)
            return DatabaseState.ON_DATABASE;

        // If wasn't on database on the first place
        if (originalId == PlayerHandler.UNDEFINED_PLAYER)
            // Still doesn't have an owner
            if (currentId == PlayerHandler.UNDEFINED_PLAYER)
                return DatabaseState.NOT_ON_DATABASE;
            else
                return DatabaseState.INSERT_TO_DATABASE;

        // Since original wasn't undefined, checks if the one is undefined (delete) or defined (should update)
        return currentId == PlayerHandler.UNDEFINED_PLAYER ? DatabaseState.DELETE_FROM_DATABASE : DatabaseState.UPDATE_DATABASE;
    }

    public final void setOnDatabase() {
        this.originalId = currentId;
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
