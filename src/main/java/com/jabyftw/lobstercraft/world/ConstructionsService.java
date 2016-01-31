package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.Service;
import com.sun.istack.internal.NotNull;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;

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
public class ConstructionsService extends Service {

    private final HashMap<String, Long> constructionsStorage = new HashMap<>();

    @Override
    public boolean onEnable() {
        try {
            cacheConstructions();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onDisable() {
    }

    public Long getConstructionId(@NotNull final String constructionName) {
        return constructionsStorage.get(constructionName.toLowerCase());
    }

    public String getConstructionName(@NotNull final long constructionId) {
        // Iterate through all entries
        for (Map.Entry<String, Long> entry : constructionsStorage.entrySet())
            // Check for the same constructionId
            if (entry.getValue() == constructionId)
                // Return if found
                return entry.getKey();
        return null;
    }

    public Set<String> getConstructionSet() {
        return constructionsStorage.keySet();
    }

    public FutureTask<Long> registerConstruction(@NotNull final String constructionName) {
        return new FutureTask<>(() -> {
            long constructionId;

            // Retrieve connection
            Connection connection = LobsterCraft.dataSource.getConnection();

            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`world_constructions` (`constructionName`) VALUES (?);",
                    Statement.RETURN_GENERATED_KEYS
            );
            preparedStatement.setString(1, constructionName.toLowerCase());

            // Execute statement
            preparedStatement.execute();

            // Return generated key
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            if (generatedKeys.next())
                constructionId = generatedKeys.getLong("constructionId");
            else
                throw new IllegalStateException("Couldn't get generated key from construction");

            // Close everything
            generatedKeys.close();
            preparedStatement.close();
            connection.close();

            // Add it to list, as it was successful
            BukkitScheduler.runTask(() -> constructionsStorage.put(constructionName.toLowerCase(), constructionId));

            return constructionId;
        });
    }

    private void cacheConstructions() throws SQLException {
        // Retrieve connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.world_constructions;");

        // Execute statement
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through results
        while (resultSet.next()) {
            // Insert to construction storage
            constructionsStorage.put(resultSet.getString("constructionName").toLowerCase(), resultSet.getLong("constructionId"));
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();
        connection.close();
    }
}
