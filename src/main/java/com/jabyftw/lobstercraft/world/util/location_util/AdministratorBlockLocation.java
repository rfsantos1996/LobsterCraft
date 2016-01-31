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
public class AdministratorBlockLocation extends ProtectedBlockLocation {

    public AdministratorBlockLocation(@NotNull final AdministratorBlockLocation playerProtectedBlockLocation) {
        super(playerProtectedBlockLocation.getChunkLocation(), playerProtectedBlockLocation.getRelativeX(), playerProtectedBlockLocation.getY(), playerProtectedBlockLocation.getRelativeZ());
        this.originalId = playerProtectedBlockLocation.originalId;
        this.currentId = playerProtectedBlockLocation.currentId;
    }

    public AdministratorBlockLocation(@NotNull final Location location) {
        super(location);
        this.originalId = PlayerHandler.UNDEFINED_PLAYER;
    }

    public AdministratorBlockLocation(@NotNull final ChunkLocation chunkLocation, byte x, short y, byte z, long constructionId) {
        super(chunkLocation, x, y, z);
        this.originalId = constructionId;
        this.currentId = constructionId;
        if (constructionId <= 0) throw new IllegalArgumentException("Construction ID must be greater than 0.");
    }

    public long getConstructionId() {
        return currentId;
    }

    public AdministratorBlockLocation setConstructionId(long constructionId) {
        this.currentId = constructionId <= 0 ? PlayerHandler.UNDEFINED_PLAYER : constructionId;
        return this;
    }

    @Override
    public ProtectionType getType() {
        return ProtectionType.ADMIN_PROTECTION;
    }

    @Override
    public void setUndefinedOwner() {
        setConstructionId(-1);
    }
}
