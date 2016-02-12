package com.jabyftw.lobstercraft.player.chat;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.listeners.ChatListener;
import com.jabyftw.lobstercraft.util.Service;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

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
public class ChatService extends Service {

    private final ConcurrentHashMap<Long, ModeratorMuteEntry> muteEntries = new ConcurrentHashMap<>();

    @Override
    public boolean onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(new ChatListener(), LobsterCraft.lobsterCraft);

        try {
            cacheModeratorMutes();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public void onDisable() {
        saveModeratorMutes();
    }

    public boolean moderatorUnmutePlayer(long mutedId) {
        ModeratorMuteEntry moderatorMuteEntry = muteEntries.get(mutedId);

        // Exclude non-existing mutes
        if (moderatorMuteEntry == null) return false;

        moderatorMuteEntry.unmute();
        return true;
    }

    public long moderatorMutePlayer(long mutedId, Long moderatorId, @NotNull final String reason, long muteDuration) {
        ModeratorMuteEntry moderatorMuteEntry = muteEntries.get(mutedId);
        long unmuteDate = System.currentTimeMillis() + muteDuration;

        // If mute already exists
        if (moderatorMuteEntry != null) {
            moderatorMuteEntry.updateMute(unmuteDate, moderatorId, reason);
            return unmuteDate;
        }

        muteEntries.put(mutedId, new ModeratorMuteEntry(mutedId, moderatorId, unmuteDate, reason));
        return unmuteDate;
    }

    private void cacheModeratorMutes() throws SQLException {
        // Get connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.mod_muted_players;");

        // Execute statement and retrieve ResultSet
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through all entries
        while (resultSet.next()) {
            long playerId = resultSet.getLong("users_playerId");
            Long moderatorId = resultSet.getLong("user_moderatorId");
            boolean nullModeratorId = resultSet.wasNull();

            // Add entry to storage
            muteEntries.put(
                    playerId,
                    new ModeratorMuteEntry(
                            resultSet.getLong("mute_index"),
                            playerId,
                            nullModeratorId ? null : moderatorId,
                            resultSet.getLong("muteDate"),
                            resultSet.getLong("unmuteDate"),
                            resultSet.getString("reason")
                    )
            );
        }

        // Close ResultSet, PreparedStatement then Connection
        resultSet.close();
        preparedStatement.close();
        connection.close();
    }

    private void saveModeratorMutes() {
        HashSet<ModeratorMuteEntry>
                pendingInsertion = new HashSet<>(),
                pendingUpdate = new HashSet<>(),
                pendingDeletion = new HashSet<>();

        // Iterate through all mute entries
        for (ModeratorMuteEntry muteEntry : muteEntries.values()) {
            // Check to save every mute entry
            switch (muteEntry.getDatabaseState()) {
                case INSERT_TO_DATABASE:
                    pendingInsertion.add(muteEntry);
                    break;
                case UPDATE_DATABASE:
                    pendingUpdate.add(muteEntry);
                    break;
                case DELETE_FROM_DATABASE:
                    pendingDeletion.add(muteEntry);
                    break;
            }
        }

        if (!pendingDeletion.isEmpty() || !pendingInsertion.isEmpty() || !pendingDeletion.isEmpty())
            try {
                // Retrieve connection
                Connection connection = LobsterCraft.dataSource.getConnection();

                if (!pendingInsertion.isEmpty()) {
                    // Create the query
                    StringBuilder stringBuilder = new StringBuilder("INSERT INTO `minecraft`.`mod_muted_players` (`user_mutedId`, `user_moderatorId`,`muteDate`, `unmuteDate`, `reason`) VALUES ");

                    // Iterate through all entries
                    Iterator<ModeratorMuteEntry> iterator = pendingInsertion.iterator();

                    while (iterator.hasNext()) {
                        ModeratorMuteEntry next = iterator.next();

                        stringBuilder.append('(')
                                .append(next.getMutedPlayerId())
                                .append(next.getModeratorId())
                                .append(next.getMuteDate())
                                .append(next.getUnmuteDate())
                                .append("'").append(next.getReason()).append("'")
                                .append(')');

                        if (iterator.hasNext()) stringBuilder.append(", ");
                    }

                    // Close statement
                    stringBuilder.append(';');

                    // Prepare, execute and close statement
                    PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
                    preparedStatement.execute();
                    preparedStatement.close();
                }

                if (!pendingUpdate.isEmpty()) {
                    // Prepare statement
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "UPDATE `minecraft`.`mod_muted_players` SET `user_moderatorId` = ?, `muteDate` = ?, `unmuteDate` = ?, `reason` = ?" +
                                    " WHERE `mute_index` = ? AND `user_mutedId` = ?;"
                    );

                    // Iterate through all entries
                    for (ModeratorMuteEntry next : pendingUpdate) {
                        Long moderatorId = next.getModeratorId();

                        if (moderatorId == null)
                            preparedStatement.setNull(1, Types.BIGINT);
                        else
                            preparedStatement.setLong(1, moderatorId);
                        preparedStatement.setLong(2, next.getMuteDate());
                        preparedStatement.setLong(3, next.getUnmuteDate());
                        preparedStatement.setString(4, next.getReason());

                        preparedStatement.setLong(5, next.getMuteIndex());
                        preparedStatement.setLong(6, next.getMutedPlayerId());

                        // Add batch
                        preparedStatement.addBatch();
                    }

                    // Execute statement
                    preparedStatement.executeBatch();
                    preparedStatement.close();
                }

                if (!pendingDeletion.isEmpty()) {
                    // Create the query
                    StringBuilder stringBuilder = new StringBuilder("DELETE FROM `minecraft`.`mod_muted_players` WHERE `mute_index` IN (");

                    // Iterate through all entries
                    Iterator<ModeratorMuteEntry> iterator = pendingDeletion.iterator();

                    while (iterator.hasNext()) {
                        ModeratorMuteEntry next = iterator.next();

                        stringBuilder.append(next.getMuteIndex());
                        if (iterator.hasNext()) stringBuilder.append(", ");
                    }

                    // Close statement
                    stringBuilder.append(");");

                    // Prepare, execute and close statement
                    PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
                    preparedStatement.execute();
                    preparedStatement.close();
                }

                // Close connection
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                pendingInsertion.clear();
                pendingUpdate.clear();
                pendingDeletion.clear();
            }
    }
}
