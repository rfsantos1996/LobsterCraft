package com.jabyftw.lobstercraft.world.util.location_util;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.world.util.ProtectionType;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.concurrent.TimeUnit;

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
@SuppressWarnings("deprecation")
public class TemporaryProtectedBlockLocation extends ProtectedBlockLocation {

    private final static long TIME_BLOCK_IS_KEPT = TimeUnit.SECONDS.toMillis(LobsterCraft.config.getLong(ConfigValue.WORLD_TEMPORARY_PROTECTION_TIME.getPath()));

    private final int blockType;
    private final byte blockData;

    private long lastBlockChange = System.currentTimeMillis();
    private boolean hasBeenFixed = false;

    public TemporaryProtectedBlockLocation(@NotNull final Location location, @NotNull final ProtectionType protectionType) {
        super(location, protectionType);
        blockType = location.getBlock().getTypeId();
        blockData = location.getBlock().getData();
    }

    public TemporaryProtectedBlockLocation(@NotNull final ProtectedBlockLocation blockLocation) {
        super(blockLocation.getChunkLocation(), blockLocation.getType(), blockLocation.getRelativeX(), blockLocation.getY(), blockLocation.getRelativeZ(), blockLocation.getCurrentId());
        blockType = blockLocation.toLocation().getBlock().getTypeId();
        blockData = blockLocation.toLocation().getBlock().getData();
    }

    public boolean shouldBeRestored() {
        return (System.currentTimeMillis() - lastBlockChange) > TIME_BLOCK_IS_KEPT;
    }

    public TemporaryProtectedBlockLocation restoreBlock() {
        Block block = toLocation().getBlock();
        block.setTypeIdAndData(blockType, blockData, false);
        return this;
    }

    public TemporaryProtectedBlockLocation triggerChange() {
        this.lastBlockChange = System.currentTimeMillis();
        return this;
    }

    @Override
    public DatabaseState getDatabaseState() {
        return isTemporaryBlock() ? DatabaseState.NOT_ON_DATABASE : super.getDatabaseState();
    }

    @Override
    public ProtectedBlockLocation setProtectionType(@NotNull ProtectionType protectionType) {
        triggerChange();
        return super.setProtectionType(protectionType);
    }

    @Override
    public ProtectedBlockLocation setCurrentId(long currentId) {
        triggerChange();
        return super.setCurrentId(currentId);
    }

    @Override
    public boolean isTemporaryBlock() {
        return !hasBeenFixed;
    }

    public void setBlockAsFixed() {
        hasBeenFixed = true;
    }
}
