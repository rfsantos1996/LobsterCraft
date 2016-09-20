package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.world.BlockLocation;
import com.jabyftw.lobstercraft.world.BlockProtectionType;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

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
public class BuildingMode {

    private final BlockProtectionType blockProtectionType;
    private final int protectionId;

    private long date;
    private BlockLocation blockLocation;

    public BuildingMode(@NotNull final BlockProtectionType blockProtectionType, @Nullable Integer protectionId, @NotNull final BlockLocation blockLocation) {
        this.blockProtectionType = blockProtectionType;
        this.protectionId = protectionId;
        setBlockLocation(blockLocation);
    }

    public String getDisplayName() {
        return blockProtectionType.getDisplayName();
    }

    protected BlockProtectionType getBlockProtectionType() {
        return blockProtectionType;
    }

    public int getProtectionId() {
        return protectionId;
    }

    public long getDate() {
        return date;
    }

    public BlockLocation getBlockLocation() {
        return blockLocation;
    }

    public void setBlockLocation(BlockLocation blockLocation) {
        this.blockLocation = blockLocation;
        this.date = System.currentTimeMillis();
    }

}
