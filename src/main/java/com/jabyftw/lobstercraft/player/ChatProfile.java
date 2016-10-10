package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
public class ChatProfile extends Profile {

    private final int MAXIMUM_AMOUNT_OF_MUTES = LobsterCraft.configuration.getInt(ConfigurationValues.PLAYER_CHAT_MAXIMUM_MUTE_ENTRY_AMOUNT.toString());
    private final HashSet<Integer>
            pendingInsertion = new HashSet<>(),
            muteEntries = new HashSet<>(),
            pendingDeletion = new HashSet<>();

    private CommandSender lastWhisper = null;

    protected ChatProfile(@NotNull OnlinePlayer onlinePlayer) {
        super(ProfileType.CHAT_PROFILE, onlinePlayer);
    }

    public synchronized boolean isPlayerMuted(int playerId) {
        return muteEntries.contains(playerId) || pendingInsertion.contains(playerId);
    }

    public synchronized MuteEntryResponse mutePlayer(@NotNull OfflinePlayer offlinePlayer) {
        // Check if the player muted a lot of people
        if ((muteEntries.size() + pendingInsertion.size()) >= MAXIMUM_AMOUNT_OF_MUTES)
            return MuteEntryResponse.MAXIMUM_AMOUNT_OF_MUTE_ENTRIES;

        // Check if player is registered
        if (!offlinePlayer.isRegistered())
            return MuteEntryResponse.PLAYER_NOT_FOUND;

        org.bukkit.OfflinePlayer bukkitOfflinePlayer = offlinePlayer.getBukkitOfflinePlayer();
        // Check if player has permission to not be muted
        if (bukkitOfflinePlayer.isOp() || LobsterCraft.permission.playerHas(null, bukkitOfflinePlayer, Permissions.CHAT_MUTE_EXCEPTION.toString()))
            return MuteEntryResponse.CAN_NOT_MUTE_THIS_PLAYER;

        // Check if player is already muted
        if (isPlayerMuted(offlinePlayer.getPlayerId()))
            return MuteEntryResponse.PLAYER_ALREADY_MUTED;

        // Mute player
        pendingInsertion.add(offlinePlayer.getPlayerId());
        return MuteEntryResponse.SUCCESSFULLY_MUTED_PLAYER;
    }

    public synchronized UnmuteEntryResponse unmutePlayer(@NotNull OfflinePlayer offlinePlayer) {
        // Check if player is registered
        if (!offlinePlayer.isRegistered())
            return UnmuteEntryResponse.PLAYER_NOT_FOUND;

        // Check if player is not muted
        if (!isPlayerMuted(offlinePlayer.getPlayerId()))
            return UnmuteEntryResponse.PLAYER_ALREADY_UNMUTED;

        // Unmute player
        if (muteEntries.remove(offlinePlayer.getPlayerId()))
            // Add to pending deletion if mute is on database already
            this.pendingDeletion.add(offlinePlayer.getPlayerId());
        else
            pendingInsertion.remove(offlinePlayer.getPlayerId());
        return UnmuteEntryResponse.SUCCESSFULLY_UNMUTED_PLAYER;
    }

    /*
     * Getters
     */

    public CommandSender getLastWhisper() {
        return lastWhisper;
    }

    public void setLastWhisper(@Nullable CommandSender lastWhisper) {
        this.lastWhisper = lastWhisper;
    }

    public Set<Integer> getMuteEntries() {
        return Collections.unmodifiableSet(muteEntries);
    }

    /*
     * Database/profile loading
     */

    @Override
    public DatabaseState getDatabaseState() {
        return !pendingDeletion.isEmpty() || !pendingInsertion.isEmpty() ?
                DatabaseState.UPDATE_DATABASE : DatabaseState.ON_DATABASE;
    }

    @Override
    protected void onLoadingFromDatabase(@NotNull Connection connection, @NotNull OnlinePlayer onlinePlayer) throws SQLException {
        // Prepare statement and execute query
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT `user_mutedId` FROM minecraft.player_muted_players WHERE `user_playerId` = ?;");
        preparedStatement.setInt(1, playerId);
        ResultSet resultSet = preparedStatement.executeQuery();

        // Check if there is a database entry
        while (resultSet.next()) {
            // Delete rows if exceeded number of entries
            if (muteEntries.size() >= MAXIMUM_AMOUNT_OF_MUTES) {
                resultSet.deleteRow();
                continue;
            }

            // Add entry
            muteEntries.add(resultSet.getInt("user_mutedId"));
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
    protected synchronized boolean onSavingToDatabase(@NotNull Connection connection) throws SQLException {
        // Check if we need to INSERT
        if (!pendingInsertion.isEmpty()) {
            HashSet<Integer> processingIds = new HashSet<>();

            // Create prepared statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`player_muted_players` (`user_playerId`, `user_mutedId`) VALUES (?, ?);"
            );

            // Iterate through pending ones
            for (Integer mutedId : pendingInsertion) {
                preparedStatement.setInt(1, playerId);
                preparedStatement.setInt(2, mutedId);
                preparedStatement.addBatch();
                processingIds.add(mutedId);
            }

            // Execute and close stuff
            preparedStatement.executeBatch();
            preparedStatement.close();

            // Mark as inserted
            for (Integer mutedPlayer : processingIds) {
                pendingInsertion.remove(mutedPlayer);
                muteEntries.add(mutedPlayer);
            }
            processingIds.clear();
        }

        // Check if we need to DELETE
        if (!pendingDeletion.isEmpty()) {
            HashSet<Integer> processingIds = new HashSet<>();
            StringBuilder query = new StringBuilder(
                    "DELETE FROM `minecraft`.`player_muted_players` WHERE (`user_playerId`, `user_mutedId`) IN (" // (?, ?), (...), (?, ?));
            );

            // Prepare query
            boolean first = true;
            for (Integer mutedId : pendingDeletion) {
                if (!first) query.append(", ");
                first = false;

                query.append('(').append(playerId).append(", ").append(mutedId).append(')');
                processingIds.add(mutedId);
            }

            // Close query
            query.append(");");

            // Prepare, execute and close statement
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString());
            preparedStatement.execute();
            preparedStatement.close();

            // Mark as deleted
            pendingDeletion.removeAll(processingIds);
            processingIds.clear();
        }
        return true;
    }

    /*
     * Classes
     */

    public enum MuteEntryResponse {
        SUCCESSFULLY_MUTED_PLAYER,
        PLAYER_ALREADY_MUTED,
        CAN_NOT_MUTE_THIS_PLAYER,
        PLAYER_NOT_FOUND,
        MAXIMUM_AMOUNT_OF_MUTE_ENTRIES
    }

    public enum UnmuteEntryResponse {
        SUCCESSFULLY_UNMUTED_PLAYER,
        PLAYER_ALREADY_UNMUTED,
        PLAYER_NOT_FOUND
    }
}
