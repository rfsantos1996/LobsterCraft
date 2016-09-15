package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import org.bukkit.util.NumberConversions;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
public enum ProtectionType {

    PLAYER_PROTECTION(
            (byte) 0,
            LobsterCraft.configuration.getDouble(ConfigurationValues.WORLD_PROTECTION_PLAYER_DISTANCE.toString()),
            LobsterCraft.configuration.getBoolean(ConfigurationValues.WORLD_PROTECTION_PLAYER_CHECK_Y.toString())
    ),
    ADMIN_PROTECTION(
            (byte) 1,
            LobsterCraft.configuration.getDouble(ConfigurationValues.WORLD_PROTECTION_ADMIN_DISTANCE.toString()),
            LobsterCraft.configuration.getBoolean(ConfigurationValues.WORLD_PROTECTION_ADMIN_CHECK_Y.toString())
    ),
    CITY_HOUSES_PROTECTION(
            (byte) 2,
            LobsterCraft.configuration.getDouble(ConfigurationValues.WORLD_PROTECTION_CITY_MAX_DISTANCE_FROM_CENTER.toString()),
            LobsterCraft.configuration.getBoolean(ConfigurationValues.WORLD_PROTECTION_CITY_CHECK_Y.toString())
    );

    private final byte id;
    private final double searchDistance, searchDistanceSquared;
    private final boolean checkY;

    ProtectionType(byte id, double searchDistance, boolean checkY) {
        this.id = id;
        this.searchDistance = searchDistance;
        this.searchDistanceSquared = NumberConversions.square(searchDistance);
        this.checkY = checkY;
    }

    public byte getTypeId() {
        return id;
    }

    public double getSearchDistance() {
        return searchDistance;
    }

    public double getSearchDistanceSquared() {
        return searchDistanceSquared;
    }

    public boolean checkY() {
        return checkY;
    }

    public static ProtectionType[] getPriorityOrder() {
        return new ProtectionType[]{
                ADMIN_PROTECTION,
                CITY_HOUSES_PROTECTION,
                PLAYER_PROTECTION
        };
    }

    public static ProtectionType getFromId(int id) {
        for (ProtectionType type : values())
            if (type.id == id) return type;
        return null;
    }
}
