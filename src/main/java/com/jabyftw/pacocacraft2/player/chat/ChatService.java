package com.jabyftw.pacocacraft2.player.chat;

import com.jabyftw.pacocacraft2.PacocaCraft;
import com.jabyftw.pacocacraft2.login.UserProfile;
import com.jabyftw.pacocacraft2.player.chat.commands.*;
import com.jabyftw.pacocacraft2.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft2.util.ServerService;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
public class ChatService implements ServerService {

    /*
     * We need something to write all entries (preferably using playerId to check the reason later)
     * And a list to separate which id is pending or fixed
     */

    public static final ConcurrentHashMap<Long, ModeratorMuteEntry> mutedPlayersStorage = new ConcurrentHashMap<>();
    public static final List<Long>
            pendingMutePlayers = Collections.synchronizedList(new LinkedList<>()),
            mutedPlayers = Collections.synchronizedList(new ArrayList<>()),
            pendingUnmutePlayers = Collections.synchronizedList(new ArrayList<>());

    public static ModeratorMuteEntry getActiveModeratorMuteEntry(final PlayerHandler playerHandler) {
        long playerId = playerHandler.getProfile(UserProfile.class).getPlayerId();
        ModeratorMuteEntry muteEntry = mutedPlayersStorage.get(playerId);

        // return the mute entry if the player is actually muted (even tho it exists)
        return ((pendingMutePlayers.contains(playerId) || mutedPlayers.contains(playerId)) && muteEntry.getUnmuteDate() > System.currentTimeMillis()) ?
                muteEntry : null;
    }

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginCommand("whisper").setExecutor(new WhisperCommand());
        Bukkit.getServer().getPluginCommand("r").setExecutor(new ReplyCommand());
        Bukkit.getServer().getPluginCommand("mute").setExecutor(new MuteCommand());
        Bukkit.getServer().getPluginCommand("adminmute").setExecutor(new AdminMuteCommand());
        Bukkit.getServer().getPluginCommand("unmute").setExecutor(new UnmuteCommand());
        Bukkit.getServer().getPluginCommand("adminunmute").setExecutor(new AdminUnmuteCommand());

        Bukkit.getPluginManager().registerEvents(new ChatListener(), PacocaCraft.pacocaCraft);

        cacheModeratorMutes();

        PacocaCraft.logger.info("Enabled " + getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
        saveModeratorMutes();
    }

    private void cacheModeratorMutes() {
        try {
            // Get connection
            Connection connection = PacocaCraft.dataSource.getConnection();

            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.mod_muted_players;");

            // Execute statement and retrieve ResultSet
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through all entries
            while (resultSet.next()) {
                long playerId = resultSet.getLong("users_playerId");

                // Add entry to storage
                mutedPlayersStorage.put(
                        playerId,
                        new ModeratorMuteEntry(
                                resultSet.getLong("mute_index"),
                                playerId,
                                resultSet.getLong("user_moderatorId"),
                                resultSet.getLong("muteDate"),
                                resultSet.getString("reason"),
                                resultSet.getLong("unmuteDate")
                        )
                );

                // Add playerId to muted
                mutedPlayers.add(playerId);
            }

            // Close ResultSet, PreparedStatement then Connection
            resultSet.close();
            preparedStatement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveModeratorMutes() {
        // TODO should be checked while on command
        // Lets double check this since we're on server restart/shutdown
        Iterator<Long> iterator = pendingMutePlayers.iterator();
        // Iterate through all pending insertion entries
        while (iterator.hasNext()) {
            long playerId = iterator.next();

            // If there is a pending remove for the same pending insertion, remove it (even from storage because it hasn't been to database yet)
            if (pendingUnmutePlayers.contains(playerId)) {
                mutedPlayersStorage.remove(playerId);
                iterator.remove();
            }
        }

        // Now save everything
        if (!pendingMutePlayers.isEmpty() && !pendingUnmutePlayers.isEmpty())
            try {
                // Get connection
                Connection connection = PacocaCraft.dataSource.getConnection();

                if (!pendingMutePlayers.isEmpty()) { // Save every pending mute
                    // Start up query
                    StringBuilder queryBuilder = new StringBuilder(
                            "INSERT INTO `minecraft`.`mod_muted_players` (`user_playerId`, `user_moderatorId`, `muteDate`, `unmuteDate`, `reason`) VALUES "
                    );

                    // Iterate through all records
                    {
                        int index = 0;
                        for (long playerId : pendingMutePlayers) {
                            ModeratorMuteEntry muteEntry = mutedPlayersStorage.get(playerId);

                            // Append all information and a comma if it isn't the last one
                            queryBuilder
                                    .append("(")
                                    .append(muteEntry.getPlayerId()).append(", ")
                                    .append(muteEntry.getModeratorPlayerId()).append(", ")
                                    .append(muteEntry.getMuteDate()).append(", ")
                                    .append(muteEntry.getUnmuteDate()).append(", ")
                                    .append(muteEntry.getMuteReason())
                                    .append(")");
                            if (index++ != pendingMutePlayers.size() - 1) queryBuilder.append(", ");
                        }
                    }

                    // Close query
                    queryBuilder.append(";");

                    // Prepare statement
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            queryBuilder.toString(),
                            Statement.RETURN_GENERATED_KEYS
                    );

                    // Execute and get generated keys
                    preparedStatement.execute();
                    ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                    int index = 0;

                    // Iterate through all generated keys and set it (in order through the linked list of ids)
                    while (generatedKeys.next())
                        mutedPlayersStorage.get(pendingMutePlayers.get(index++)).setMuteIndex(generatedKeys.getLong(""));

                    // Close everything
                    generatedKeys.close();
                    preparedStatement.close();
                }

                if (!pendingUnmutePlayers.isEmpty()) { // Delete every pending unmute
                    // Start up query
                    StringBuilder queryBuilder = new StringBuilder("DELETE FROM `minecraft`.`mod_muted_players` WHERE `user_playerId` IN (");

                    // Iterate through all pending unmute entries
                    int index = 0;
                    for (long playerId : pendingUnmutePlayers) {
                        // Append its mute index got from storage
                        queryBuilder.append(mutedPlayersStorage.get(playerId).getMuteIndex());
                        // Add a comma if it isn't the last one
                        if (index++ != pendingUnmutePlayers.size() - 1) queryBuilder.append(", ");
                    }

                    // Close query
                    queryBuilder.append(");");

                    // Prepare, execute and close statement
                    PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString());
                    preparedStatement.execute();
                    preparedStatement.close();
                }
                // Close connection on exit
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        // Pass pendingMutePlayers to moderatorMutedPlayers
        mutedPlayers.addAll(pendingMutePlayers);

        // Clear pending
        pendingMutePlayers.clear();
        pendingUnmutePlayers.clear();
    }
}
