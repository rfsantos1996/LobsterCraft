package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.services.Service;
import com.jabyftw.lobstercraft.services.services_event.*;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.CityOccupation;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.NumberConversions;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

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
public class PlayerHandlerService extends Service {

    // Lock used when multiple maps are used on the same space
    private final Object playerMapsLock = new Object();

    /*
     * Online players
     */
    private final ConcurrentHashMap<Player, OnlinePlayer> onlinePlayers_player = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<OfflinePlayer, OnlinePlayer> onlinePlayers_offlinePlayer = new ConcurrentHashMap<>();

    /*
     * Offline players
     */
    private final ConcurrentHashMap<Integer, HashSet<OfflinePlayer>> registeredOfflinePlayers_cityId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OfflinePlayer> registeredOfflinePlayers_name = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, OfflinePlayer> registeredOfflinePlayers_id = new ConcurrentHashMap<>();
    // Unregistered players that may be going to rejoin the server before it closes
    private final ConcurrentHashMap<String, OfflinePlayer> unregisteredOfflinePlayers_name = new ConcurrentHashMap<>();

    /*
     * Profile handling
     */
    private final ConcurrentHashMap<Integer, ProfileStorage> playerProfiles = new ConcurrentHashMap<>();
    private final ProfileUnloader profileUnloader;
    private static final long
            TIME_PROFILE_IS_STORED_MILLISECONDS = TimeUnit.SECONDS.toMillis(LobsterCraft.configuration.getLong(ConfigurationValues.PLAYER_TIME_PROFILE_KEPT_SECONDS.toString())),
            PROFILE_SAVING_PERIOD_TICKS = LobsterCraft.configuration.getLong(ConfigurationValues.PLAYER_TIME_BETWEEN_PROFILE_SAVES_TICKS.toString());
    private Connection connection;

    /*
     * Player limiter for AsyncPlayerPreLoginEvent
     *
     * Number of ticks per player join, this must be the time difference between 2 "joins"
     * Unit: (players per second / ticks per second)^(-1) => (ticks per player)
     */
    private final static int TICKS_NEEDED_BETWEEN_JOINS =
            NumberConversions.ceil(LobsterCraft.configuration.getInt(ConfigurationValues.LOGIN_LIMITER_PLAYERS_FOR_PERIOD.toString()) /
                    (LobsterCraft.configuration.getInt(ConfigurationValues.LOGIN_LIMITER_PLAYERS_PERIOD_OF_TIME_SECONDS.toString()) * 20.0D));
    private final Object playerJoinedLock = new Object();
    private long lastPlayerJoinedTick = -1; // We will lock with playerJoinedLock

    /*
     * Player name changes
     */
    private final static long
            REQUIRED_TIME_TO_ALLOW_NAME = TimeUnit.DAYS.toMillis(LobsterCraft.configuration.getLong(ConfigurationValues.LOGIN_NAME_CHANGE_USERNAME_AVAILABLE_DAYS.toString())),
            PLAYER_CAN_CHANGE_NAME_AGAIN = TimeUnit.DAYS.toMillis(LobsterCraft.configuration.getLong(ConfigurationValues.LOGIN_NAME_CHANGE_PLAYER_ALLOWED_TO_CHANGE_DAYS.toString()));
    private final List<String> blacklistedNames = LobsterCraft.configuration.getStringList(ConfigurationValues.PLAYER_NAME_BLACKLIST.toString());
    private final ConcurrentHashMap<Integer, NameChangeEntry> nameChangeEntries = new ConcurrentHashMap<>();

    /*
     * Ban entries
     */
    private final ConcurrentHashMap<Integer, HashSet<BannedPlayerEntry>> playerBanEntries = new ConcurrentHashMap<>();

    public PlayerHandlerService() throws SQLException {
        // Register service
        super();

        // Create database cache
        connection = LobsterCraft.dataSource.getConnection(); // connection used on profile unloader
        cacheOfflinePlayers(connection);
        cachePlayerNameChanges(connection);
        cachePlayerBanRecords(connection);

        // Register our listeners
        Bukkit.getServer().getPluginManager().registerEvents(new CustomEventsListener(), LobsterCraft.plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new PreLoginListener(), LobsterCraft.plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new TeleportListener(), LobsterCraft.plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new SafePlayerActionsListener(), LobsterCraft.plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new XrayListener(), LobsterCraft.plugin);

        // Register profile unloader
        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(
                LobsterCraft.plugin,
                (profileUnloader = new ProfileUnloader()),
                PROFILE_SAVING_PERIOD_TICKS,
                PROFILE_SAVING_PERIOD_TICKS
        );
    }

    @Override
    public void onDisable() {
        try {
            checkConnection();

            // Make sure all profiles are saved
            synchronized (playerProfiles) {
                while (!playerProfiles.isEmpty()) profileUnloader.run();
            }

            // Save our cache
            saveChangedPlayers(connection);
            saveChangedPlayerNames(connection);

            // Finally, close our connection
            connection.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    /*
     * Player management
     */

    /**
     * Get offline player from cache, if registered
     * Note: this will retrieve an OfflinePlayer even if the player is online
     *
     * @param playerName valid player name (doesn't need to be lower cased)
     * @return offline player from database, if registered; default offline player, otherwise
     * @throws IllegalArgumentException if player name isn't valid
     */
    public OfflinePlayer getOfflinePlayer(@NotNull final String playerName) throws IllegalArgumentException {
        OfflinePlayer offlinePlayer;
        final String loweredPlayerName = playerName.toLowerCase();

        synchronized (playerMapsLock) {
            // Retrieve from registered names
            if ((offlinePlayer = registeredOfflinePlayers_name.get(loweredPlayerName)) != null) {
                return offlinePlayer;
            } else if ((offlinePlayer = unregisteredOfflinePlayers_name.get(loweredPlayerName)) != null) { // unregistered players that might be going to rejoin the
                // server
                return offlinePlayer;
            } else {
                return new OfflinePlayer(loweredPlayerName);
            }
        }
    }

    /**
     * Get offline player from cache using player's id
     * Note: this will retrieve an OfflinePlayer even if the player is online
     *
     * @param playerId <b>EXISTING</b> player id
     * @return offline player for that id; null, if none found
     * @see PlayerHandlerService#getOfflinePlayer(String) that method needs locking because it'll check 2 lists simultaneously, while this method will rely on Map's
     * synchronization (it's thread safe). The player doesn't need to be on the "string"-key Map to get a not-null object returned here.
     */
    public OfflinePlayer getOfflinePlayer(int playerId) {
        return registeredOfflinePlayers_id.get(playerId);
    }

    /**
     * Retrieve OnlinePlayer for Bukkit's player instance
     * <p>
     * OnlinePlayer will be available between PlayerJoinEvent with priority set to LOW and PlayerQuitEvent with priority set to HIGHEST
     *
     * @param player      Bukkit's player instance
     * @param onlineState player's online state, null will search for any player
     * @return online player; null, if none found
     * @see PlayerHandlerService#getOfflinePlayer(String) that method needs locking because it'll check 2 lists simultaneously, while this method will rely on Map's
     * synchronization (it's thread safe). The player doesn't need to be on the "string"-key Map to get a not-null object returned here.
     */
    public OnlinePlayer getOnlinePlayer(@NotNull Player player, @Nullable OnlinePlayer.OnlineState onlineState) {
        OnlinePlayer onlinePlayer = onlinePlayers_player.get(player);
        if (onlinePlayer == null) return null;
        else return onlineState == null || onlinePlayer.getOnlineState() == onlineState ? onlinePlayer : null;
    }

    /**
     * Retrieve OnlinePlayer for OfflinePlayer's instance
     * <p>
     * OnlinePlayer will be available between PlayerJoinEvent with priority set to LOW and PlayerQuitEvent with priority set to HIGHEST
     *
     * @param offlinePlayer OfflinePlayer instance to search
     * @param onlineState   player's online state, null will search for any player
     * @return online player; null, if none found
     * @see PlayerHandlerService#getOfflinePlayer(String) that method needs locking because it'll check 2 lists simultaneously, while this method will rely on Map's
     * synchronization (it's thread safe). The player doesn't need to be on the "string"-key Map to get a not-null object returned here.
     */
    public OnlinePlayer getOnlinePlayer(@NotNull OfflinePlayer offlinePlayer, @Nullable OnlinePlayer.OnlineState onlineState) {
        OnlinePlayer onlinePlayer = onlinePlayers_offlinePlayer.get(offlinePlayer);
        if (onlinePlayer == null) return null;
        else return onlineState == null || onlinePlayer.getOnlineState() == onlineState ? onlinePlayer : null;
    }

    /**
     * Retrieve OnlinePlayer for player's name
     * <p>
     * OnlinePlayer will be available between PlayerJoinEvent with priority set to LOW and PlayerQuitEvent with priority set to HIGHEST
     *
     * @param playerName  player's exact name
     * @param onlineState player's online state, null will search for any player
     * @return online player; null, if none found
     * @see PlayerHandlerService#getOfflinePlayer(String) that method needs locking because it'll check 2 lists simultaneously, while this method will rely on Map's
     * synchronization (it's thread safe). The player doesn't need to be on the "string"-key Map to get a not-null object returned here.
     */
    public OnlinePlayer getOnlinePlayer(@NotNull String playerName, @Nullable OnlinePlayer.OnlineState onlineState) {
        OnlinePlayer onlinePlayer = onlinePlayers_offlinePlayer.get(getOfflinePlayer(playerName));
        if (onlinePlayer == null) return null;
        else return onlineState == null || onlinePlayer.getOnlineState() == onlineState ? onlinePlayer : null;
    }

    /**
     * Retrieve OnlinePlayer for player name (will use the most "perfect" name)
     * <p>
     * OnlinePlayer will be available between PlayerJoinEvent with priority set to LOW and PlayerQuitEvent with priority set to HIGHEST
     *
     * @param string      player name to search
     * @param onlineState player's online state, null will search for any player
     * @return matched online player; null, if none found
     */
    public OnlinePlayer matchOnlinePlayer(@NotNull String string, @Nullable OnlinePlayer.OnlineState onlineState) {
        if (string.length() < 3)
            return null;

        OnlinePlayer mostEqual = getOnlinePlayer(string, onlineState);
        int equalSize = 3;

        // Check if name is exact
        if (mostEqual != null) return mostEqual;

        synchronized (playerMapsLock) {
            for (OnlinePlayer onlinePlayer : onlinePlayers_offlinePlayer.values()) {
                int thisSize = Util.getEqualityOfNames(string.toCharArray(), onlinePlayer.getOfflinePlayer().getPlayerName().toCharArray());

                if (thisSize >= equalSize) {
                    mostEqual = onlinePlayer;
                    equalSize = thisSize;
                }
            }
        }
        return mostEqual != null && (onlineState == null || mostEqual.getOnlineState() == onlineState) ? mostEqual : null;
    }

    /**
     * Retrieve registered OfflinePlayer by player name (will use the most "perfect" name)
     *
     * @param string player name to search
     * @return matched online player; null, if none found
     */
    public OfflinePlayer matchOfflinePlayer(@NotNull String string) {
        if (string.length() < 3)
            return null;

        OfflinePlayer mostEqual = registeredOfflinePlayers_name.get(string.toLowerCase());
        int equalSize = 3;

        // Check if name is exact
        if (mostEqual != null) return mostEqual;

        synchronized (playerMapsLock) {
            for (OfflinePlayer offlinePlayer : registeredOfflinePlayers_name.values()) {
                int thisSize = Util.getEqualityOfNames(string.toCharArray(), offlinePlayer.getPlayerName().toCharArray());

                if (thisSize >= equalSize) {
                    mostEqual = offlinePlayer;
                    equalSize = thisSize;
                }
            }
        }
        return mostEqual;
    }

    /**
     * Retrieve a list of online players
     *
     * @param onlineState null means no filtering at all
     * @return a Set from HashSet with all online players filtered by given onlineState
     */
    public Set<OnlinePlayer> getOnlinePlayers(@Nullable OnlinePlayer.OnlineState onlineState) {
        HashSet<OnlinePlayer> playerSet = new HashSet<>();

        // Filter logged players
        synchronized (playerMapsLock) {
            for (OnlinePlayer onlinePlayer : onlinePlayers_player.values())
                if (onlineState == null || onlinePlayer.getOnlineState() == onlineState)
                    playerSet.add(onlinePlayer);
        }

        return playerSet;
    }

    /**
     * Retrieve a list of offline players for given cityId
     *
     * @param cityId null means no filtering at all
     * @return a Set from HashSet with all offline players filtered by given city id
     */
    public Set<OfflinePlayer> getOfflinePlayersPlayersForCity(int cityId) {
        // Filter logged players
        synchronized (playerMapsLock) {
            return registeredOfflinePlayers_cityId.get(cityId);
        }
    }

    /**
     * Retrieve a list of online players
     *
     * @param cityId      city to search for
     * @param onlineState null means no filtering at all
     * @return a Set from HashSet with all online players filtered by given onlineState and cityId
     */
    public Set<OnlinePlayer> getOnlinePlayersForCity(int cityId, @Nullable OnlinePlayer.OnlineState onlineState) {
        HashSet<OnlinePlayer> playerSet = new HashSet<>();

        // Filter logged players
        synchronized (playerMapsLock) {
            for (OfflinePlayer offlinePlayer : registeredOfflinePlayers_cityId.get(cityId)) {
                OnlinePlayer onlinePlayer;
                if ((onlinePlayer = offlinePlayer.getOnlinePlayer(onlineState)) != null)
                    playerSet.add(onlinePlayer);
            }
        }

        return playerSet;
    }

    /**
     * Register player
     *
     * @param encryptedPassword player's encrypted password
     * @return a LoginResponse to send the CommandSender ("LOGIN_WENT_ASYNCHRONOUS_SUCCESSFULLY" is a success)
     * @see Util#encryptString(String) for password encrypting
     */
    public OnlinePlayer.LoginResponse registerPlayer(@NotNull final OnlinePlayer onlinePlayer, @NotNull final String encryptedPassword) {
        final OfflinePlayer offlinePlayer = onlinePlayer.getOfflinePlayer();
        // Check if player is registered
        if (offlinePlayer.isRegistered())
            return OnlinePlayer.LoginResponse.ALREADY_REGISTERED;

        Bukkit.getScheduler().runTaskAsynchronously(LobsterCraft.plugin,
                () -> {
                    try {
                        // Set offline player's attributes (lastIp is just set on login)
                        offlinePlayer.lastIp = onlinePlayer.getPlayer().getAddress().getAddress().getHostAddress();
                        offlinePlayer.encryptedPassword = encryptedPassword;
                        offlinePlayer.databaseState = DatabaseState.INSERT_TO_DATABASE;

                        // Register player on database
                        Connection connection = LobsterCraft.dataSource.getConnection();

                        // Prepare statement
                        PreparedStatement preparedStatement = connection.prepareStatement(
                                "INSERT INTO `minecraft`.`user_profiles`" +
                                        "(`playerName`, `password`, `moneyAmount`, `city_cityId`, `cityOccupation`, `lastTimeOnline`, `timePlayed`, `lastIp`)" +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
                                Statement.RETURN_GENERATED_KEYS
                        );

                        // Set variables
                        preparedStatement.setString(1, offlinePlayer.getPlayerName().toLowerCase()); // Lower case it just to make sure
                        preparedStatement.setString(2, offlinePlayer.getEncryptedPassword());
                        preparedStatement.setDouble(3, offlinePlayer.getMoneyAmount());
                        preparedStatement.setObject(4, offlinePlayer.getCityId(), Types.SMALLINT); // Will write null if is null
                        preparedStatement.setObject(5, offlinePlayer.getCityOccupation() != null ? offlinePlayer.getCityOccupation().getOccupationId() : null, Types.TINYINT);
                        preparedStatement.setLong(6, offlinePlayer.getLastTimeOnline());
                        preparedStatement.setLong(7, offlinePlayer.getTimePlayed());
                        preparedStatement.setString(8, offlinePlayer.getLastIp());

                        // Execute statement
                        preparedStatement.execute();

                        // Retrieve generated keys
                        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

                        // Check if key exists
                        if (!generatedKeys.next())
                            throw new SQLException("Query didn't return any generated key");

                        offlinePlayer.playerId = generatedKeys.getInt("playerId");

                        // Close everything
                        generatedKeys.close();
                        preparedStatement.close();
                        connection.close();

                        // Check if was successful
                        if (offlinePlayer.getPlayerId() == null || offlinePlayer.getPlayerId() <= 0)
                            throw new IllegalStateException(Util.appendStrings("Failed to register player: playerId is ", offlinePlayer.getPlayerId()));

                        // Change player's instance location
                        synchronized (playerMapsLock) {
                            unregisteredOfflinePlayers_name.remove(offlinePlayer.getPlayerName(), offlinePlayer);
                            registeredOfflinePlayers_name.put(offlinePlayer.getPlayerName(), offlinePlayer);
                            registeredOfflinePlayers_id.put(offlinePlayer.getPlayerId(), offlinePlayer);

                            // Check if player has a city (even though he just registered...)
                            if (offlinePlayer.getCityId() != null) {
                                if (!registeredOfflinePlayers_cityId.containsKey(offlinePlayer.getCityId()))
                                    registeredOfflinePlayers_cityId.put(offlinePlayer.getCityId(), new HashSet<>());
                                registeredOfflinePlayers_cityId.get(offlinePlayer.getCityId()).add(offlinePlayer);
                            }
                        }

                        // Update database state
                        offlinePlayer.databaseState = DatabaseState.ON_DATABASE;
                        onlinePlayer.onlineState = OnlinePlayer.OnlineState.PRE_LOGIN;

                        // Force login
                        forceLoginPlayer(onlinePlayer);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        onlinePlayer.getPlayer().kickPlayer("§4Um erro ocorreu ao registrar!");
                    }
                });

        return OnlinePlayer.LoginResponse.LOGIN_WENT_ASYNCHRONOUS_SUCCESSFULLY;
    }

    /**
     * Force player to login
     *
     * @param onlinePlayer player to force login
     * @return a response to the possible CommandSender
     */
    public OnlinePlayer.LoginResponse forceLoginPlayer(@NotNull OnlinePlayer onlinePlayer) {
        return onlinePlayer.loginPlayer();
    }

    /**
     * Change player's password (the player <b>MUST</b> be registered)
     * Note: you should check if the player knows old password
     *
     * @param offlinePlayer        player to get the password changed
     * @param newEncryptedPassword new encrypted password
     * @return true if the password was changed
     * @see Util#encryptString(String) for password encrypting
     */
    public boolean changePlayerPassword(@NotNull final OfflinePlayer offlinePlayer, @NotNull final String newEncryptedPassword) {
        // Check if player is registered
        if (!offlinePlayer.isRegistered())
            return false;

        // Update password
        offlinePlayer.encryptedPassword = newEncryptedPassword;
        offlinePlayer.databaseState = DatabaseState.UPDATE_DATABASE;
        return true;
    }

    /**
     * Change player's name (the player <b>MUST</b> be registered)
     * Note: you should check if player knows the password before
     *
     * @param offlinePlayer player that will receive the new name
     * @param newPlayerName player's new name, will be lower cased
     * @return result of change
     */
    public ChangeNameResponse changePlayerName(@NotNull final OfflinePlayer offlinePlayer, @NotNull final String newPlayerName) {
        final String newPlayerNameLowered = newPlayerName.toLowerCase(),
                oldPlayerName = offlinePlayer.playerName;
        OnlinePlayer onlinePlayer = offlinePlayer.getOnlinePlayer(null);
        OfflinePlayer hypotheticalOfflinePlayer;

        // Check if name is valid
        if (!Util.checkStringCharactersAndLength(newPlayerNameLowered, 3, 16))
            return ChangeNameResponse.NAME_INVALID;

        synchronized (nameChangeEntries) {
            for (NameChangeEntry entry : nameChangeEntries.values())
                // Check if name is a recent change that isn't offlinePlayer's
                if (entry.getPlayerId() != offlinePlayer.getPlayerId() && !entry.isNameAvailable() && entry.getOldPlayerName().equalsIgnoreCase(newPlayerNameLowered))
                    return ChangeNameResponse.NAME_PROTECTED;

            // Check player name is valid
            synchronized (playerMapsLock) {
                // Don't need to catch IllegalArgumentException, name is already checked above
                hypotheticalOfflinePlayer = getOfflinePlayer(newPlayerNameLowered);

                // Check if player is registered
                if (hypotheticalOfflinePlayer.isRegistered())
                    return ChangeNameResponse.NAME_UNAVAILABLE;

                // Insert name change record
                NameChangeEntry oldNameEntry = nameChangeEntries.putIfAbsent(offlinePlayer.getPlayerId(), new NameChangeEntry(offlinePlayer));
                // If old existed, the new NameChangeEntry (created above) wasn't inserted and, because of this, we should update the old one
                if (oldNameEntry != null)
                    // Check if player can change its name again
                    if (oldNameEntry.canPlayerChangeNameAgain())
                        oldNameEntry.changeNameAgain(oldPlayerName);
                    else
                        return ChangeNameResponse.CANT_CHANGE_NAME_YET;

                // Remove from registered player's map
                if (!registeredOfflinePlayers_name.remove(offlinePlayer.getPlayerName(), offlinePlayer))
                    return ChangeNameResponse.ERROR_OCCURRED;

                // Store Bukkit's OfflinePlayer for posterior permission transfer
                org.bukkit.OfflinePlayer oldBukkitOfflinePlayer = offlinePlayer.getBukkitOfflinePlayer(),
                        newBukkitOfflinePlayer = hypotheticalOfflinePlayer.getBukkitOfflinePlayer();

                // If there is an online player, kick it before we corrupt any online player map
                if (onlinePlayer != null)
                    onlinePlayers_offlinePlayer.remove(offlinePlayer, onlinePlayer);

                // Update player name
                offlinePlayer.playerName = newPlayerNameLowered;
                offlinePlayer.databaseState = DatabaseState.UPDATE_DATABASE;

                // Re-insert even though OnlinePlayer will contain an outdated name
                if (onlinePlayer != null)
                    onlinePlayers_offlinePlayer.put(offlinePlayer, onlinePlayer);

                Permission permission = LobsterCraft.permission;
                String primaryGroup = permission.getPrimaryGroup(null, oldBukkitOfflinePlayer);

                // Transfer permissions
                boolean removeGroup = permission.playerRemoveGroup(null, oldBukkitOfflinePlayer, primaryGroup);
                boolean addGroup = permission.playerAddGroup(null, newBukkitOfflinePlayer, primaryGroup);

                LobsterCraft.logger.config(Util.appendStrings("Player change name: ", oldPlayerName, " -> ", newPlayerName, "; removed old from group? ", removeGroup,
                        " added new to group? ", addGroup));

                // Reinsert player on map
                registeredOfflinePlayers_name.put(newPlayerNameLowered, offlinePlayer);
            }
        }

        // Kick player if is online
        if (onlinePlayer != null)
            onlinePlayer.getPlayer().kickPlayer(Util.appendStrings("§aNome alterado para §6\"", newPlayerName, "\"§a!"));
        return ChangeNameResponse.SUCCESSFULLY_CHANGED;
    }

    /**
     * Retrieves the entire history of bans for player.
     *
     * @param playerId player's id
     * @return an UNMODIFIABLE set of player's records
     */
    public Set<BannedPlayerEntry> getPlayerBanEntries(int playerId) {
        return Collections.unmodifiableSet(playerBanEntries.getOrDefault(playerId, new HashSet<>()));
    }

    /**
     * Kick or ban player, online or not. This won't announce to the server and will keep a record on MySQL. This method <b>SHOULD</b> run asynchronously.
     *
     * @param offlinePlayer  player to be kicked
     * @param banType        kick type
     * @param reason         reason to be at record, from 4 to 120 characters
     * @param moderatorId    moderator to be stored, can be null
     * @param bannedDuration ban duration, can be null
     * @return a ban response to the CommandSender
     */
    public BanResponse kickPlayer(@NotNull OfflinePlayer offlinePlayer, @NotNull final BanType banType, @NotNull final String reason, @Nullable Integer moderatorId,
                                  @Nullable final Long bannedDuration) {
        // Check if player is registered
        if (!offlinePlayer.isRegistered())
            return BanResponse.PLAYER_NOT_REGISTERED;

        // Set variables
        int playerId = offlinePlayer.getPlayerId();
        long recordDate = System.currentTimeMillis();

        // Check unban date
        Long unbanDate;
        if (banType != BanType.PLAYER_TEMPORARILY_BANNED) // Only temporary banned requires this argument
            unbanDate = null;
        else if (bannedDuration != null)
            unbanDate = recordDate + bannedDuration;
        else return BanResponse.BAN_DURATION_NOT_SET;

        // Check if reason has right size
        if (!Util.checkStringLength(reason, 4, 120))
            return BanResponse.INVALID_REASON_LENGTH;

        try {
            // Retrieve connection
            Connection connection = LobsterCraft.dataSource.getConnection();

            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    // 6 arguments
                    "INSERT INTO `minecraft`.`ban_records` (`user_playerId`, `user_moderatorId`, `banType`, `recordDate`, `reason`, `unbanDate`) VALUES (?, ?, ?, ?, ?, ?);",
                    Statement.RETURN_GENERATED_KEYS
            );

            // Set variables for query
            preparedStatement.setInt(1, playerId);
            preparedStatement.setObject(2, moderatorId, Types.INTEGER); // will write null if is null
            preparedStatement.setByte(3, banType.getTypeId());
            preparedStatement.setLong(4, recordDate);
            preparedStatement.setString(5, reason);
            preparedStatement.setObject(6, unbanDate, Types.BIGINT);

            // Execute statement
            preparedStatement.execute();

            // Retrieve generated keys
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            // Throw error if there is no generated key
            if (!generatedKeys.next())
                throw new SQLException("There is no generated key");

            // Create entry
            BannedPlayerEntry bannedPlayerEntry = new BannedPlayerEntry(
                    generatedKeys.getLong("recordId"),
                    moderatorId,
                    banType,
                    recordDate,
                    reason,
                    unbanDate
            );

            // Add entry to storage
            synchronized (playerBanEntries) {
                playerBanEntries.putIfAbsent(playerId, new HashSet<>());
                playerBanEntries.get(playerId).add(bannedPlayerEntry);
            }

            // Close everything
            generatedKeys.close();
            preparedStatement.close();
            connection.close();

            // Schedule player kick, if he is online
            OnlinePlayer onlinePlayer = offlinePlayer.getOnlinePlayer(null);
            if (onlinePlayer != null)
                Bukkit.getServer().getScheduler().runTask(
                        LobsterCraft.plugin,
                        () -> {
                            if (onlinePlayer.getPlayer().isOnline())
                                // Kick player if he is online
                                onlinePlayer.getPlayer().kickPlayer(bannedPlayerEntry.getKickMessage());
                        }
                );

            return BanResponse.SUCCESSFULLY_EXECUTED;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return BanResponse.ERROR_OCCURRED;
        }
    }

    /*
     * Database handling
     */

    /**
     * This method will cache every registered player. Should run on start, so we don't need to synchronize it.
     *
     * @param connection MySQL connection
     * @throws SQLException in case of something going wrong, should stop the server on start
     */
    private void cacheOfflinePlayers(@NotNull final Connection connection) throws SQLException {
        long start = System.nanoTime();

        // Prepare statement and execute query
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.user_profiles;");
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through all results
        while (resultSet.next()) {
            String playerName = resultSet.getString("playerName").toLowerCase();

            // Check for the nullity of some variables
            Integer cityId = resultSet.getInt("city_cityId");
            if (resultSet.wasNull()) cityId = null;

            Byte cityPositionId = resultSet.getByte("cityPositionId");
            if (resultSet.wasNull()) cityPositionId = null;

            OfflinePlayer offlinePlayer = new OfflinePlayer(
                    resultSet.getInt("playerId"),
                    playerName,
                    resultSet.getString("password"),
                    resultSet.getDouble("moneyAmount"),
                    cityId,
                    CityOccupation.fromId(cityPositionId),
                    resultSet.getLong("lastTimeOnline"),
                    resultSet.getLong("timePlayed"),
                    resultSet.getString("lastIp")
            );

            registeredOfflinePlayers_name.put(playerName, offlinePlayer);
            registeredOfflinePlayers_id.put(offlinePlayer.getPlayerId(), offlinePlayer);

            // Check if player has a city
            if (offlinePlayer.getCityId() != null) {
                if (!registeredOfflinePlayers_cityId.containsKey(offlinePlayer.getCityId()))
                    registeredOfflinePlayers_cityId.put(offlinePlayer.getCityId(), new HashSet<>());
                registeredOfflinePlayers_cityId.get(offlinePlayer.getCityId()).add(offlinePlayer);
            }
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();

        // Announce values
        LobsterCraft.logger.info(Util.appendStrings("Took us ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                "ms to retrieve ", registeredOfflinePlayers_id.size(), " players."));
    }

    /**
     * This method will cache every valid (not old) player name change record. Should run on start, so we don't need to synchronize it.
     *
     * @param connection MySQL connection
     * @throws SQLException in case of something going wrong, should stop the server on start
     */
    private void cachePlayerNameChanges(@NotNull final Connection connection) throws SQLException {
        long start = System.nanoTime();

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.player_name_changes WHERE changeDate > ?;");

        // Set the minimum date for this (we will only consider name changes that actually can deny a player join)
        preparedStatement.setLong(1, System.currentTimeMillis() - REQUIRED_TIME_TO_ALLOW_NAME);

        // Execute query
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through all entries
        while (resultSet.next()) {
            int playerId = resultSet.getInt("user_playerId");

            nameChangeEntries.put(
                    playerId,
                    new NameChangeEntry(
                            playerId,
                            resultSet.getString("oldPlayerName"),
                            resultSet.getLong("changeDate")
                    )
            );
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();

        // Announce values
        LobsterCraft.logger.info(Util.appendStrings("Took us ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                "ms to retrieve ", registeredOfflinePlayers_id.size(), " player name changes."));
    }

    /**
     * This method will cache every valid (not old) ban records that can deny a player join. Should run on start, so we don't need to synchronize it.
     *
     * @param connection MySQL connection
     * @throws SQLException in case of something going wrong, should stop the server on start
     */
    private void cachePlayerBanRecords(@NotNull final Connection connection) throws SQLException {
        long start = System.nanoTime();
        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `minecraft`.`ban_records`;"); // WHERE `banType` = ? OR `unbanDate` > ?;");

        // Set variables - do not filter, let's leave the history available for administrator commands
        //preparedStatement.setByte(1, BanType.PLAYER_PERMANENTLY_BANNED.getTypeId()); // will filter permanent ban
        //preparedStatement.setLong(2, System.currentTimeMillis()); // will filter unfinished temporary bans

        // Execute query
        ResultSet resultSet = preparedStatement.executeQuery();

        // Iterate through results
        while (resultSet.next()) {
            // Retrieve variables
            int playerId = resultSet.getInt("user_playerId");
            Integer moderatorId = resultSet.getInt("user_moderatorId");
            if (resultSet.wasNull())
                moderatorId = null;
            Long unbanDate = resultSet.getLong("unbanDate");
            if (resultSet.wasNull())
                unbanDate = null;

            // Insert base set
            playerBanEntries.putIfAbsent(playerId, new HashSet<>());
            // Insert entry
            playerBanEntries.get(playerId).add(new BannedPlayerEntry(
                    resultSet.getLong("recordId"),
                    moderatorId,
                    BanType.getBanType(resultSet.getByte("banType")),
                    resultSet.getLong("recordDate"),
                    resultSet.getString("reason"),
                    unbanDate
            ));
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();

        // Announce values
        LobsterCraft.logger.info(Util.appendStrings("Took us ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                "ms to retrieve ", playerBanEntries.size(), " ban records."));
    }

    /**
     * This should run on server close, so we don't need to synchronize as every player join is denied before.
     *
     * @param connection MySQL connection
     * @throws SQLException in case of something going wrong
     */
    private void saveChangedPlayers(@NotNull Connection connection) throws SQLException {
        long start = System.nanoTime();
        int numberOfPlayersUpdated = 0;

        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE `minecraft`.`user_profiles` SET `playerName` = ?, `password` = ?, `moneyAmount` = ?, `city_cityId` = ?," +
                        " `cityOccupation` = ?, `lastTimeOnline` = ?, `timePlayed` = ?, `lastIp` = ? WHERE `playerId` = ?;"
        );

        // Iterate through all players
        for (OfflinePlayer offlinePlayer : registeredOfflinePlayers_id.values())
            // Filter the ones needing updates => REGISTERED PLAYERS: they have money amount, passwords, last time online, last IP
            if (offlinePlayer.getDatabaseState() == DatabaseState.UPDATE_DATABASE) {
                preparedStatement.setString(1, offlinePlayer.getPlayerName());
                preparedStatement.setString(2, offlinePlayer.getEncryptedPassword());
                preparedStatement.setDouble(3, offlinePlayer.getMoneyAmount());
                preparedStatement.setObject(4, offlinePlayer.getCityId(), Types.INTEGER); // Will write null if is null
                preparedStatement.setObject(5, offlinePlayer.getCityOccupation() != null ? offlinePlayer.getCityOccupation().getOccupationId() : null, Types.TINYINT);
                preparedStatement.setLong(6, offlinePlayer.getLastTimeOnline());
                preparedStatement.setLong(7, offlinePlayer.getTimePlayed());
                preparedStatement.setString(8, offlinePlayer.getLastIp());
                preparedStatement.setLong(9, offlinePlayer.getPlayerId());

                // Add batch
                preparedStatement.addBatch();

                // Update their database state
                offlinePlayer.databaseState = DatabaseState.ON_DATABASE;
                numberOfPlayersUpdated++;
            }

        // Execute and announce
        if (numberOfPlayersUpdated > 0) {
            preparedStatement.executeBatch();
            LobsterCraft.logger.info(Util.appendStrings("Took us ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                    "ms to update ", numberOfPlayersUpdated, " players."));
        }

        // Close statement
        preparedStatement.close();
    }

    /**
     * This should run on server close, so we don't need to synchronize as every player join is denied before.
     *
     * @param connection MySQL connection
     * @throws SQLException in case of something going wrong
     */
    private void saveChangedPlayerNames(@NotNull Connection connection) throws SQLException {
        long start = System.nanoTime();
        int numberOfEntriesUpdated = 0, numberOfEntriesInserted = 0;

        // Prepare statements
        PreparedStatement updateStatement = connection.prepareStatement(
                "UPDATE `minecraft`.`player_name_changes` SET `oldPlayerName` = ?, `changeDate` = ? WHERE `user_playerId` = ?;"
        );
        PreparedStatement insertStatement = connection.prepareStatement(
                "INSERT INTO `minecraft`.`player_name_changes` (`user_playerId`, `oldPlayerName`, `changeDate`) VALUES  (?, ?, ?);"
        );

        // Iterate through all entries
        for (NameChangeEntry nameChangeEntry : nameChangeEntries.values()) {
            if (nameChangeEntry.databaseState == DatabaseState.UPDATE_DATABASE) {
                // Set variables
                insertStatement.setString(1, nameChangeEntry.getOldPlayerName());
                insertStatement.setLong(2, nameChangeEntry.getChangeDate());
                insertStatement.setInt(3, nameChangeEntry.getPlayerId());

                // Add batch
                updateStatement.addBatch();
                numberOfEntriesUpdated++;
            } else if (nameChangeEntry.databaseState == DatabaseState.INSERT_TO_DATABASE) {
                // Set variables
                insertStatement.setInt(1, nameChangeEntry.getPlayerId());
                insertStatement.setString(2, nameChangeEntry.getOldPlayerName());
                insertStatement.setLong(3, nameChangeEntry.getChangeDate());

                // Add batch
                insertStatement.addBatch();
                numberOfEntriesInserted++;
            } else {
                // Lets not change their database state
                continue;
            }

            // Update their database state
            nameChangeEntry.databaseState = DatabaseState.ON_DATABASE;
        }

        // Delete those who wasn't updated
        PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM `minecraft`.`player_name_changes` WHERE `changeDate` > ?;");
        deleteStatement.setLong(1, System.currentTimeMillis() + REQUIRED_TIME_TO_ALLOW_NAME);
        deleteStatement.execute();
        deleteStatement.close();

        // Delete from cache too
        Iterator<NameChangeEntry> iterator = nameChangeEntries.values().iterator();
        while (iterator.hasNext()) {
            NameChangeEntry next = iterator.next();

            if (next.isNameAvailable())
                iterator.remove();
        }

        // Execute and announce if needed
        if (numberOfEntriesUpdated > 0) updateStatement.executeBatch();
        if (numberOfEntriesInserted > 0) insertStatement.executeBatch();
        if (numberOfEntriesUpdated > 0 || numberOfEntriesInserted > 0)
            LobsterCraft.logger.info(Util.appendStrings("Took us ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                    "ms to clean old, insert ", numberOfEntriesInserted, " and update ", numberOfEntriesUpdated, " name changes."));

        // Close statement
        updateStatement.close();
        insertStatement.close();
    }

    /**
     * Retrieve profiles from database. This should run asynchronously as it may search on MySQL.
     *
     * @param onlinePlayer online player instance to build Profiles
     * @return a set of profiles, null if any error occurred
     */
    public FutureTask<Set<Profile>> retrieveProfiles(final OnlinePlayer onlinePlayer) {
        return new FutureTask<>(() -> {
            synchronized (playerProfiles) {
                ProfileStorage profileStorage = playerProfiles.remove(onlinePlayer.getOfflinePlayer().getPlayerId());
                // Check if profile was queued to be removed
                if (profileStorage != null) return profileStorage.profiles;
            }

            // Prepare for database search
            HashSet<Profile> profiles = new HashSet<>();
            checkConnection();

            try {
                // Get all profiles types
                for (Profile.ProfileType profileType : Profile.ProfileType.values())
                    profiles.add(profileType.getProfileClass().getConstructor(OnlinePlayer.class).newInstance(onlinePlayer).loadProfileFromDatabase(connection));
                return profiles;
            } catch (Exception exception) {
                exception.printStackTrace();
                return null;
            }
        });
    }

    private void checkConnection() throws SQLException {
        boolean connectionInvalid = false;
        // Create connection if it is dead or something
        if (connection == null || (connectionInvalid = !connection.isValid(1))) {
            if (connectionInvalid)
                connection.close();
            connection = LobsterCraft.dataSource.getConnection();
        }
    }

    /*
     * Event handling
     */

    /**
     * Lowest priority event check: will run first
     * Does NOT ignore cancelled events
     * This method will check, in this order:
     * - server is closing => deny login
     * - server is initializing => deny login
     * - there's a lot of players joining => deny login
     * - server is full => deny login if player doesn't have the permission
     * - player's name has invalid length or characters => deny login
     * - player's name is on blacklist => deny login
     * - player's name is already logged in => deny login
     * - player's name is on recent changed player names list => deny login
     * - player's (temporary) banned => deny login
     * <p>
     * <b>Note:</b> player's name is checked while retrieving OfflinePlayer from PlayerHandlerService (will occur on ban check) but we will check it before anyway
     *
     * @param event Spigot/Bukkit given event
     * @see com.jabyftw.lobstercraft.player.PlayerHandlerService#getOfflinePlayer(String)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName().toLowerCase();

        // Check if server is closing
        if (LobsterCraft.serverClosing) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§4Servidor fechando...\n§6Tente novamente em alguns minutos.");
            return;
        }

        // Check if login is early
        if (LobsterCraft.tickCounter.getTick() < 10L) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§4Servidor iniciando...\n§6Tente novamente em alguns segundos.");
            return;
        }

        // Check if there's a lot of players joining
        synchronized (playerJoinedLock) {
            if (lastPlayerJoinedTick > 0 && // a player must already have joined the server
                    (LobsterCraft.tickCounter.getTick() - lastPlayerJoinedTick) < TICKS_NEEDED_BETWEEN_JOINS) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "§4Muitas pessoas entrando simultaneamente...\n§6Tente novamente em alguns segundos.");
                return;
            }
        }

        // Check if server is full
        {
            Server server = Bukkit.getServer();
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offlinePlayer = server.getOfflinePlayer(playerName); // There will always be a player for this method
            // If is full and ((player isn't op) or (player don't have permission))
            if (server.getOnlinePlayers().size() >= server.getMaxPlayers() &&
                    // Note: 'null' worlds are supported for permissions system that allows global permissions
                    (!offlinePlayer.isOp() || !LobsterCraft.permission.playerHas(null, offlinePlayer, Permissions.JOIN_FULL_SERVER.toString()))) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "§4Servidor lotado!\n§cAguarde alguns segundos e tente novamente.");
            }
        }

        // Check if player's name is valid
        if (!Util.checkStringCharactersAndLength(playerName, 3, 16)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§4Nome inválido!\n§cEle contém caracteres inválidos ou\n§cé muito longo/curto");
            return;
        }

        // After name check done above, OfflinePlayer will always exist

        // Check if player's name is on blacklist
        for (String blacklistedName : blacklistedNames) {
            if (playerName.equalsIgnoreCase(blacklistedName) || playerName.contains(blacklistedName)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§4Nome inválido!\n§cEntre no servidor com um nome diferente.");
                return;
            }
        }

        // Check if player is already online
        //noinspection deprecation
        OnlinePlayer onlinePlayer = getOnlinePlayer(playerName, null);
        if (onlinePlayer != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§4Jogador já está online.");
            return;
        }

        // Check if player's name is a "recent changed name"
        synchronized (nameChangeEntries) {
            for (NameChangeEntry entry : nameChangeEntries.values())
                if (!entry.isNameAvailable() && entry.getOldPlayerName().equalsIgnoreCase(playerName)) {
                    event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            Util.appendStrings("§4Nome indisponível!\n§6O nome será liberado para todos os jogadores em\n§6",
                                    Util.formatDate(entry.getChangeDate() + REQUIRED_TIME_TO_ALLOW_NAME))
                    );
                    return;
                }
        }

        // Check if player is (temporary) banned
        {
            OfflinePlayer offlinePlayer = getOfflinePlayer(playerName);
            // Just check if player is registered
            if (offlinePlayer.isRegistered())
                synchronized (playerBanEntries) {
                    Set<BannedPlayerEntry> entries = getPlayerBanEntries(offlinePlayer.getPlayerId());

                    // Check if player has a record
                    if (entries != null)
                        // Iterate checking for active record
                        for (BannedPlayerEntry bannedPlayerEntry : entries)
                            if (bannedPlayerEntry.isBanned()) {
                                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, bannedPlayerEntry.getKickMessage());
                                return;
                            }
                }
        }

        // Call event for services to prepare for player join
        AsyncPlayerPreJoinEvent asyncPlayerPreJoinEvent = new AsyncPlayerPreJoinEvent();
        LobsterCraft.plugin.getServer().getPluginManager().callEvent(asyncPlayerPreJoinEvent);

        // Check if event was cancelled by any service
        if (asyncPlayerPreJoinEvent.isCancelled())
            event.disallow(asyncPlayerPreJoinEvent.getResult(), asyncPlayerPreJoinEvent.getKickMessage());
    }

    /**
     * Lowest priority event check: will run first
     * Does NOT ignore cancelled events
     * <p>
     * This event will call PlayerBecameOnlineEvent after assigning a Player to an OfflinePlayer, creating a OnlinePlayer (its OnlineState will be defined by
     * OfflinePlayer.isRegistered()
     * <p>
     * The OnlinePlayer will be created on this event, all services might initialize its variables for the player through an PlayerBecameOnlineEvent
     * <p>
     * Note: even if the event (PlayerJoinEvent) is cancelled, the OnlinePlayer will be created
     *
     * @param event Spigot/Bukkit given event
     * @see PlayerBecameOnlineEvent
     * @see OnlinePlayer#getOnlineState()
     * @see com.jabyftw.lobstercraft.player.OfflinePlayer#isRegistered()
     */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerJoinLowest(PlayerJoinEvent event) {
        // Remove join message, because it'll announced when logged in
        event.setJoinMessage("");

        // This will create OnlinePlayer instance
        Player bukkitPlayer = event.getPlayer();
        OfflinePlayer offlinePlayer;
        OnlinePlayer onlinePlayer;

        // Insert everything (synchronized because every map should be inserted on the same time)
        synchronized (playerMapsLock) {
            offlinePlayer = getOfflinePlayer(bukkitPlayer.getName());
            onlinePlayer = new OnlinePlayer(offlinePlayer, bukkitPlayer);

            onlinePlayers_offlinePlayer.put(offlinePlayer, onlinePlayer);
            onlinePlayers_player.put(bukkitPlayer, onlinePlayer);

            // Insert unregistered offline player
            if (!offlinePlayer.isRegistered())
                unregisteredOfflinePlayers_name.put(offlinePlayer.getPlayerName(), offlinePlayer);
        }

        // Call event that will initialize OnlinePlayer's attributes on each service
        LobsterCraft.plugin.getServer().getPluginManager().callEvent(new PlayerBecameOnlineEvent(onlinePlayer));

        // Set last joined tick if player has successfully joined
        synchronized (playerJoinedLock) {
            lastPlayerJoinedTick = LobsterCraft.tickCounter.getTick();
        }
    }

    /**
     * Highest check
     * This event will ignore cancelled
     * <p>
     * This event will remove leave message from invisible kicked players
     *
     * @param event Bukkit given event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerKickHighest(PlayerKickEvent event) {
        // Set quit message, none if player is invisible
        event.setLeaveMessage(LobsterCraft.vanishManager.isVanished(event.getPlayer()) ? "" : "§4- §c" + event.getPlayer().getName());
    }

    /**
     * Highest check
     * This event isn't cancellable
     * <p>
     * This event will remove quit message from invisible players
     *
     * @param event Bukkit given event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerQuitHighest(PlayerQuitEvent event) {
        // Set quit message, none if player is invisible
        event.setQuitMessage(LobsterCraft.vanishManager.isVanished(event.getPlayer()) ? "" : "§4- §c" + event.getPlayer().getName());
    }

    /**
     * Monitor check
     * This event isn't cancellable
     * <p>
     * This event will call PlayerLoggedOutEvent and later destroy OnlinePlayer
     *
     * @param event Bukkit given event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuitMonitor(PlayerQuitEvent event) {
        Player bukkitPlayer = event.getPlayer();
        OnlinePlayer onlinePlayer = getOnlinePlayer(bukkitPlayer, null);

        // Call event for all services to destroy OnlinePlayer
        LobsterCraft.plugin.getServer().getPluginManager().callEvent(new PlayerLoggedOutEvent(onlinePlayer));

        // Log off player before removing its profiles
        onlinePlayer.logOff();

        // Remove and store player's profiles
        if (onlinePlayer.getOfflinePlayer().isRegistered()) {
            HashSet<Profile> profileSet = new HashSet<>();

            synchronized (onlinePlayer.profiles) {
                Iterator<Profile> iterator = onlinePlayer.profiles.values().iterator();
                // Iterate through all profiles
                while (iterator.hasNext()) {
                    Profile profile = iterator.next();

                    // Delete OnlinePlayer from profile
                    profile.applyProfile(null);
                    profileSet.add(profile);
                    iterator.remove();
                }
            }
            // Store profiles
            synchronized (playerProfiles) {
                playerProfiles.put(onlinePlayer.getOfflinePlayer().getPlayerId(), new ProfileStorage(profileSet));
            }
        }

        boolean removeOfflineKey, removeBukkitKey;
        // Remove everything
        synchronized (playerMapsLock) {
            removeOfflineKey = onlinePlayers_offlinePlayer.remove(getOfflinePlayer(bukkitPlayer.getName()), onlinePlayer);
            removeBukkitKey = onlinePlayers_player.remove(bukkitPlayer, onlinePlayer);
        }

        // Check if everything was successfully removed
        if (!removeBukkitKey || !removeOfflineKey) {
            StringBuilder stringBuilder = new StringBuilder("Player ").append(bukkitPlayer.getName()).append(" wasn't removed properly from ");
            if (!removeBukkitKey) {
                stringBuilder.append("Bukkit player's map");
                if (!removeOfflineKey) stringBuilder.append(" and from ");
            }
            if (!removeOfflineKey) stringBuilder.append("OfflinePlayer's map");
            LobsterCraft.logger.warning(stringBuilder.toString());
        }
    }

//    /**
//     * This will listen for PlayerJoinsCityEvent. We must check anything if needed
//     *
//     * @param event our event
//     * @see com.jabyftw.lobstercraft.world.CityStructure#joinCity(OfflinePlayer) where this event is called and some other stuff is checked
//     */
//    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
//    private void onPlayerJoinCityHigh(PlayerJoinsCityEvent event) {
//        // TODO maybe we are forgetting some checks?
//    }

    /**
     * This will listen for PlayerJoinsCityEvent. We must set cityId and occupation for the player.
     *
     * @param event our event
     * @see com.jabyftw.lobstercraft.world.CityStructure#joinCity(OfflinePlayer) where this event is called and some other stuff is checked
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerJoinCityMonitor(PlayerJoinsCityEvent event) {
        // Set variables
        event.getOfflinePlayer().cityId = event.getCityStructure().getCityId();
        event.getOfflinePlayer().cityOccupation = CityOccupation.CITIZEN;
        event.getOfflinePlayer().databaseState = DatabaseState.UPDATE_DATABASE;

        synchronized (playerMapsLock) {
            // Insert player to map
            if (!registeredOfflinePlayers_cityId.containsKey(event.getCityStructure().getCityId()))
                registeredOfflinePlayers_cityId.put(event.getCityStructure().getCityId(), new HashSet<>());
            registeredOfflinePlayers_cityId.get(event.getCityStructure().getCityId()).add(event.getOfflinePlayer());
        }
    }

//    /**
//     * This will listen for PlayerChangesCityOccupationEvent. We must check anything if needed
//     *
//     * @param event our event
//     * @see com.jabyftw.lobstercraft.world.CityStructure#joinCity(OfflinePlayer) where this event is called and some other stuff is checked
//     */
//    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
//    private void onPlayerChangeCityOccupationHigh(PlayerChangesCityOccupationEvent event) {
//        // TODO maybe we are forgetting some checks?
//    }

    /**
     * This will listen for PlayerChangesCityOccupationEvent. We must set city occupation for the player.
     *
     * @param event our event
     * @see com.jabyftw.lobstercraft.world.CityStructure#joinCity(OfflinePlayer) where this event is called and some other stuff is checked.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerChangeCityOccupationMonitor(PlayerChangesCityOccupationEvent event) {
        // Set variables
        event.getOfflinePlayer().cityOccupation = event.getCityOccupation();
        event.getOfflinePlayer().databaseState = DatabaseState.UPDATE_DATABASE;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerChangesBuildingMode(PlayerChangesBuildingModeEvent event) {
        event.getOnlinePlayer().buildingMode = event.getBuildingMode();
        if (event.shouldWarnPlayer())
            event.getOnlinePlayer().getPlayer().sendMessage(Util.appendStrings("§6Você está construindo no tipo de proteção de §c",
                    event.getBuildingMode().getDisplayName()));
    }

    /*
     * Some classes
     */

    private class ProfileUnloader implements Runnable {

        @Override
        public void run() {
            synchronized (playerProfiles) {
                Iterator<ProfileStorage> iterator = playerProfiles.values().iterator();

                try {
                    checkConnection();
                } catch (SQLException exception) {
                    exception.printStackTrace();
                    LobsterCraft.logger.severe("Couldn't restore connection for profile saving!");
                }

                // Iterate through all storage
                while (iterator.hasNext()) {
                    ProfileStorage storage = iterator.next();

                    // Check if we should remove it
                    if (storage.shouldBeRemoved() || LobsterCraft.serverClosing) {
                        for (Profile profile : storage.profiles)
                            // This will filter profile saving
                            if (profile.getDatabaseState().shouldSave() && !profile.saveToDatabase(connection))
                                LobsterCraft.logger.warning(Util.appendStrings("Couldn't save profile ", profile.profileType.name(), " for playerId=", profile.playerId));

                        // Remove from queue, it was saved
                        iterator.remove();
                    }
                }
            }
        }
    }

    private class ProfileStorage {

        protected final HashSet<Profile> profiles = new HashSet<>();
        protected final long timeWhenStored = System.currentTimeMillis();

        protected ProfileStorage(@NotNull final Collection<Profile> profileCollection) {
            this.profiles.addAll(profileCollection);
        }

        protected boolean shouldBeRemoved() {
            return System.currentTimeMillis() - timeWhenStored > TIME_PROFILE_IS_STORED_MILLISECONDS;
        }
    }

    private class NameChangeEntry {

        // Database information
        private final int playerId;
        private String oldPlayerName;
        private long changeDate;

        // Class variable
        DatabaseState databaseState = DatabaseState.NOT_ON_DATABASE;

        /**
         * This should be created BEFORE the name is changed
         *
         * @param offlinePlayer player that is changing name
         */
        private NameChangeEntry(@NotNull final OfflinePlayer offlinePlayer) {
            this.playerId = offlinePlayer.getPlayerId();
            this.oldPlayerName = offlinePlayer.getPlayerName();
            this.changeDate = System.currentTimeMillis();
            this.databaseState = DatabaseState.INSERT_TO_DATABASE;
        }

        private NameChangeEntry(int playerId, @NotNull final String oldPlayerName, long changeDate) {
            this.playerId = playerId;
            this.oldPlayerName = oldPlayerName;
            this.changeDate = changeDate;
            this.databaseState = DatabaseState.ON_DATABASE;
        }

        /**
         * Change player name of the entry, this will block the old player name for logging in players
         * This method <b>WON'T</b> check for any condition.
         * This method <b>WON'T</b> save NameChangeEntry on database either.
         *
         * @param oldPlayerName before-change player name
         * @return this instance to be saved
         */
        private NameChangeEntry changeNameAgain(@NotNull final String oldPlayerName) {
            this.oldPlayerName = oldPlayerName;
            this.changeDate = System.currentTimeMillis();
            this.databaseState = DatabaseState.UPDATE_DATABASE;
            return this;
        }

        boolean isNameAvailable() {
            return (System.currentTimeMillis() - changeDate) > REQUIRED_TIME_TO_ALLOW_NAME;
        }

        boolean canPlayerChangeNameAgain() {
            return (System.currentTimeMillis() - changeDate) > PLAYER_CAN_CHANGE_NAME_AGAIN;
        }

        int getPlayerId() {
            return playerId;
        }

        long getChangeDate() {
            return changeDate;
        }

        String getOldPlayerName() {
            return oldPlayerName;
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && ((obj instanceof OfflinePlayer && ((OfflinePlayer) obj).getPlayerId() == playerId)
                    || (obj instanceof NameChangeEntry && ((NameChangeEntry) obj).playerId == playerId));
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(3, 9)
                    .append(playerId)
                    .toHashCode();
        }
    }

    public class BannedPlayerEntry {

        private final long recordId;
        private final Integer moderatorId;
        private final BanType banType;
        private final long recordDate;
        private final String reason;
        private final Long unbanDate;

        private BannedPlayerEntry(long recordId, @Nullable Integer moderatorId, @NotNull BanType banType, long recordDate, @NotNull String reason,
                                  @Nullable Long unbanDate) {
            this.recordId = recordId;
            this.moderatorId = moderatorId;
            this.banType = banType;
            this.recordDate = recordDate;
            this.reason = reason;
            this.unbanDate = unbanDate;
            if (unbanDate == null && banType == BanType.PLAYER_TEMPORARILY_BANNED)
                throw new IllegalArgumentException("BanType can't be PLAYER_TEMPORARILY_BANNED if unbanDate is not set!");
        }

        /**
         * @return true if moderator is a player, false if moderator is the console
         */
        boolean isModeratorAPlayer() {
            return moderatorId != null;
        }

        boolean isBanned() {
            return (unbanDate == null && banType == BanType.PLAYER_PERMANENTLY_BANNED) ||
                    (banType == BanType.PLAYER_TEMPORARILY_BANNED && unbanDate != null && unbanDate > System.currentTimeMillis());
        }

        /**
         * @return the string that should appear to the player
         */
        String getKickMessage() {
            return banType.getBaseKickMessage()
                    .replaceAll("%moderator%", isModeratorAPlayer() ? getOfflinePlayer(moderatorId).getPlayerName() : "Console")
                    .replaceAll("%reason%", reason)
                    // Check Util#dateFormat
                    .replaceAll("%unbanDate%", unbanDate != null ? Util.formatDate(unbanDate) : "30/02/2194 25:66")
                    .replaceAll("%recordDate%", Util.formatDate(recordDate));
        }

        /*
         * Getters
         */

        public Integer getModeratorId() {
            return moderatorId;
        }

        public BanType getBanType() {
            return banType;
        }

        public long getRecordDate() {
            return recordDate;
        }

        public String getReason() {
            return reason;
        }

        public Long getUnbanDate() {
            return unbanDate;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof BannedPlayerEntry && ((BannedPlayerEntry) obj).recordId == recordId;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(5, 63).append(recordId).toHashCode();
        }
    }

    public enum BanType {

        PLAYER_KICKED((byte) 1, "expulsão",
                "§6Você foi expulso por §c%moderator%\n" +
                        "§6Motivo: §c\"%reason%\""
        ),
        PLAYER_PERMANENTLY_BANNED((byte) 2, "perm.",
                "§6Você foi banido por §c%moderator%\n" +
                        "§6Motivo: §c\"%reason%\"\n" +
                        "§6Banido permanentemente em §c%recordDate%"
        ),
        PLAYER_TEMPORARILY_BANNED((byte) 3, "temp.",
                "§6Você foi exilado por §c%moderator%\n" +
                        "§6Motivo: §c\"%reason%\"\n" +
                        "§6Exilado §c%recordDate% §6até §c%unbanDate%"
        );

        private final byte typeId;
        private final String kickMessage, typeName;

        BanType(byte typeId, @NotNull final String typeName, @NotNull final String kickMessage) {
            this.typeId = typeId;
            this.kickMessage = kickMessage;
            this.typeName = typeName;
        }

        public byte getTypeId() {
            return typeId;
        }

        /**
         * This is used for history command
         *
         * @return a portuguese name for the entry
         */
        public String getTypeName() {
            return typeName;
        }

        /**
         * Get the kick message to be customized:<br>
         * <b>%moderator%</b>: should be moderator's name, <i>"Console"</i> if null<br>
         * <b>%reason%</b>: should be the reason of kick
         * <b>%unbanDate%</b>: should be the date the player will be allowed to rejoin
         * <b>%recordDate%</b>: should be the date the player was kicked
         *
         * @return a string that should be shown to player screen
         * @see Util#formatDate(long) to use on <b>%unbanDate%</b>
         * @see BannedPlayerEntry#getKickMessage()
         */
        public String getBaseKickMessage() {
            return kickMessage;
        }

        public static BanType getBanType(byte typeId) {
            for (BanType banType : values())
                if (banType.getTypeId() == typeId) return banType;
            return null;
        }
    }

    public enum ChangeNameResponse {
        SUCCESSFULLY_CHANGED,
        NAME_UNAVAILABLE,
        NAME_PROTECTED,
        NAME_INVALID,
        CANT_CHANGE_NAME_YET,
        INCORRECT_PASSWORD,
        ERROR_OCCURRED
    }

    public enum BanResponse {
        SUCCESSFULLY_EXECUTED,
        PLAYER_NOT_REGISTERED,
        BAN_DURATION_NOT_SET,
        INVALID_REASON_LENGTH,
        ERROR_OCCURRED
    }
}
