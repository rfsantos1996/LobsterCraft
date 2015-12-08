package com.jabyftw.pacocacraft.location;

import com.jabyftw.Util;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.BasePlayerProfile;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.player.PlayerProfile;
import com.jabyftw.pacocacraft.player.ProfileType;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

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

    protected static final HashMap<Player, TeleportProfile> waitingTeleportPlayers = new HashMap<>();

    // Variables
    private BukkitTask currentTeleport = null;

    // Database related
    private Location lastLocation = null;

    public TeleportProfile(long playerId) {
        super(ProfileType.TELEPORT_PROFILE, playerId);
    }

    public TeleportProfile(long playerId, @NotNull Location lastLocation) {
        super(ProfileType.TELEPORT_PROFILE, playerId);
        this.lastLocation = lastLocation;
    }

    @Override
    public void onPlayerHandleApply(@NotNull PlayerHandler playerHandler) {
    }

    @Override
    public void onPlayerHandleDestruction() {
        setCurrentTeleport(null, false);
    }

    public BukkitTask getCurrentTeleport() {
        return currentTeleport;
    }

    /**
     * Set delayed teleport task (it'll check for player move, damage etc)
     *
     * @param currentTeleport current task (null means finished teleporting and player will be removed from listener)
     * @param warnPlayer      if player should be warned about being cancelled or started teleporting
     */
    public void setCurrentTeleport(@Nullable BukkitTask currentTeleport, boolean warnPlayer) {
        // Cancel current teleport task if it exists
        if(this.currentTeleport != null)
            this.currentTeleport.cancel(); // Cancel current (it won't set null)

        Player player = getPlayerHandler().getPlayer();

        // If new teleport isn't null, don't let player move and stuff (used on listener)
        if(currentTeleport != null) {
            waitingTeleportPlayers.put(player, this);

            // Warn player
            if(warnPlayer) player.sendMessage("§4NÃO SE MOVA! §cTeleporte iniciado...");
        } else {
            // Else if teleport is finished (set as null), remove from listener
            waitingTeleportPlayers.remove(player);

            // Warn player
            if(warnPlayer) player.sendMessage("§cSeu teleporte foi cancelado!");
        }

        this.currentTeleport = currentTeleport;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
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
            if(!resultSet.next())
                // If player doesn't exists, return null
                return null;

            // Retrieve information
            Location lastLocation = new Location(
                    Util.parseToWorld(resultSet.getString("worldName")),
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

            Location lastLocation = teleportProfile.getLastLocation();
            preparedStatement.setString(2, lastLocation.getWorld().getName().toLowerCase());
            preparedStatement.setString(8, lastLocation.getWorld().getName().toLowerCase());

            preparedStatement.setDouble(3, lastLocation.getX());
            preparedStatement.setDouble(9, lastLocation.getX());

            preparedStatement.setDouble(4, lastLocation.getY());
            preparedStatement.setDouble(10, lastLocation.getY());

            preparedStatement.setDouble(5, lastLocation.getZ());
            preparedStatement.setDouble(11, lastLocation.getZ());

            preparedStatement.setFloat(6, lastLocation.getYaw());
            preparedStatement.setFloat(12, lastLocation.getYaw());

            preparedStatement.setFloat(7, lastLocation.getPitch());
            preparedStatement.setFloat(13, lastLocation.getPitch());

            // Execute statement
            preparedStatement.execute();

            // Close statement
            preparedStatement.close();
        }
        // Close connection and return true (it worked or it'll throw exceptions)
        connection.close();
    }
}
