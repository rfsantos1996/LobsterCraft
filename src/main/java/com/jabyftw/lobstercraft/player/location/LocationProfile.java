package com.jabyftw.lobstercraft.player.location;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.Profile;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.Location;

import java.sql.*;

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
public class LocationProfile extends Profile {

    private Location lastLocation = null;

    protected LocationProfile(long playerId) {
        super(playerId);
    }

    protected LocationProfile(long playerId, @Nullable final Location lastLocation) {
        super(playerId);
        this.lastLocation = lastLocation;
    }

    public static LocationProfile retrieveProfile(@NotNull final Connection connection, long playerId) throws Exception {
        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.last_location_profile WHERE `user_playerId`=?;");
        preparedStatement.setLong(1, playerId);

        // Execute query
        ResultSet resultSet = preparedStatement.executeQuery();

        LocationProfile locationProfile;

        // Listen to results or create default
        if (resultSet.next()) {
            long worldId = resultSet.getLong("worldId");
            boolean locationIsNull = resultSet.wasNull();

            Location lastLocation = locationIsNull ? null : new Location(
                    LobsterCraft.worldService.getWorldFromId(worldId),
                    resultSet.getDouble("x"),
                    resultSet.getDouble("y"),
                    resultSet.getDouble("z"),
                    resultSet.getFloat("yaw"),
                    resultSet.getFloat("pitch")
            );
            locationProfile = new LocationProfile(playerId, lastLocation);
        } else {
            locationProfile = new LocationProfile(playerId);
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();

        return locationProfile;
    }

    public static boolean saveProfile(@NotNull final Connection connection, @NotNull final LocationProfile profile) {
        try {
            boolean inserting = profile.databaseState == DatabaseState.INSERT_TO_DATABASE;

            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    inserting ? "INSERT INTO `minecraft`.`last_location_profile` (`user_playerId`, `worldId`, `x`, `y`, `z`, `yaw`, `pitch`) VALUES (?, ?, ?, ?, ?, ?, ?);"
                            : "UPDATE `minecraft`.`last_location_profile` SET `worldId` = ?, `x` = ?, `y` = ?,`z` = ?, `yaw` = ?, `pitch` = ? WHERE `user_playerId` = ?;"
            );

            int index = inserting ? 2 : 1;
            Location lastLocation = profile.lastLocation;
            boolean nullLocation = lastLocation == null || LobsterCraft.worldService.getIdFromWorld(lastLocation.getWorld()) == null;

            preparedStatement.setObject(index++, nullLocation ? null : LobsterCraft.worldService.getIdFromWorld(lastLocation.getWorld()), Types.BIGINT);
            preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getX(), Types.DOUBLE);
            preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getY(), Types.DOUBLE);
            preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getZ(), Types.DOUBLE);
            preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getYaw(), Types.FLOAT);
            preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getPitch(), Types.FLOAT);
            preparedStatement.setLong(inserting ? 1 : index, profile.getPlayerId());

            // Execute statement
            preparedStatement.execute();

            // Close statement
            preparedStatement.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onProfileApplication(@NotNull PlayerHandler playerHandler) {
    }

    @Override
    protected void onProfileDestruction() {
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(@Nullable final Location lastLocation) {
        this.lastLocation = lastLocation;
        setAsModified();
    }
}
