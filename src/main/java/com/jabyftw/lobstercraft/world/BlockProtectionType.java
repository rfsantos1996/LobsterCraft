package com.jabyftw.lobstercraft.world;

import com.sun.istack.internal.NotNull;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.util.NumberConversions;

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
enum BlockProtectionType {

    CITY_BLOCKS((byte) 0, 60.0D, false, "blocos da cidade") {
        @Override
        public double getMaximumDistanceBetweenCities() {
            return 1500.0D;
        }

        @Override
        public double getMinimumDistanceBetweenCities() {
            return 150.0D;
        }

        @Override
        public double getProtectionRange(byte cityLevel) {
            return Math.min(60.0D + NumberConversions.ceil(8.78D * (cityLevel - 1)), getMinimumDistanceBetweenCities()); // max protection range (level 10) will be 140 blocks
        }
    },
    CITY_HOUSES((byte) 1, 20.0D, true, "casa da cidade"),
    PLAYER_BLOCKS((byte) 2, 18.75D, true, "jogador"),
    ADMINISTRATOR_BLOCKS((byte) 3, 170.0D, false, "administrador");

    private final byte typeId;
    private final double protectionDistance, protectionDistanceSquared;
    private final boolean checkY;
    private final String displayName;

    BlockProtectionType(byte typeId, double protectionDistance, boolean checkY, @NotNull final String displayName) {
        this.typeId = typeId;
        this.protectionDistance = protectionDistance;
        this.protectionDistanceSquared = NumberConversions.square(protectionDistance);
        this.checkY = checkY;
        this.displayName = displayName;
    }

    public byte getTypeId() {
        return typeId;
    }

    public double getProtectionDistance() {
        return protectionDistance;
    }

    public double getProtectionDistanceSquared() {
        return protectionDistanceSquared;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double protectionDistance(@NotNull final BlockLocation blockLocation, @NotNull final BlockLocation protectionCenter) {
        return protectionDistance(blockLocation, protectionCenter, checkY);
    }

    public double protectionDistanceSquared(@NotNull final BlockLocation blockLocation, @NotNull final BlockLocation protectionCenter) {
        return protectionDistanceSquared(blockLocation, protectionCenter, checkY);
    }

    /*
     * Exclusive methods for some
     */

//    public double _() {
//        throw new NotImplementedException();
//    }

    public double getMaximumDistanceBetweenCities() {
        throw new NotImplementedException();
    }

    public double getMinimumDistanceBetweenCities() {
        throw new NotImplementedException();
    }

    public double getProtectionRange(byte cityLevel) {
        throw new NotImplementedException();
    }

    public double getProtectionRangeSquared(byte cityLevel) {
        return NumberConversions.square(getProtectionRange(cityLevel));
    }

    /*
     * Static methods
     */

    public static BlockProtectionType valueOf(byte type) {
        for (BlockProtectionType blockProtectionType : values())
            if (blockProtectionType.typeId == type)
                return blockProtectionType;
        return null;
    }

    public static double protectionDistance(@NotNull final BlockLocation blockLocation, @NotNull final BlockLocation protectionCenter, boolean checkY) {
        return checkY ? blockLocation.distance(protectionCenter) : blockLocation.distanceXZ(protectionCenter);
    }

    public static double protectionDistanceSquared(@NotNull final BlockLocation blockLocation, @NotNull final BlockLocation protectionCenter, boolean checkY) {
        return checkY ? blockLocation.distanceSquared(protectionCenter) : blockLocation.distanceXZSquared(protectionCenter);
    }

//    WORLD_PROTECTION_PLAYER_DISTANCE("world.protection.player.protection_distance", 13.0D),
//    WORLD_PROTECTION_PLAYER_CHECK_Y("world.protection.player.protection_check_y_axis", true),
//    WORLD_PROTECTION_ADMIN_DISTANCE("world.protection.admin.protection_distance", 150.0D),
//    WORLD_PROTECTION_ADMIN_CHECK_Y("world.protection.admin.protection_check_y_axis", false),
//    WORLD_PROTECTION_CITY_MAX_DISTANCE_FROM_CENTER("world.protection.city.protection_distance", 125.0D),
//    WORLD_PROTECTION_CITY_CHECK_Y("world.protection.city.protection_check_y_axis", false),
//    CITY_DISTANCE_BETWEEN_CITIES_CHECK_Y("city.check_y_distance_between_cities", false),
//    CITY_MINIMUM_DISTANCE_BETWEEN_CITIES("city.minimum_distance_between_cities", 150.0D),
//    CITY_MAXIMUM_DISTANCE_BETWEEN_CITIES("city.maximum_distance_between_cities", 1_500.0D),
//    CITY_HOUSE_PROTECTION_DISTANCE("city.house_protection_radius", 13.0D), // on the minimum radius (60), you "can" fit 20 houses (one is the center)
//*    CITY_LEVELING_INITIAL_RANGE("city.leveling.initial_protection_range", 60.0D),
//*    CITY_LEVELING_RANGE_PER_LEVEL("city.leveling.range_per_level", 7.10D), // 124 in total

}
