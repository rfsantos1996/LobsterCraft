package com.jabyftw.lobstercraft.world;

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
public enum CityOccupation {

    CITIZEN((byte) 0),
    BUILDER((byte) 1),
    MANAGER((byte) 2);

    private final byte occupationId;

    CityOccupation(byte occupationId) {
        this.occupationId = occupationId;
    }

    public byte getOccupationId() {
        return occupationId;
    }

    /**
     * @param occupationId occupation id, can be null
     * @return city occupation, null if none found
     */
    public static CityOccupation fromId(@Nullable Byte occupationId) {
        if (occupationId == null) return null;
        for (CityOccupation cityOccupation : values())
            // Check if it is the same
            if (cityOccupation.getOccupationId() == occupationId)
                return cityOccupation;
        return null;
    }
}
