package com.jabyftw.lobstercraft.world.city;

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
public enum CityPosition {

    CITIZEN((byte) 1),
    BUILDER((byte) 2),
    MANAGER((byte) 3);

    private final byte positionId;

    CityPosition(byte positionId) {
        this.positionId = positionId;
    }

    public byte getPositionId() {
        return positionId;
    }

    public static CityPosition fromId(byte positionId) {
        for (CityPosition cityPosition : values())
            // Check if it is the same
            if (cityPosition.getPositionId() == positionId)
                return cityPosition;
        return null;
    }
}
