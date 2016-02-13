package com.jabyftw.lobstercraft.world.city;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.world.util.location_util.BlockLocation;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

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
public class CityHomeLocation extends BlockLocation {

    private long homeId = PlayerHandler.UNDEFINED_PLAYER;
    private long cityId;
    private Long playerId = null;

    private DatabaseState databaseState;

    public CityHomeLocation(long cityId, @NotNull final BlockLocation blockLocation) {
        super(blockLocation);
        this.cityId = cityId;
        this.databaseState = DatabaseState.NOT_ON_DATABASE;
    }

    public CityHomeLocation(long homeId, long cityId, @NotNull final BlockLocation blockLocation, @Nullable Long playerId) {
        super(blockLocation);
        this.homeId = homeId;
        this.cityId = cityId;
        this.playerId = playerId;
        this.databaseState = DatabaseState.ON_DATABASE;
    }

    public boolean isRegistered() {
        return homeId > 0;
    }

    public long getHomeId() {
        return homeId;
    }

    public long getCityId() {
        return cityId;
    }

    public CityStructure getCity() {
        return null; // TODO cityService
    }

    public boolean isOccupied() {
        return playerId != null;
    }

    public Long getPlayerId() {
        return playerId;
    }

    // TODO check for city level up, player must pay an entrance fee, must set playerId and position on the city
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    public void setAsModified() {
        if (databaseState == DatabaseState.NOT_ON_DATABASE)
            databaseState = DatabaseState.INSERT_TO_DATABASE;
        if (databaseState == DatabaseState.ON_DATABASE)
            databaseState = DatabaseState.UPDATE_DATABASE;
    }
}
