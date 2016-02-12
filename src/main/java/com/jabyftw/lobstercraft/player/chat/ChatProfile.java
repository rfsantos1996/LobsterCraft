package com.jabyftw.lobstercraft.player.chat;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.Profile;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.sun.istack.internal.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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
public class ChatProfile extends Profile {

    private final int MAXIMUM_AMOUNT_OF_MUTES = LobsterCraft.config.getInt(ConfigValue.PLAYER_MAXIMUM_AMOUNT_OF_MUTES.toString());
    private final List<MuteEntry>
            pendingInsertion = Collections.synchronizedList(new ArrayList<>()),
            muteEntries = Collections.synchronizedList(new ArrayList<>()),
            pendingDeletion = Collections.synchronizedList(new ArrayList<>());

    public ChatProfile(long playerId) {
        super(playerId);
    }

    @Override
    protected void onProfileApplication(@NotNull PlayerHandler playerHandler) {
    }

    @Override
    protected void onProfileDestruction() {
    }

    public Set<MuteEntry> getMuteEntries() {
        HashSet<MuteEntry> mutedPlayers = new HashSet<>();
        synchronized (muteEntries) {
            mutedPlayers.addAll(muteEntries);
        }
        synchronized (pendingInsertion) {
            mutedPlayers.addAll(pendingInsertion);
        }
        return mutedPlayers;
    }

    public boolean isPlayerMuted(long playerId) {
        // Check pending mute players
        for (MuteEntry muteEntry : pendingInsertion)
            if (muteEntry.getMutedPlayerId() == playerId)
                return true;

        // Check already muted players
        for (MuteEntry muteEntry : muteEntries)
            if (muteEntry.getMutedPlayerId() == playerId)
                return true;

        return false;
    }

    public MuteResponse mutePlayer(long playerId) {
        // Check for mute list size
        if ((pendingInsertion.size() + muteEntries.size()) >= MAXIMUM_AMOUNT_OF_MUTES)
            return MuteResponse.FULL_MUTE_LIST;

        MuteEntry muteEntry = new MuteEntry(playerId, getPlayerId(), System.currentTimeMillis());

        // Check it is already muted
        if (pendingInsertion.contains(muteEntry) || muteEntries.contains(muteEntry))
            return MuteResponse.ALREADY_MUTED;

        int deletedIndex = pendingDeletion.indexOf(muteEntry);

        // If player was removed before
        if (deletedIndex >= 0) {
            // Remove the "pending removal" and insert it back to the list
            muteEntries.add(pendingDeletion.remove(deletedIndex));
        } else {
            // Add it to pending insertion otherwise
            pendingInsertion.add(muteEntry);
            setAsModified();
        }

        return MuteResponse.SUCCESSFULLY_MUTED;
    }

    public UnmuteResponse unmutePlayer(long playerId) {
        MuteEntry muteEntry = new MuteEntry(playerId, getPlayerId(), System.currentTimeMillis());

        int insertedIndex = pendingInsertion.indexOf(muteEntry);

        // If player was just added to mute list, remove it (and, therefore, don't saving on the database)
        if (insertedIndex >= 0) {
            pendingInsertion.remove(insertedIndex);
            return UnmuteResponse.SUCCESSFULLY_UNMUTED;
        }

        int muteIndex = muteEntries.indexOf(muteEntry);

        // If the player was muted on database
        if (muteIndex >= 0) {
            // Add it to the pending deletion, so it is removed from database
            pendingDeletion.add(muteEntries.remove(muteIndex));
            setAsModified();
            return UnmuteResponse.SUCCESSFULLY_UNMUTED;
        }

        return UnmuteResponse.ALREADY_UNMUTED;
    }

    @Override
    public DatabaseState getDatabaseState() {
        return (!pendingDeletion.isEmpty() || !pendingInsertion.isEmpty()) ? DatabaseState.UPDATE_DATABASE : DatabaseState.ON_DATABASE;
    }

    public static ChatProfile retrieveProfile(@NotNull final Connection connection, long playerId) throws SQLException {
        ChatProfile chatProfile = new ChatProfile(playerId);

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.muted_players WHERE user_ownerId=?;");

        // Setup variables
        preparedStatement.setLong(1, playerId);

        // Execute query
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through all results
        while (resultSet.next()) {
            chatProfile.muteEntries.add(new MuteEntry(
                    resultSet.getLong("mute_index"),
                    resultSet.getLong("user_mutedId"),
                    resultSet.getLong("user_ownerId"),
                    resultSet.getLong("muteDate")
            ));
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();

        // Return profile
        return chatProfile;
    }

    public static boolean saveProfile(@NotNull final Connection connection, @NotNull final ChatProfile chatProfile) {
        try {
            List<MuteEntry> pendingInsertion = chatProfile.pendingInsertion;
            List<MuteEntry> pendingDeletion = chatProfile.pendingDeletion;

            if (!pendingInsertion.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder("INSERT INTO `minecraft`.`muted_players` (`user_mutedId`, `user_ownerId`, `muteDate`) VALUES ");

                // Iterate through all profiles
                Iterator<MuteEntry> iterator = pendingInsertion.iterator();

                while (iterator.hasNext()) {
                    MuteEntry next = iterator.next();

                    // Append information
                    stringBuilder.append('(')
                            .append(next.getMutedPlayerId()).append(", ")
                            .append(next.getOwnerPlayerId()).append(", ")
                            .append(next.getMuteDate())
                            .append(')');

                    if (iterator.hasNext()) stringBuilder.append(", ");

                    // Remove from list (we will have to fetch everything again, might as well use retrieveProfile)
                    iterator.remove();
                }

                // Close query
                stringBuilder.append(';');

                // Prepare, execute and close statement
                PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
                preparedStatement.execute();
                preparedStatement.close();
            }

            if (!pendingDeletion.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder("DELETE FROM `minecraft`.`muted_players` WHERE `mute_index` IN (");

                // Iterate through all profiles
                Iterator<MuteEntry> iterator = pendingDeletion.iterator();

                while (iterator.hasNext()) {
                    MuteEntry next = iterator.next();

                    // Append information
                    stringBuilder.append(next.getMuteIndex());

                    if (iterator.hasNext()) stringBuilder.append(", ");

                    // Remove from list
                    iterator.remove();
                }

                // Close query
                stringBuilder.append(");");

                // Prepare, execute and close statement
                PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
                preparedStatement.execute();
                preparedStatement.close();
            }

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public enum MuteResponse {
        ALREADY_MUTED,
        SUCCESSFULLY_MUTED,
        FULL_MUTE_LIST
    }

    public enum UnmuteResponse {
        ALREADY_UNMUTED,
        SUCCESSFULLY_UNMUTED,
    }
}
