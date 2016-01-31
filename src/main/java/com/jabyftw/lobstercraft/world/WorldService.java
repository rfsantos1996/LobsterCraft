package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.Service;
import com.jabyftw.lobstercraft.world.listeners.ProtectionListener;
import com.jabyftw.lobstercraft.world.listeners.WorldListener;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

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
public class WorldService extends Service {

    private final HashSet<String> ignoredWorlds = new HashSet<>();
    private final HashMap<Long, World> worldIds = new HashMap<>(Bukkit.getWorlds().size());

    @Override
    public boolean onEnable() {
        // Add all ignored worlds with lower-cased names
        for (String worldName : LobsterCraft.config.getStringList(ConfigValue.WORLD_IGNORED_WORLDS.toString()))
            ignoredWorlds.add(worldName.toLowerCase());

        // Update world cache
        try {
            updateWorldCache();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        // Register listeners
        Bukkit.getServer().getPluginManager().registerEvents(new ProtectionListener(), LobsterCraft.lobsterCraft);

        // Register later (in case WorldLoadEvent is thrown before plugin initialization)
        BukkitScheduler.runTaskLater(() -> Bukkit.getServer().getPluginManager().registerEvents(new WorldListener(), LobsterCraft.lobsterCraft), 3L);
        return true;
    }

    @Override
    public void onDisable() {
    }

    public boolean isWorldIgnored(@NotNull final World world) {
        return ignoredWorlds.contains(world.getName().toLowerCase());
    }

    public synchronized World getWorldFromId(long worldId) {
        if (worldId < 0) throw new IllegalArgumentException("World Id can't be less than one!");
        return worldIds.get(worldId);
    }

    /**
     * Get world's Id, null if the world is supposed to be ignored
     *
     * @param world given Bukkit's world
     * @return world's Id on database, null if world is supposed to be ignored
     */
    public synchronized Long getIdFromWorld(@NotNull final World world) {
        // Iterate through all items
        for (Map.Entry<Long, World> entry : worldIds.entrySet()) {
            // Check if it is the same world
            if (entry.getValue().getName().equalsIgnoreCase(world.getName()))
                return entry.getKey();
        }

        LobsterCraft.logger.warning("WorldId returning null for " + world.getName() + "(worldId's size is " + worldIds.size() + ")");
        return null;
    }

    public synchronized void updateWorldCache() throws SQLException {
        // Get connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        HashSet<World> pendingInsertion = new HashSet<>();
        HashSet<Long> pendingDeletion = new HashSet<>();

        // Fill pendingInsertion with worlds that aren't ignored
        for (World world : Bukkit.getWorlds()) {
            if (!ignoredWorlds.contains(world.getName().toLowerCase()))
                pendingInsertion.add(world);
        }

        { // Check current state
            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.worlds;");

            // Execute statement
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through all results
            while (resultSet.next()) {
                String worldName = resultSet.getString("worldName").toLowerCase();
                long worldId = resultSet.getLong("worldId");
                World foundWorld = getWorldIgnoringCase(worldName);

                // Exists a world on database that doesn't exists on Bukkit or was supposed to be ignored => delete
                if (foundWorld == null || ignoredWorlds.contains(worldName)) {
                    pendingDeletion.add(worldId);
                } else {
                    if (!worldIds.containsKey(worldId)) // World exists on database and on bukkit, insert it on worldId
                        worldIds.put(worldId, foundWorld);

                    // Remove from remaining worlds
                    pendingInsertion.remove(foundWorld);
                }
            }

            // Close statement
            resultSet.close();
            preparedStatement.close();
        }

        if (!pendingInsertion.isEmpty()) { // Insert pending insertions
            // Create query
            StringBuilder stringBuilder = new StringBuilder("INSERT INTO `minecraft`.`worlds` (`worldName`) VALUES \n" +
                    "SELECT * FROM `minecraft`.`worlds` WHERE `worldName` IN ('world', 'world_nether');");

            { // Iterate through all items
                Iterator<World> iterator = pendingInsertion.iterator();

                while (iterator.hasNext()) {
                    World world = iterator.next();

                    stringBuilder.append('(').append(world.getName().toLowerCase()).append(')');
                    if (iterator.hasNext()) stringBuilder.append(", ");
                }
            }

            // Start select query
            stringBuilder.append(";\nSELECT * FROM `minecraft`.`worlds` WHERE `worldName` IN (");

            { // Iterate through all items (again)
                Iterator<World> iterator = pendingInsertion.iterator();

                while (iterator.hasNext()) {
                    World world = iterator.next();

                    // Append their name
                    stringBuilder.append(world.getName().toLowerCase());
                    if (iterator.hasNext()) stringBuilder.append(", ");

                    // Doesn't need them anymore, remove it
                    iterator.remove();
                }
            }

            // Close query
            stringBuilder.append(");");

            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());

            // Return everything as we need to get the world's name (do not trusting linked lists)
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through inserted worlds
            while (resultSet.next()) {
                // Insert them to world map
                worldIds.put(resultSet.getLong("worldId"), getWorldIgnoringCase(resultSet.getString("worldName")));
            }

            // Close statement
            resultSet.close();
            preparedStatement.close();
        }

        if (!pendingDeletion.isEmpty()) { // Delete pending deletions
            // Create query
            StringBuilder stringBuilder = new StringBuilder("DELETE FROM `minecraft`.`worlds` WHERE worldId IN (");

            // Iterate through all items
            Iterator<Long> iterator = pendingDeletion.iterator();

            while (iterator.hasNext()) {
                Long worldId = iterator.next();

                // Append information (its ids)
                stringBuilder.append(worldId);
                if (iterator.hasNext()) stringBuilder.append(", ");

                // Doesn't need it anymore, remove
                iterator.remove();
            }

            // Close query
            stringBuilder.append(");");

            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());

            // Execute statement
            preparedStatement.execute();

            // Close statement
            preparedStatement.close();
        }

        // Close connection
        connection.close();
    }

    public World getWorldIgnoringCase(@NotNull final String worldName) {
        // Iterate through all worlds
        for (World world : Bukkit.getWorlds())
            // If the world has the same name, return it
            if (world.getName().equalsIgnoreCase(worldName)) return world;
        return null;
    }
}
