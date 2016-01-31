package com.jabyftw.lobstercraft.world.util;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.world.util.location_util.AdministratorBlockLocation;
import com.jabyftw.lobstercraft.world.util.location_util.PlayerBlockLocation;
import com.jabyftw.lobstercraft.world.util.location_util.ProtectedBlockLocation;
import com.sun.istack.internal.NotNull;
import org.bukkit.util.NumberConversions;

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
public enum ProtectionType {

    ADMIN_PROTECTION(AdministratorBlockLocation.class, LobsterCraft.config.getDouble(ConfigValue.WORLD_ADMINISTRATOR_BLOCK_SEARCH_DISTANCE.getPath()), true),
    PLAYER_PROTECTION(PlayerBlockLocation.class, LobsterCraft.config.getDouble(ConfigValue.WORLD_PLAYER_BLOCK_SEARCH_DISTANCE.getPath()), false); // All 3 dimensions

    private final Class<? extends ProtectedBlockLocation> protectedBlockLocationClass;
    private final double searchDistance, searchDistanceSquared;
    private final boolean checkOnlyXZ;

    ProtectionType(@NotNull final Class<? extends ProtectedBlockLocation> protectedBlockLocationClass, double searchDistance, boolean checkOnlyXZ) {
        this.protectedBlockLocationClass = protectedBlockLocationClass;
        this.searchDistance = searchDistance;
        this.searchDistanceSquared = NumberConversions.square(searchDistance);
        this.checkOnlyXZ = checkOnlyXZ;
    }

    public Class<? extends ProtectedBlockLocation> getProtectedBlockLocationClass() {
        return protectedBlockLocationClass;
    }

    public double getSearchDistance() {
        return searchDistance;
    }

    public double getSearchDistanceSquared() {
        return searchDistanceSquared;
    }

    public boolean checkOnlyXZ() {
        return checkOnlyXZ;
    }
}
