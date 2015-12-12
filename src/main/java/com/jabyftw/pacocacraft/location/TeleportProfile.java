package com.jabyftw.pacocacraft.location;

import com.jabyftw.Util;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.profile_util.DatabaseState;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.profile_util.PlayerProfile;
import com.jabyftw.profile_util.ProfileType;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.Location;

import java.sql.*;

/**
 * Copyright (C) 2015  Rafael Sartori for PacocaCraft Plugin
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
public class TeleportProfile extends PlayerProfile {

    // Database related
    private Location lastLocation = null;

    public TeleportProfile(long playerId) {
        super(ProfileType.TELEPORT_PROFILE, playerId);
    }

    public TeleportProfile(long playerId, @NotNull Location lastLocation) {
        super(ProfileType.TELEPORT_PROFILE, playerId);
        this.lastLocation = lastLocation;
        this.databaseState = DatabaseState.ON_DATABASE;
    }

    @Override
    public void onPlayerHandleApply(@NotNull PlayerHandler playerHandler) {
    }

    @Override
    public void onPlayerHandleDestruction() {
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    protected void setLastLocation(@Nullable Location lastLocation) {
        this.lastLocation = lastLocation;
        setModified();
    }

    public static TeleportProfile fetchTeleportProfile(long playerId) throws SQLException {
        TeleportProfile teleportProfile;

        // Get connection from pool and execute query
        Connection connection = PacocaCraft.dataSource.getConnection();
        {
            // Prepare statement arguments
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM `minecraft`.`last_location_profile` WHERE `last_location_profile`.`user_playerId` = ?;"
            );
            preparedStatement.setLong(1, playerId);

            // Execute statement
            ResultSet resultSet = preparedStatement.executeQuery();
            if(!resultSet.next()) {
                // Returning before, close everything
                resultSet.close();
                preparedStatement.close();
                connection.close();

                // If player doesn't exists, return null
                return null;
            }

            // Check if lastLocation is null on database; if so, set teleport profile
            if(resultSet.wasNull()) {
                teleportProfile = new TeleportProfile(playerId, null);
            } else {
                String worldName = resultSet.getString("worldName");

                // Create last location
                Location lastLocation = new Location(
                        Util.parseToWorld(worldName),
                        resultSet.getDouble("x"),
                        resultSet.getDouble("y"),
                        resultSet.getDouble("z"),
                        resultSet.getFloat("yaw"),
                        resultSet.getFloat("pitch")
                );

                // Apply information to profile on a "loaded profile" constructor
                teleportProfile = new TeleportProfile(playerId, lastLocation);
            }

            // Close ResultSet and PreparedStatement
            resultSet.close();
            preparedStatement.close();
        }
        // Close connection and return profile
        connection.close();

        return teleportProfile;
    }

    public static void saveTeleportProfile(@NotNull TeleportProfile teleportProfile) throws SQLException {
        if(!teleportProfile.shouldBeSaved()) return;

        // Get connection from pool and execute query
        Connection connection = PacocaCraft.dataSource.getConnection();
        {
            boolean isInserting = teleportProfile.databaseState == DatabaseState.INSERT_DATABASE;

            // Prepare statement arguments
            PreparedStatement preparedStatement = connection.prepareStatement(
                    isInserting ?
                            "INSERT INTO `minecraft`.`last_location_profile` (`user_playerId`, `worldName`, `x`, `y`, `z`, `yaw`, `pitch`) VALUES (?, ?, ?, ?, ?, ?, ?);" :
                            "UPDATE `minecraft`.`last_location_profile` SET `worldName` = ?, `x` = ?, `y` = ?, `z` = ?, `yaw` = ?, `pitch` = ? WHERE `user_playerId` = ?;"
            );

            // Set variables
            // This table supports null values
            Location lastLocation = teleportProfile.getLastLocation();
            boolean isNUll = lastLocation == null;

            // If it is inserting, starts at 1; else, starts at 2
            int index = isInserting ? 1 : 2;
            // Set world
            preparedStatement.setObject(index++, isNUll ? null : lastLocation.getWorld().getName().toLowerCase(), Types.VARCHAR);
            // Set x, y and z axis
            preparedStatement.setObject(index++, isNUll ? null : lastLocation.getX(), Types.DOUBLE);
            preparedStatement.setObject(index++, isNUll ? null : lastLocation.getY(), Types.DOUBLE);
            preparedStatement.setObject(index++, isNUll ? null : lastLocation.getZ(), Types.DOUBLE);
            // Set direction
            preparedStatement.setObject(index++, isNUll ? null : lastLocation.getYaw(), Types.FLOAT);
            preparedStatement.setObject(index++, isNUll ? null : lastLocation.getPitch(), Types.FLOAT);
            preparedStatement.setLong(isInserting ? 1 : index, teleportProfile.getPlayerId());

            // Execute statement
            preparedStatement.execute();

            // Close statement
            preparedStatement.close();
        }
        // Close connection and return true (it worked or it'll throw exceptions)
        connection.close();

        // Update its state
        teleportProfile.databaseState = DatabaseState.ON_DATABASE;
    }
}
