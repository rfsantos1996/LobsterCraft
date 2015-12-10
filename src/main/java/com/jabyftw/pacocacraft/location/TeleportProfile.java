package com.jabyftw.pacocacraft.location;

import com.jabyftw.Util;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.player.PlayerProfile;
import com.jabyftw.pacocacraft.player.ProfileType;
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

    public TeleportProfile(long playerId, @Nullable Location lastLocation) {
        super(ProfileType.TELEPORT_PROFILE, playerId);
        this.lastLocation = lastLocation;
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
        modified = true;
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

            Location lastLocation;
            String worldName = resultSet.getString("worldName");

            // Check if lastLocation is null on database
            if(resultSet.wasNull())
                lastLocation = null; // Don't search for more values
            else
                lastLocation = new Location(
                        Util.parseToWorld(worldName),
                        resultSet.getDouble("x"),
                        resultSet.getDouble("y"),
                        resultSet.getDouble("z"),
                        resultSet.getFloat("yaw"),
                        resultSet.getFloat("pitch")
                );

            // Apply information to profile on a "loaded profile" constructor
            teleportProfile = new TeleportProfile(playerId, lastLocation);

            // Close ResultSet and PreparedStatement
            resultSet.close();
            preparedStatement.close();
        }
        // Close connection and return profile
        connection.close();

        return teleportProfile;
    }

    public static void saveTeleportProfile(@NotNull TeleportProfile teleportProfile) throws SQLException {
        // Get connection from pool and execute query
        Connection connection = PacocaCraft.dataSource.getConnection();
        {
            // Prepare statement arguments
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`last_location_profile`(`user_playerId`,`worldName`,`x`,`y`,`z`,`yaw`,`pitch`) VALUES (?,?,?,?,?,?,?)\n" + // 1->7
                            "ON DUPLICATE KEY UPDATE `worldName` = ?, `x` = ?, `y` = ?, `z` = ?, `yaw` = ?, `pitch` = ?;" // 7+1 -> 7+1+5
            );

            // Update statement where needed
            preparedStatement.setLong(1, teleportProfile.getPlayerId());

            // This table supports null values, so... (this looks ugly but I would need to store something to get if it is on database ); )
            Location lastLocation = teleportProfile.getLastLocation();
            preparedStatement.setObject(2, lastLocation == null ? null : lastLocation.getWorld().getName().toLowerCase(), Types.VARCHAR);
            preparedStatement.setObject(8, lastLocation == null ? null : lastLocation.getWorld().getName().toLowerCase(), Types.VARCHAR);

            preparedStatement.setObject(3, lastLocation == null ? null : lastLocation.getX(), Types.DOUBLE);
            preparedStatement.setObject(9, lastLocation == null ? null : lastLocation.getX(), Types.DOUBLE);

            preparedStatement.setObject(4, lastLocation == null ? null : lastLocation.getY(), Types.DOUBLE);
            preparedStatement.setObject(10, lastLocation == null ? null : lastLocation.getY(), Types.DOUBLE);

            preparedStatement.setObject(5, lastLocation == null ? null : lastLocation.getZ(), Types.DOUBLE);
            preparedStatement.setObject(11, lastLocation == null ? null : lastLocation.getZ(), Types.DOUBLE);

            preparedStatement.setObject(6, lastLocation == null ? null : lastLocation.getYaw(), Types.FLOAT);
            preparedStatement.setObject(12, lastLocation == null ? null : lastLocation.getYaw(), Types.FLOAT);

            preparedStatement.setObject(7, lastLocation == null ? null : lastLocation.getPitch(), Types.FLOAT);
            preparedStatement.setObject(13, lastLocation == null ? null : lastLocation.getPitch(), Types.FLOAT);

            // Execute statement
            preparedStatement.execute();

            // Close statement
            preparedStatement.close();
        }
        // Close connection and return true (it worked or it'll throw exceptions)
        connection.close();
    }
}
