package com.jabyftw.lobstercraft.world.util.location_util;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.world.util.ProtectionType;
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
public class PlayerBlockLocation extends ProtectedBlockLocation {

    public PlayerBlockLocation(@NotNull Location location) {
        super(location);
        this.originalId = PlayerHandler.UNDEFINED_PLAYER;
    }

    public PlayerBlockLocation(@NotNull ChunkLocation chunkLocation, byte x, short y, byte z, long ownerId) {
        super(chunkLocation, x, y, z);
        this.currentId = ownerId;
        this.originalId = ownerId;
    }

    public long getOwnerId() {
        return currentId;
    }

    public PlayerBlockLocation setOwnerId(long ownerId) {
        this.currentId = ownerId < 0 ? PlayerHandler.UNDEFINED_PLAYER : ownerId;
        return this;
    }

    @Override
    public ProtectionType getType() {
        return ProtectionType.PLAYER_PROTECTION;
    }

    @Override
    public void setUndefinedOwner() {
        setOwnerId(-1);
    }
}
