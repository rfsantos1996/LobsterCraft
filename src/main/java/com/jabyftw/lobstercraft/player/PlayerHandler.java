package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.location.TeleportBuilder;
import com.jabyftw.lobstercraft.player.util.*;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.util.ProtectionType;
import com.jabyftw.lobstercraft.world.util.location_util.OreBlockLocation;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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
public class PlayerHandler {

    public final static long UNDEFINED_PLAYER = -1;

    // Player variables
    protected final ConcurrentHashMap<ProfileType, Profile> playerProfiles = new ConcurrentHashMap<>();
    private final LinkedList<OreBlockLocation> oreLocationHistory = new LinkedList<>();
    private final ConditionController conditionController = new ConditionController(this);

    // Database information // TODO make it volatile since it'll be worked on at least 2 different threads but only atomic read/write (I believe)
    private long playerId = UNDEFINED_PLAYER;
    private volatile String playerName;
    private volatile String password = null;
    private long lastTimeOnline = 0;
    private long playTime = 0;
    private String lastIp = null;

    private Player player;
    private PlayerState preLoginState = null;
    private BuildMode buildMode = BuildMode.DEFAULT;
    private TeleportBuilder.Teleport pendingTeleport;
    private volatile DatabaseState databaseState = DatabaseState.NOT_ON_DATABASE;
    private boolean loggedIn = false;
    private boolean godMode = false;
    private long loginTime; // Used to calculate playerTime

    /**
     * Constructor for generation of a new player
     *
     * @param playerName logging in player's name
     */
    public PlayerHandler(@NotNull final String playerName) {
        this.playerName = playerName.toLowerCase();
    }

    public PlayerHandler(final long playerId, @NotNull final String playerName, @NotNull final String password, final long lastTimeOnline, final long playTime, @NotNull final String lastIp) {
        this.playerId = playerId;
        this.playerName = playerName.toLowerCase();
        this.password = password;
        this.lastTimeOnline = lastTimeOnline;
        this.playTime = playTime;
        this.lastIp = lastIp;
        this.databaseState = DatabaseState.ON_DATABASE;
    }

    /*
     * Configuration methods
     */

    public static long savePlayerHandle(@NotNull final Connection connection, @NotNull PlayerHandler playerHandler) throws SQLException {
        boolean insertPlayer = playerHandler.databaseState == DatabaseState.INSERT_TO_DATABASE;
        //LobsterCraft.logger.info("Database state is " + playerHandler.databaseState.name());

        // Prepare statement
        PreparedStatement preparedStatement;
        if (insertPlayer)
            preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`user_profiles` (`playerName`, `password`, `lastTimeOnline`, `playTime`, `lastIp`) VALUES (?, ?, ?, ?, ?);",
                    Statement.RETURN_GENERATED_KEYS
            );
        else
            preparedStatement = connection.prepareStatement(
                    "UPDATE `minecraft`.`user_profiles` SET `playerName` = ?, `password` = ?, `lastTimeOnline` = ?, `playTime` = ?, `lastIp` = ? WHERE `playerId` = ?;"
            );

        // Set up variables accordingly to the statement executed
        preparedStatement.setString(1, playerHandler.getPlayerName().toLowerCase()); // Lower case it just to make sure
        preparedStatement.setString(2, playerHandler.password);
        preparedStatement.setLong(3, playerHandler.getLastTimeOnline());
        preparedStatement.setLong(4, playerHandler.getPlayTime());
        preparedStatement.setString(5, playerHandler.getLastIp());
        if (!insertPlayer) preparedStatement.setLong(6, playerHandler.getPlayerId());

        // Execute statement
        preparedStatement.execute();
        playerHandler.databaseState = DatabaseState.ON_DATABASE;

        // Store playerId for return
        long playerId = playerHandler.playerId;

        if (insertPlayer) {
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next())
                playerId = generatedKeys.getLong("playerId");
            else throw new IllegalStateException("There wasn't returned any generated key!");
        }

        // Close statement
        preparedStatement.close();

        return playerId;
    }

    public void initialize(@NotNull Player player) {
        // Check if player name matches
        if (!player.getName().equalsIgnoreCase(playerName))
            throw new IllegalArgumentException("Player isn't owner of this PlayerHandler (" + playerName + ")");

        // Set player instance
        this.player = player;

        // Update display name
        player.setDisplayName(ChatColor.translateAlternateColorCodes('&', LobsterCraft.chat.getPlayerPrefix(player) + player.getName() + LobsterCraft.chat.getPlayerSuffix(player)));

        // If the state isn't null, restore it
        if (preLoginState != null) preLoginState.restorePlayerState();

        // Store player State
        preLoginState = new PlayerState(this);
        // Clear player
        preLoginState.clearPlayer();
        // Teleport player to spawn location
        TeleportBuilder.getBuilder(this)
                .setLocation(player.getWorld().getSpawnLocation())
                .setInstantaneousTeleport(true)
                .execute();

        // Set pre-login variables
        player.setCanPickupItems(false);
        player.setAllowFlight(true);
        player.setFlying(true);

        // Add player to player list
        LobsterCraft.playerHandlerService.playerHandlers.put(player, this);
    }

    /**
     * Don't need to be asynchronous (it will queue saving but won't save stuff)
     */
    public void destroy() {
        // Restore player moment if it exists
        if (preLoginState != null) {
            preLoginState.restorePlayerState();
            preLoginState = null;
        }

        if (isLoggedIn()) {
            // Save lastTimeOnline and timeOnline
            this.lastTimeOnline = System.currentTimeMillis();
            this.playTime += Math.abs(System.currentTimeMillis() - loginTime);

            // Set as not logged in
            this.loggedIn = false;

            // Make it save the profile (since it logged in)
            setAsModified();
        }

        // Queue saving all profiles since player is registered
        if (isRegistered()) {
            // Clear all profiles and store them
            playerProfiles.forEach(((profileType, profile) -> profile.destroyProfile()));
            LobsterCraft.profileService.storeProfiles(playerId, playerProfiles.values());
            playerProfiles.clear();

            // Store instance on PlayerHandlerServices in case player re-logs in (it depends whenever player was registered => it'll store password)
            LobsterCraft.playerHandlerService.storeProfile(this);
        }

        // Remove from player list, lastly
        LobsterCraft.playerHandlerService.playerHandlers.remove(player, this);
    }

    /**
     * This is supposed to run asynchronously
     *
     * @return the login response
     * @see PlayerHandler#forceLogin() it retrieves all profiles from database => player's password and databaseState must be volatile
     */
    public LoginResponse attemptRegister(@NotNull final String password) {
        // Assure player isn't registered
        if (isRegistered()) return LoginResponse.ALREADY_REGISTERED;

        // Encrypt password
        String encryptedPassword;
        try {
            encryptedPassword = Util.encryptString(password);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return LoginResponse.ERROR_OCCURRED;
        }

        // Set variables for register
        this.password = encryptedPassword;
        this.lastIp = player.getAddress().getAddress().getHostAddress(); // this was null (updated on forceLogin(), only)
        setAsModified();

        // Save profile, retrieve playerId
        try {
            playerId = savePlayerHandle(LobsterCraft.dataSource.getConnection(), this); // others will be set as 0, but that's okay
        } catch (SQLException e) {
            e.printStackTrace();
            return LoginResponse.ERROR_OCCURRED;
        }

        // Assure playerId is greater than 0
        if (playerId < 0) return LoginResponse.ERROR_OCCURRED;

        return forceLogin();
    }

    /**
     * This is supposed to run asynchronously
     *
     * @return the login response
     * @see PlayerHandler#forceLogin() it retrieves all profiles from database
     */
    public LoginResponse attemptLogin(@NotNull final String password) {
        // Assure player is registered
        if (!isRegistered()) return LoginResponse.NOT_REGISTERED;

        // Encrypt password
        String encryptedPassword;
        try {
            encryptedPassword = Util.encryptString(password);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return LoginResponse.ERROR_OCCURRED;
        }

        // Check if passwords match
        if (!this.password.equals(encryptedPassword))
            return LoginResponse.PASSWORD_DO_NOT_MATCH;

        return forceLogin();
    }

    /**
     * This is supposed to run asynchronously
     *
     * @return the login response
     */
    public LoginResponse forceLogin() {
        // Fetch all profiles
        FutureTask<Set<Profile>> retrieveProfiles = LobsterCraft.profileService.retrieveProfiles(playerId);
        retrieveProfiles.run();

        // Apply all profiles, if execution went right; return error otherwise
        try {
            for (Profile profile : retrieveProfiles.get()) // Will cause NullPointerException if there is a error on profile retrieving
                profile.applyProfile(this);
        } catch (InterruptedException | ExecutionException | NullPointerException e) {
            e.printStackTrace();
            return LoginResponse.ERROR_OCCURRED;
        }

        // synchronize with Bukkit
        BukkitScheduler.runTask(() -> {
            try {
                // Restore player pre-login moment
                if (preLoginState != null) {
                    preLoginState.restorePlayerState();
                    preLoginState = null;
                }

                // Update login time and player's last IP
                this.lastIp = player.getAddress().getAddress().getHostAddress();
                this.loginTime = System.currentTimeMillis();

                // Set as logged in
                this.loggedIn = true;

                // Check if player have the permission to join quietly/invisible
                boolean vanished = LobsterCraft.vanishManager.isVanished(player);
                if (LobsterCraft.permission.has(player, Permissions.JOIN_VANISHED)) {
                    // Make him invisible
                    if (!vanished) LobsterCraft.vanishManager.toggleVanishQuiet(player, false);
                } else {
                    // Make him visible
                    if (vanished) LobsterCraft.vanishManager.toggleVanishQuiet(player, false);

                    // Broadcast player login
                    Bukkit.broadcastMessage("§b+ §3" + player.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                player.kickPlayer("§4Ocorreu um erro durante login.");
            }
        });

        return LoginResponse.SUCCESSFULLY_LOGGED_IN;
    }

    /**
     * Change the player's name, player MUST be kicked on this operation.
     * Must run asynchronously as it'll change database and automatically save the player
     *
     * @param newPlayerName new player's name
     * @param password      player's current password for security
     * @return change response
     */
    public ChangeNameResponse changePlayerName(@NotNull final String newPlayerName, @NotNull final String password) {
        // Check if player is registered
        if (!isRegistered()) return ChangeNameResponse.NOT_REGISTERED;

        // Encrypt password
        String encryptedPassword;
        try {
            encryptedPassword = Util.encryptString(password);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return ChangeNameResponse.ERROR_OCCURRED;
        }

        // Check if encrypted password matched
        if (!this.password.equals(encryptedPassword))
            return ChangeNameResponse.WRONG_PASSWORD;

        // TODO check if player can change name (time since last change)
        // TODO check if player name is available
        // TODO banned names list
        // TODO change the player's name
        // TODO the kick will be on the command => playerName must be updated on database, profile must be stored on queue with the new name

        // TODO make and move Response down
        return ChangeNameResponse.SUCCESSFULLY_CHANGED;
    }

    /*
     * Runtime commands
     */

    public ChangePasswordResponse changePlayerPassword(@NotNull final String oldPassword, @NotNull final String newPassword) {
        String encryptedPassword;
        try {
            // Check if the old password corresponds to the new one
            if (!Util.encryptString(oldPassword).equals(this.password))
                return ChangePasswordResponse.WRONG_OLD_PASSWORD;

            // Encrypt new password
            encryptedPassword = Util.encryptString(newPassword);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return ChangePasswordResponse.ERROR_OCCURRED;
        }

        // Change password
        this.password = encryptedPassword;
        return ChangePasswordResponse.SUCCESSFULLY_CHANGED;
    }

    public <T extends Profile> T getProfile(Class<T> profileClass) {
        ProfileType profileType = ProfileType.getType(profileClass);
        if (profileType == null) throw new IllegalStateException("ProfileType is not registered!");
        return (T) profileClass.cast(playerProfiles.get(profileType));
    }

    /*
     * Simple getters and setters
     */

    public void sendMessage(@NotNull final String message) {
        // TODO type of message, fixed chat
        player.sendMessage(message);
    }

    public Player getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getLastIp() {
        return lastIp;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public BuildMode getBuildMode() {
        return buildMode;
    }

    public ProtectionType getProtectionType() {
        return buildMode.getType();
    }

    public void setBuildMode(@Nullable BuildMode buildMode) {
        this.buildMode = buildMode == null ? BuildMode.DEFAULT : buildMode;
    }

    public long getBuildModeId() {
        return buildMode.getType() == ProtectionType.ADMIN_PROTECTION ? ((AdministratorBuildMode) buildMode).getConstructionId() : playerId;
    }

    public boolean isRegistered() {
        return this.password != null && playerId >= 0;
    }

    public boolean isInvisible() {
        return LobsterCraft.vanishManager.isVanished(player);
    }

    public boolean isGodMode() {
        return godMode;
    }

    public boolean setGodMode(boolean godMode) {
        this.godMode = godMode;

        // Credits: VanishNoPacket
        // If is going to be on god mode, remove entities' agro
        if (godMode) {
            // Iterate through all entities
            for (Entity entity : player.getNearbyEntities(48 / 2, 48 / 2, 48 / 2)) {
                LivingEntity target;
                // If entity have a target and it is the same player, remove target
                if (entity instanceof Creature && (target = ((Creature) entity).getTarget()) instanceof Player && target.equals(player))
                    ((Creature) entity).setTarget(null);
            }
        }
        return godMode;
    }

    public boolean isDamageable() {
        return isLoggedIn() && !isInvisible() && !isGodMode();
    }

    public ConditionController getConditionController() {
        return conditionController;
    }

    public long getPlayerId() {
        return playerId;
    }

    public long getLastTimeOnline() {
        return lastTimeOnline;
    }

    public long getPlayTime() {
        return playTime;
    }

    public TeleportBuilder.Teleport getPendingTeleport() {
        return pendingTeleport;
    }

    public void setPendingTeleport(@Nullable TeleportBuilder.Teleport pendingTeleport) {
        this.pendingTeleport = pendingTeleport;
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    public LinkedList<OreBlockLocation> getOreLocationHistory() {
        return oreLocationHistory;
    }

    protected void setAsModified() {
        if (databaseState == DatabaseState.NOT_ON_DATABASE)
            databaseState = DatabaseState.INSERT_TO_DATABASE;
        if (databaseState == DatabaseState.ON_DATABASE)
            databaseState = DatabaseState.UPDATE_DATABASE;
        // If is DELETE_DATABASE | INSERT_DATABASE | UPDATE_DATABASE, continue DELETE_DATABASE | INSERT_DATABASE | UPDATE_DATABASE
    }

    /*
     * Equality stuff
     */

    @Override
    public boolean equals(Object obj) {
        // Checking the instance of something returns false if null (so it would be ambiguous)
        return obj instanceof PlayerHandler && player.equals(((PlayerHandler) obj).player);
    }

    /**
     * We will have 2 states:
     * * non-registered/not logged in players => playerName
     * * registered players => playerId
     *
     * @return the hash code that is capable of identify objects
     */
    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(7, 17);
        if (!isLoggedIn())
            hashCodeBuilder.append(playerName);
        else if (isRegistered())
            hashCodeBuilder.append(playerId);
        else
            throw new IllegalStateException("Player is logged in without playerId");
        return hashCodeBuilder.toHashCode();
    }

    /*
     * Some extra classes
     */

    public enum ChangeNameResponse {
        SUCCESSFULLY_CHANGED,
        PLAYER_NAME_NOT_AVAILABLE,
        WRONG_PASSWORD,
        NOT_REGISTERED,
        ERROR_OCCURRED
    }

    public enum ChangePasswordResponse {
        SUCCESSFULLY_CHANGED,
        WRONG_OLD_PASSWORD,
        ERROR_OCCURRED
    }

    public enum LoginResponse {
        SUCCESSFULLY_LOGGED_IN,
        PASSWORD_DO_NOT_MATCH,
        ALREADY_REGISTERED,
        NOT_REGISTERED,
        ERROR_OCCURRED
    }
}
