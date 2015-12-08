package com.jabyftw.pacocacraft.login;

import com.jabyftw.Util;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.location.TeleportBuilder;
import com.jabyftw.pacocacraft.location.TeleportProfile;
import com.jabyftw.pacocacraft.location.TeleportService;
import com.jabyftw.pacocacraft.player.*;
import com.jabyftw.pacocacraft.player.invisibility.InvisibilityService;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.Permissions;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.sql.*;
import java.text.SimpleDateFormat;

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
public class UserProfile extends BasePlayerProfile {

    // Login/profile information
    private long playerId = -1;
    private String playerName;
    private String password = null;
    private long lastTimeOnline = -1;
    private long playTime = 0;
    private byte[] lastIp;

    // Variables (not database related)
    private final Object passwordLock = new Object(); // synchronized because I don't want the password to change middle execution of some methods
    private long loginTime;
    private volatile boolean loggedIn = false;
    private PlayerMoment preLoginMoment = null; // Lock not needed, even though it is used on login command (async), it requires Bukkit's API and so it is synchronized for this

    /**
     * Create user profile given player's name for first login identification
     * NOTE: player name can be changed ingame
     *
     * @param playerName player's name
     */
    public UserProfile(@NotNull String playerName) {
        super(ProfileType.USER_PROFILE);
        this.playerName = playerName;
    }

    /**
     * Create user profile using database information
     *
     * @param playerName     player's name
     * @param playerId       player's database identification number
     * @param password       player's encrypted password
     * @param lastTimeOnline player's last time online (using System.currentTimeInMillis)
     *
     * @see System#currentTimeMillis()
     */
    public UserProfile(@NotNull String playerName, long playerId, @NotNull String password, long lastTimeOnline, long playTime, byte[] lasIp) {
        super(ProfileType.USER_PROFILE);
        this.playerId = playerId;
        this.playerName = playerName;
        synchronized(passwordLock) {
            this.password = password;
        }
        this.lastTimeOnline = lastTimeOnline;
        this.playTime = playTime;
        this.lastIp = lasIp;
    }

    @Override
    public void onPlayerHandleApply(@NotNull PlayerHandler playerHandler) {
        Player player = playerHandler.getPlayer();

        // Update display name and send greetings
        player.setDisplayName(ChatColor.translateAlternateColorCodes('&', PacocaCraft.chat.getPlayerPrefix(player) + player.getName() + PacocaCraft.chat.getPlayerSuffix(player)));
        /*synchronized(passwordLock) { // TODO my idea of a static chat
            player.sendMessage(
                    (password == null ? "§6Bem vindo, " : "§6Bem vindo novamente, ") +
                            player.getDisplayName() + "§6!" +
                            (password == null ? "" : "\n§6Você entrou pela ultima vez em §c" + Util.parseTimeInMillis(lastTimeOnline, "dd/MM/yyyy HH:mm"))
            );
        }*/

        // Need Bukkit API, but we're on PlayerJoinEvent
        // Store last Ip
        lastIp = player.getAddress().getAddress().getAddress();
        modified = true;

        // Store before login information
        preLoginMoment = new PlayerMoment(playerHandler);

        // Set login 'ideal status' (no potion effects, no pending damage etc)
        PlayerMoment.setIdealStatus(player);

        // Hide player from everyone and vice-versa (note: we're on PlayerJoinEvent, as stated before)
        InvisibilityService.hideEveryoneFromPlayer(player);
        InvisibilityService.hidePlayerFromEveryone(player);

        // Teleport to spawn (without saving player's last location but saving server's before-login location)
        TeleportBuilder.getBuilder(playerHandler).setLocation(player.getWorld().getSpawnLocation()).setInstantaneousTeleport(true).execute();
    }

    @Override
    public void onPlayerHandleDestruction() {
        // Update last time online if logged in
        if(isLoggedIn()) {
            // Add playTime from login time
            playTime += Math.abs(System.currentTimeMillis() - loginTime); // abs, just in case
            // Update last time online
            lastTimeOnline = System.currentTimeMillis();
            modified = true;
        }

        // Restore login moment if player isn't logged in still
        if(preLoginMoment != null)
            preLoginMoment.restorePlayerMoment(); // needs Bukkit API, but we're on PlayerQuitEvent

        // Set logged in to false in the end (because it may be checked before)
        loggedIn = false;
    }

    @Override
    public boolean shouldBeSaved() {
        synchronized(passwordLock) {
            return super.shouldBeSaved() && password != null;
        }
    }

    /**
     * Set player as logged in given password typed by player (ran on a random BukkitScheduler's thread)
     * <b>NOTE:</b> player will already "be welcomed"/warned about its state
     * <b>NOTE:</b> this should be ran asynchronously as it'll retrieve every profile on database
     * <b>NOTE:</b> as this requires async, DO NOT USE Bukkit API here without scheduling the task
     * <p>
     * Conclusion: password (not player, sendMessage is thread-safe)
     *
     * @param encryptedPassword encrypted user given password (caller must encrypt the password)
     *
     * @return true if successful log in
     */
    public boolean attemptLogin(@NotNull String encryptedPassword) {
        Player player = getPlayerHandler().getPlayer();

        synchronized(passwordLock) {
            // Check if password is the same or if the password even exists
            if(this.password != null && !this.password.equals(encryptedPassword)) {
                player.sendMessage("§4Senha incorreta! §cTente novamente...");
                return false;
            } else if(this.password == null) {
                player.sendMessage("§4Você não está registrado! §cUse o comando §6/register");
                return false;
            }
        }

        // Retrieve stored profiles
        for(PlayerProfile playerProfile : PacocaCraft.playerService.getProfiles(playerId).values())
            getPlayerHandler().applyProfile(playerProfile);

        // Check if all profiles are ready
        for(ProfileType profileType : ProfileType.values()) {
            if(getPlayerHandler().getProfile(profileType) == null)
                try {
                    // Apply missing profile
                    getPlayerHandler().applyProfile(profileType.retrieveProfile(playerId));
                } catch(SQLException e) {
                    e.printStackTrace();
                    player.sendMessage("§4Ocorreu um erro! §cBanco de dados não encontrou " + profileType.name());
                    return false;
                }
        }

        // Everything went fine, log player in and restore everything
        BukkitScheduler.runTask(PacocaCraft.pacocaCraft, () -> {
            // Restore player pre-login
            preLoginMoment.restorePlayerMoment(); // Needs Bukkit API, run sync
            preLoginMoment = null;

            // Show player to everyone and vice-versa (uses Bukkit API)
            InvisibilityService.showEveryoneToPlayer(player);
            InvisibilityService.showPlayerToEveryone(player);

            // Successful login, set variable to true
            this.loggedIn = true;

            // Store login time so we can calculate playTime
            loginTime = System.currentTimeMillis();

            // Broadcast messages
            player.sendMessage("§6Login bem sucedido!");
            if(!PacocaCraft.permission.playerHas(player, Permissions.JOIN_VANISHED))
                Bukkit.getServer().broadcastMessage("§b+ §3" + player.getName());
        });
        return true;
    }

    /**
     * Set the player's password
     * NOTE: the player will be logged on and will "be welcomed"/warned about its state
     *
     * @param encryptedPassword password of the player (encrypted by caller; caller must be sure to check both passwords on command)
     *
     * @return true if player was successfully registered
     *
     * @see UserProfile#attemptLogin(String): this should be ran asynchronously as it'll retrieve every profile on database
     */
    public boolean registerPlayer(@NotNull String encryptedPassword) {
        synchronized(passwordLock) {
            // Check if player is already registered
            if(this.password != null) {
                getPlayerHandler().getPlayer().sendMessage("§cVocê já foi registrado!");
                return false;
            }

            // Register player password (it'll be saved)
            this.password = encryptedPassword;
            this.modified = true;
        }

        try {
            UserProfile.saveUserProfile(this);
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }

        attemptLogin(encryptedPassword);
        return true;
    }

    public long getPlayerId() {
        return playerId;
    }

    /**
     * Check if player is logged on (used to deny movement, most commands, events etc)
     *
     * @return true if player is logged in
     *
     * @see com.jabyftw.pacocacraft.login.LoginListener#onChat(AsyncPlayerChatEvent) should be a volatile now
     */
    public synchronized boolean isLoggedIn() {
        return loggedIn;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getLastTimeOnlineMillis() {
        return lastTimeOnline;
    }

    public long getPlayTime() {
        return playTime;
    }

    public String getFormattedPlayTime() {
        return Util.parseTimeInMillis(playTime);
    }

    public String getEncryptedPassword() {
        synchronized(passwordLock) {
            return password;
        }
    }

    /**
     * Save user profile to MySQL
     * <b>NOTE:</b> should be on async since it'll change the database
     *
     * @param userProfile the desired user profile
     *
     * @return player id
     *
     * @throws SQLException
     */
    public static long saveUserProfile(@NotNull UserProfile userProfile) throws SQLException {
        long playerId;

        // Get connection from pool and execute query
        Connection connection = PacocaCraft.dataSource.getConnection();
        {
            boolean playerOnDatabase = userProfile.getPlayerId() >= 0;

            // Prepare statement arguments
            PreparedStatement preparedStatement = connection.prepareStatement(
                    playerOnDatabase ?
                            "UPDATE `minecraft`.`user_profiles` SET `playerName` = ?, `password` = ?,`lastTimeOnline` = ?,`playTime` = ?,`lastIp` = ? WHERE `playerId` = ?;" : // Player exists
                            "INSERT INTO `minecraft`.`user_profiles`(`playerName`, `password`, `lastTimeOnline`, `playTime`, `lastIp`) VALUES (?, ?, ?, ?, ?);", // Insert player
                    Statement.RETURN_GENERATED_KEYS
            );

            // Update statement where needed
            int index = 1;
            preparedStatement.setString(index++, userProfile.getPlayerName().toLowerCase());
            preparedStatement.setString(index++, userProfile.getEncryptedPassword());
            preparedStatement.setLong(index++, userProfile.getLastTimeOnlineMillis());
            preparedStatement.setLong(index++, userProfile.getPlayTime());
            preparedStatement.setBytes(index++, userProfile.lastIp);
            if(playerOnDatabase) preparedStatement.setLong(index, userProfile.getPlayerId());

            // Execute statement
            preparedStatement.execute();

            // Set player id variable
            if(playerOnDatabase) {
                playerId = userProfile.getPlayerId();
            } else {
                // Retrieve generated key (player Id)
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if(generatedKeys.next())
                    playerId = generatedKeys.getLong("playerId");
                else
                    throw new IllegalStateException("Generated keys weren't returned!");
                generatedKeys.close();
            }

            // Close statement
            preparedStatement.close();
        }
        // Close connection and return true (it worked or it'll throw exceptions)
        connection.close();

        return playerId;
    }

    /**
     * Return user profile fetched from MySQL
     * <b>NOTE:</b> should be on async since it'll lookup in the database
     *
     * @param playerName player's name
     *
     * @return null if profile wasn't found
     *
     * @throws SQLException
     */
    public static UserProfile fetchUserProfile(@NotNull String playerName) throws SQLException {
        playerName = playerName.toLowerCase();
        UserProfile userProfile;

        // Get connection from pool and execute query
        Connection connection = PacocaCraft.dataSource.getConnection();
        {
            // Prepare statement arguments
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM `minecraft`.`user_profiles` WHERE `user_profiles`.`playerName` = ?;" // Since player name is unique, it'll work just fine
            );
            preparedStatement.setString(1, playerName);

            // Execute statement
            ResultSet resultSet = preparedStatement.executeQuery();
            if(!resultSet.next())
                // If player doesn't exists, return null
                return null;

            // Retrieve information
            long playerId = resultSet.getLong("playerId");
            String password = resultSet.getString("password");
            long lastTimeOnline = resultSet.getLong("lastTimeOnline");
            long playTime = resultSet.getLong("playTime");
            byte[] lastIp = resultSet.getBytes("lastIp");

            // Apply information to profile on a "loaded profile" constructor
            userProfile = new UserProfile(playerName, playerId, password, lastTimeOnline, playTime, lastIp);

            // Close ResultSet and PreparedStatement
            resultSet.close();
            preparedStatement.close();
        }
        // Close connection and return profile
        connection.close();

        return userProfile;
    }
}
