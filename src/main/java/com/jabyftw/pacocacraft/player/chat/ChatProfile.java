package com.jabyftw.pacocacraft.player.chat;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.Permissions;
import com.jabyftw.profile_util.DatabaseState;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.profile_util.PlayerProfile;
import com.jabyftw.profile_util.ProfileType;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.collections4.list.FixedSizeList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.*;

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
public class ChatProfile extends PlayerProfile {

    private final static long FIXED_CHAT_REFRESH_DELAY = PacocaCraft.config.getLong(ConfigValue.PLAYER_FIXED_CHAT_REFRESH_DELAY.getPath()) * 20L; // Seconds to ticks
    public final static int MAXIMUM_NUMBER_MUTED_PLAYERS = PacocaCraft.config.getInt(ConfigValue.PLAYER_MAXIMUM_MUTED_PLAYERS.getPath());

    private final List<MuteEntry>
            pendingMute = Collections.synchronizedList(new LinkedList<>()),
            pendingUnmute = Collections.synchronizedList(new LinkedList<>()),
            mutedPlayers = Collections.synchronizedList(new LinkedList<>());

    // Variables
    private CommandSender lastPrivateSender = null;
    private final Object privateSenderLock = new Object();
    private volatile ChatState chatState = ChatState.EVERY_MESSAGE;
    private volatile boolean usingFixedChat = false;

    private final FixedSizeList<String> chatMessages = FixedSizeList.fixedSizeList(Arrays.asList(new String[9]));
    private final Object chatMessagesLock = new Object(); // Don't know if Collections.synchronized works here (FixedSizeList is from apache commons)
    private BukkitTask fixedChatTask = null;

    public ChatProfile(long playerId) {
        super(ProfileType.CHAT_PROFILE, playerId);
    }

    public ChatProfile(long playerId, Collection<MuteEntry> mutedPlayers) {
        super(ProfileType.CHAT_PROFILE, playerId);
        this.mutedPlayers.addAll(mutedPlayers);
        databaseState = DatabaseState.ON_DATABASE; // Not really needed
    }

    @Override
    public void onPlayerHandleApply(@NotNull PlayerHandler playerHandler) {
    }

    @Override
    public void onPlayerHandleDestruction() {
        setUsingFixedChat(false); // Destroy chat task
    }

    @Override
    public boolean shouldBeSaved() {
        return !pendingMute.isEmpty() || !pendingUnmute.isEmpty();
    }

    /*
     * Runtime methods
     */

    public MuteResponse mutePlayer(long playerId) {
        // Check muted players amount
        if(pendingMute.size() + mutedPlayers.size() > MAXIMUM_NUMBER_MUTED_PLAYERS &&
                !PacocaCraft.permission.has(getPlayerHandler().getPlayer(), Permissions.PLAYER_MUTE_UNLIMITED))
            return MuteResponse.FULL_MUTE_LIST;

        MuteEntry muteEntry = new MuteEntry(getPlayerId(), playerId);

        // Check if player was already muted
        if(pendingMute.contains(muteEntry) || mutedPlayers.contains(muteEntry)) return MuteResponse.ALREADY_MUTED;

        // If already requested to be removed, reinsert it
        int pendingUnmuteIndex = pendingUnmute.indexOf(muteEntry);
        if(pendingUnmuteIndex >= 0)
            mutedPlayers.add(pendingUnmute.remove(pendingUnmuteIndex)); // reinsert the actual muteEntry and not our copy
        else  // else, add it to pending mute list
            pendingMute.add(muteEntry);

        return MuteResponse.SUCCESSFULLY_MUTED;
    }

    public UnmuteResponse unmutePlayer(long playerId) {
        MuteEntry muteEntry = new MuteEntry(getPlayerId(), playerId);

        // Remove from pending mute and return successful if removed
        if(pendingMute.remove(muteEntry)) return UnmuteResponse.SUCCESSFULLY_UNMUTED;

        // Check if already asked to be removed
        if(pendingUnmute.contains(muteEntry)) return UnmuteResponse.ALREADY_UNMUTED;

        // if it is muted, remove it and put on pending unmute
        int mutedPlayerIndex = mutedPlayers.indexOf(muteEntry);
        if(mutedPlayerIndex >= 0)
            pendingUnmute.add(mutedPlayers.remove(mutedPlayerIndex)); // get the actual muteEntry and not our copy
        else
            return UnmuteResponse.NEVER_WAS_MUTED;

        return UnmuteResponse.SUCCESSFULLY_UNMUTED;
    }

    public boolean sendServerMessage(@NotNull String string) {
        if(!chatState.accepts(ChatState.MessageType.SERVER_MESSAGE))
            return false;

        sendMessage(string);
        return true;
    }

    public boolean sendPrivateMessage(@Nullable CommandSender sender, @NotNull String message) {
        // Console (CommandSender) can't be denied
        if(sender instanceof Player)
            throw new IllegalArgumentException("Use ChatProfile#sendPrivateMessage(PlayerHandler, String)");

        // Update last private message sender
        synchronized(privateSenderLock) {
            lastPrivateSender = sender;
        }

        // TODO format
        sendMessage(message);
        // TODO format
        // TODO if(sender instanceof Player) is always false -- why?
        sender.sendMessage(message);
        return true;
    }

    public boolean sendPrivateMessage(@Nullable PlayerHandler sender, @NotNull String message) {
        // Check if player is himself OR if player can receive message from sender
        if(sender.equals(getPlayerHandler()) || !chatState.accepts(ChatState.MessageType.PRIVATE_MESSAGE) || isPlayerMuted(sender))
            return false;

        // Update last private message sender
        synchronized(privateSenderLock) {
            lastPrivateSender = sender.getPlayer();
        }

        // TODO format
        sendMessage(message);
        // TODO format
        sender.getProfile(ChatProfile.class).sendMessage(message);
        return true;
    }

    public boolean sendChatMessage(@NotNull PlayerHandler sender, @NotNull String message) {
        // Check if sender isn't himself AND (player can't receive chat from sender or sender is muted)
        if(!sender.equals(getPlayerHandler()) && (!chatState.accepts(ChatState.MessageType.CHAT_MESSAGE) || isPlayerMuted(sender)))
            return false;

        // TODO format
        sendMessage(message);
        return true;
    }

    private void sendMessage(@NotNull String string) {
        if(usingFixedChat)
            synchronized(chatMessagesLock) {
                int lastIndex = chatMessages.size() - 1;

                // Check if chat messages list isn't full
                if(chatMessages.get(chatMessages.size() - 1) == null) {
                    // Get the highest index possible
                    for(int i = chatMessages.size() - 2; i >= 0; i--) {
                        String index = chatMessages.get(i);

                        // If index isn't used, mark as latest
                        if(index == null)
                            lastIndex = i;
                        else // stop if found an string
                            break;
                    }

                    // Since list isn't full, set the message on the latest empty index
                    chatMessages.set(lastIndex, string);
                } else {
                    // Shift every message backwards, leaving the last one
                    for(int i = 0; i < chatMessages.size() - 1; i++)
                        chatMessages.set(i, chatMessages.get(i + 1));

                    // Set the last one
                    chatMessages.set(lastIndex, string);
                }
            }
        else
            getPlayerHandler().getPlayer().sendMessage(string);
    }

    private boolean isPlayerMuted(@NotNull PlayerHandler playerHandler) {
        long playerId = playerHandler.getProfile(UserProfile.class).getPlayerId();

        // Check if player can't be muted
        if(PacocaCraft.permission.has(playerHandler.getPlayer(), Permissions.PLAYER_MUTE_EXCEPTION))
            return false;

        MuteEntry muteEntry = new MuteEntry(getPlayerId(), playerId);
        // Return true if player is muted or pending mute
        return pendingMute.contains(muteEntry) || mutedPlayers.contains(muteEntry);
    }

    /*
     * Getters and setters
     */

    public void setChatState(@NotNull ChatState chatState) {
        this.chatState = chatState;
    }

    public void setUsingFixedChat(boolean usingFixedChat) {
        this.usingFixedChat = usingFixedChat;

        if(usingFixedChat && fixedChatTask == null) {
            fixedChatTask = BukkitScheduler.runTaskTimerAsynchronously(new FixedChatTask(), 0L, FIXED_CHAT_REFRESH_DELAY);
        } else if(!usingFixedChat && fixedChatTask != null) {
            fixedChatTask.cancel();
            fixedChatTask = null;
        }
    }

    public CommandSender getLastPrivateSender() {
        return lastPrivateSender;
    }

    public void setLastPrivateSender(CommandSender lastPrivateSender) {
        this.lastPrivateSender = lastPrivateSender;
    }

    /**
     * Must run asynchronously as it'll search through database (on low priority)
     *
     * @return player's names
     *
     * @deprecated uses MySQL, run async
     */
    @SuppressWarnings("Convert2streamapi")
    @Deprecated
    public List<String> getMutedPlayers() throws SQLException {
        // Variables needed
        int numberOfPlayers = pendingMute.size() + mutedPlayers.size();
        LinkedList<Long> idsToSearch = new LinkedList<>();
        ArrayList<String> playerNames = new ArrayList<>(numberOfPlayers);

        // Add all ids to the list
        for(MuteEntry muteEntry : pendingMute)
            idsToSearch.add(muteEntry.getModeratorPlayerId());
        for(MuteEntry mutedPlayer : mutedPlayers)
            idsToSearch.add(mutedPlayer.getModeratorPlayerId());

        { // MySQL start
            // Get a new connection from the pool
            Connection connection = PacocaCraft.dataSource.getConnection();
            {
                // Make query SQL
                StringBuilder queryBuilder = new StringBuilder("SELECT `playerName` FROM minecraft.user_profiles WHERE `playerId` IN (");

                // Add all ids to the query
                for(int i = 0; i < idsToSearch.size(); i++) {
                    // Append "id" and ", " if not the last one
                    queryBuilder.append(idsToSearch.get(i));
                    if(i != idsToSearch.size() - 1) queryBuilder.append(", ");
                }

                // Close query
                queryBuilder.append(");");

                // Prepare and execute statement
                PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString());
                ResultSet resultSet = preparedStatement.executeQuery();

                // Iterate through all entries
                while(resultSet.next()) {
                    // Add player name to list
                    playerNames.add(resultSet.getString("playerName"));
                }

                // Close ResultSet and Statement
                resultSet.close();
                preparedStatement.close();
            }
            // Close connection
            connection.close();
        }

        return playerNames;
    }

    /*
     * MySQL methods
     */

    public static void fetchChatProfile(@NotNull ChatProfile chatProfile) throws SQLException {
        // Retrieve connection
        Connection connection = PacocaCraft.dataSource.getConnection();
        {
            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM minecraft.muted_players WHERE `user_playerId` = ? ORDER BY `muteDate` DESC;",
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_UPDATABLE
            );

            preparedStatement.setLong(1, chatProfile.getPlayerId());

            // Execute statement
            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<MuteEntry> mutedPlayers = new ArrayList<>();

            // While there are muted players
            while(resultSet.next()) {
                // If number exceeds limit, remove it; put on the map otherwise
                if(mutedPlayers.size() > MAXIMUM_NUMBER_MUTED_PLAYERS) // old ones are unmuted
                    resultSet.deleteRow();
                else
                    mutedPlayers.add(
                            new MuteEntry(
                                    resultSet.getLong("mute_index"),
                                    resultSet.getLong("user_playerId"),
                                    resultSet.getLong("user_moderatorId"),
                                    resultSet.getLong("muteDate")
                            )
                    );
            }

            // Set variables as same as the previous constructor
            chatProfile.mutedPlayers.addAll(mutedPlayers);
            chatProfile.databaseState = DatabaseState.ON_DATABASE;

            // Close statement and ResultSet
            resultSet.close();
            preparedStatement.close();
        }
        // Close connection
        connection.close();
    }

    public static void saveProfile(@NotNull ChatProfile chatProfile) throws SQLException {
        if(!chatProfile.shouldBeSaved()) return;

        // Get connection
        Connection connection = PacocaCraft.dataSource.getConnection();
        { // Insert pending mute
            List<MuteEntry> pendingMute = chatProfile.pendingMute;

            // Check if there are ones pending to be saved
            if(pendingMute.size() > 0) {

                // Build the string
                StringBuilder sqlQuery = new StringBuilder();
                sqlQuery.append("INSERT INTO `minecraft`.`muted_players` (`user_playerId`, `user_moderatorId`, `muteDate`) VALUES ");

                for(int i = 0; i < pendingMute.size(); i++) {
                    sqlQuery.append("(?, ?, ?)");
                    // The last one SHOULD NOT have a comma
                    if(i != (pendingMute.size() - 1)) sqlQuery.append(", ");
                }

                // Prepare statement
                PreparedStatement preparedStatement = connection.prepareStatement(
                        sqlQuery.toString(),
                        Statement.RETURN_GENERATED_KEYS
                );

                // Set every MuteEntry data
                for(int index = 0; index < pendingMute.size(); index++) {
                    MuteEntry muteEntry = pendingMute.get(index);

                    preparedStatement.setLong(1 + (index * 3), muteEntry.getPlayerId());
                    preparedStatement.setLong(2 + (index * 3), muteEntry.getModeratorPlayerId());
                    preparedStatement.setLong(3 + (index * 3), muteEntry.getMuteDate());
                }

                // Execute query
                preparedStatement.execute();

                // Get generated keys
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

                // Update mute index for every MuteEntry
                for(int i = 0; generatedKeys.next(); i++) {
                    MuteEntry muteEntry = pendingMute.remove(i);
                    muteEntry.setMuteIndex(generatedKeys.getLong("mute_index"));
                    chatProfile.mutedPlayers.add(muteEntry);
                }

                // Close everything
                generatedKeys.close();
                preparedStatement.close();
            }

            // Clear pending list - Note: make sure to add them to mutedPlayer list
            pendingMute.clear();
        }
        { // Remove pending unmute
            List<MuteEntry> pendingUnmute = chatProfile.pendingUnmute;

            // Check if there are ones pending to be removed
            if(!pendingUnmute.isEmpty()) {

                // Build the string
                StringBuilder sqlStatement = new StringBuilder();
                sqlStatement.append("DELETE FROM `minecraft`.`muted_players` WHERE `mute_index` IN (");

                for(int i = 0; i < pendingUnmute.size(); i++) {
                    sqlStatement.append("?");
                    // The last one SHOULD NOT have a comma
                    if(i != (pendingUnmute.size() - 1)) sqlStatement.append(", ");
                }

                sqlStatement.append(");");

                // Prepare statement
                PreparedStatement preparedStatement = connection.prepareStatement(
                        sqlStatement.toString()
                );

                // Set variables for each MuteEntry
                for(int index = 0; index < pendingUnmute.size(); index++) {
                    preparedStatement.setLong(index, pendingUnmute.get(index).getMuteIndex());
                }

                // Execute statement
                preparedStatement.execute();

                // Close everything
                preparedStatement.close();
            }

            // Clear pending list
            pendingUnmute.clear();
        }
        // Finally, close connection
        connection.close();
    }

    protected class FixedChatTask implements Runnable {

        @Override
        public void run() {
            // TODO maybe packets? is it faster? Need testing on a high amount of players
            synchronized(chatMessagesLock) {
                for(int i = 0; i < chatMessages.size(); i++)
                    getPlayerHandler().getPlayer().sendMessage(chatMessages.get(i));
            }
        }
    }

    public enum MuteResponse {
        ALREADY_MUTED, FULL_MUTE_LIST, SUCCESSFULLY_MUTED
    }

    public enum UnmuteResponse {
        SUCCESSFULLY_UNMUTED, ALREADY_UNMUTED, NEVER_WAS_MUTED
    }
}
