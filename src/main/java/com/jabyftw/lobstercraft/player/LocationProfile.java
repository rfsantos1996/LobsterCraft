package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.sun.istack.internal.NotNull;
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

    public LocationProfile(@NotNull OnlinePlayer onlinePlayer) {
        super(ProfileType.LOCATION_PROFILE, onlinePlayer);
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
        setAsModified();
    }

    /*
     * Overridden methods
     */

    @Override
    protected void onLoadingFromDatabase(@NotNull Connection connection, OnlinePlayer onlinePlayer) throws SQLException {
        // Prepare statement and execute query
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.last_location_profile WHERE `user_playerId`=?;");
        preparedStatement.setInt(1, playerId);
        ResultSet resultSet = preparedStatement.executeQuery();

        // Check if database record exists
        if (resultSet.next()) {
            byte worldId = resultSet.getByte("worldId");

            // Set last location
            lastLocation = resultSet.wasNull() ? null : new Location(
                    LobsterCraft.servicesManager.worldService.getWorld(worldId),
                    resultSet.getDouble("x"),
                    resultSet.getDouble("y"),
                    resultSet.getDouble("z"),
                    resultSet.getFloat("yaw"),
                    resultSet.getFloat("pitch")
            );

            // Set database state
            this.databaseState = DatabaseState.ON_DATABASE;
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();
    }

    @Override
    protected void onProfileApplication() {
    }

    @Override
    protected void onProfileDestruction() {
    }

    @Override
    protected boolean onSavingToDatabase(@NotNull Connection connection) throws SQLException {
        boolean inserting = databaseState == DatabaseState.INSERT_TO_DATABASE;

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement(
                inserting ? "INSERT INTO `minecraft`.`last_location_profile` (`user_playerId`, `worldId`, `x`, `y`, `z`, `yaw`, `pitch`) VALUES (?, ?, ?, ?, ?, ?, ?);"
                        : "UPDATE `minecraft`.`last_location_profile` SET `worldId` = ?, `x` = ?, `y` = ?,`z` = ?, `yaw` = ?, `pitch` = ? WHERE `user_playerId` = ?;"
        );

        int index = inserting ? 2 : 1;
        boolean nullLocation = lastLocation == null || LobsterCraft.servicesManager.worldService.getWorldId(lastLocation.getWorld()) == null;

        // Set variables
        preparedStatement.setObject(index++, nullLocation ? null : LobsterCraft.servicesManager.worldService.getWorldId(lastLocation.getWorld()), Types.TINYINT);
        preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getX(), Types.DOUBLE);
        preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getY(), Types.DOUBLE);
        preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getZ(), Types.DOUBLE);
        preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getYaw(), Types.FLOAT);
        preparedStatement.setObject(index++, nullLocation ? null : lastLocation.getPitch(), Types.FLOAT);
        preparedStatement.setLong(inserting ? 1 : index, onlinePlayer.getOfflinePlayer().getPlayerId());

        // Execute and close statement
        preparedStatement.execute();
        preparedStatement.close();
        return true;
    }
}
