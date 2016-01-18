package com.jabyftw.pacocacraft.block.block_protection;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.sql.*;
import java.util.*;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
@SuppressWarnings("Convert2streamapi")
public class WorldController {

    public final UnmodifiableList<String> ignoredWorlds;
    private final HashMap<World, Long> worldIdMap = new HashMap<>();

    protected WorldController() {
        ArrayList<String> worlds = new ArrayList<>();

        // Add all ignored worlds name
        for(String worldName : PacocaCraft.config.getStringList(ConfigValue.BLOCK_IGNORED_WORLDS.getPath()))
            worlds.add(worldName.toLowerCase());

        ignoredWorlds = new UnmodifiableList<>(worlds);
    }

    public Set<World> getProtectedWorlds() {
        return worldIdMap.keySet();
    }

    public void updateWorldList() {
        ArrayList<World> worldsToHandle = new ArrayList<>(Bukkit.getWorlds());

        // Variables
        ArrayList<Long> pendingWorldDeletion = new ArrayList<>();
        LinkedList<World> pendingWorldInsertion = new LinkedList<>();

        try {
            // Get connection
            Connection connection = PacocaCraft.dataSource.getConnection();
            { // Get worldsToHandle on database
                // Prepare statement
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.worldsToHandle;");

                // Execute statement, return entries
                ResultSet resultSet = preparedStatement.executeQuery();

                // Handle results
                while(resultSet.next()) {
                    // Variables from database
                    String worldName = resultSet.getString("worldName").toLowerCase();
                    long worldId = resultSet.getLong("worldId");

                    // Variables for look-up
                    boolean worldFound = false;
                    World currentWorld = null;

                    // Check if the world exists
                    for(World world : worldsToHandle)
                        if(world.getName().equalsIgnoreCase(worldName)) {
                            worldFound = true;
                            currentWorld = world;
                        }

                    // If the world wasn't found or is supposed to be ignored, delete it from database
                    if(!worldFound || ignoredWorlds.contains(worldName))
                        pendingWorldDeletion.add(worldId);
                    else if(!worldIdMap.containsKey(currentWorld))
                        worldIdMap.put(currentWorld, worldId);

                    // Remove a handled world (will remain: to insertion or already existing worlds)
                    worldsToHandle.remove(currentWorld);
                }

                // Close everything
                resultSet.close();
                preparedStatement.close();
            }
            { // Remove already existing worlds
                Iterator<World> iterator = worldsToHandle.iterator();

                // Iterate through all worlds
                while(iterator.hasNext()) {
                    // If world exist in database, remove it
                    if(worldIdMap.containsKey(iterator.next()))
                        iterator.remove();
                }
                // Now remain only to insertion worlds
            }
            { // Insert pending worldsToHandle
                // Prepare query
                StringBuilder stringBuilder = new StringBuilder("INSERT INTO `minecraft`.`worldsToHandle` (`worldId`, `worldName`) VALUES ");

                for(int i = 0; i < pendingWorldInsertion.size(); i++) {
                    // Append world name
                    stringBuilder.append('(').append(pendingWorldInsertion.get(i).getName().toLowerCase()).append(')');
                    // Append a coma if isn't the last one
                    if(i != pendingWorldInsertion.size() - 1) stringBuilder.append(", ");
                }

                // Close query
                stringBuilder.append(';');

                // Prepare statement
                PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString(), Statement.RETURN_GENERATED_KEYS);

                // Execute query returning
                preparedStatement.execute();
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

                // Iterate through generated keys
                int index = 0;
                while(generatedKeys.next()) {
                    World world = pendingWorldInsertion.get(index);

                    // Add to world map
                    worldIdMap.put(world, generatedKeys.getLong("worldId"));

                    // Remove from worlds to handle
                    worldsToHandle.remove(world);

                    // Increment index
                    index++;
                }

                // Close everything
                generatedKeys.close();
                preparedStatement.close();

                // Clear lists
                pendingWorldInsertion.clear();
            }
            { // Delete pending worldsToHandle
                // Prepare query
                StringBuilder stringBuilder = new StringBuilder("DELETE FROM `minecraft`.`worldsToHandle` WHERE `worldId` IN (");

                for(int i = 0; i < pendingWorldDeletion.size(); i++) {
                    // Append world name
                    stringBuilder.append(pendingWorldDeletion.get(i));
                    // Append a coma if isn't the last one
                    if(i != pendingWorldDeletion.size() - 1) stringBuilder.append(", ");
                }

                // Close query
                stringBuilder.append(");");

                // Prepare, execute and close statement
                PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
                preparedStatement.execute();
                preparedStatement.close();

                // Clear list
                pendingWorldDeletion.clear();
            }
            // Close connection
            connection.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }

        // Clear lists
        if(!worldsToHandle.isEmpty())
            PacocaCraft.logger.warning(worldsToHandle.size() + " weren't handled on world cache update");
        worldsToHandle.clear();
    }
}
