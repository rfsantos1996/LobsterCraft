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
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
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

    private final OfflinePlayerHandler offlinePlayer;

    private Player player;
    private PlayerState preLoginState = null, gamemodeState = null;
    private BuildMode buildMode = BuildMode.DEFAULT;
    private TeleportBuilder.Teleport pendingTeleport;
    private volatile CommandSender lastWhisper = null;
    private boolean loggedIn = false;
    private boolean godMode = false;
    private long loginTime; // Used to calculate playerTime

    public PlayerHandler(@NotNull final OfflinePlayerHandler offlinePlayer, @NotNull final Player player) {
        this.offlinePlayer = offlinePlayer;

        // Check if player name matches
        if (!player.getName().equalsIgnoreCase(offlinePlayer.getPlayerName()))
            throw new IllegalArgumentException("Player isn't owner of this PlayerHandler (" + offlinePlayer.getPlayerName() + ")");

        // Check if OfflinePlayer already have been assigned before
        if (this.offlinePlayer.playerHandler != null)
            throw new IllegalArgumentException("OfflinePlayerHandler has already a PlayerHandler instance (" + offlinePlayer.getPlayerName() + ")");

        // Set player instance
        this.player = player;
        this.offlinePlayer.playerHandler = this;

        // Update display name
        player.setDisplayName(ChatColor.translateAlternateColorCodes('&', LobsterCraft.chat.getPlayerPrefix(player) + player.getName() + LobsterCraft.chat.getPlayerSuffix(player)));

        // If the state isn't null, restore it
        if (preLoginState != null) preLoginState.restorePlayerState();

        // Set player gamemode to SURVIVAL
        setGameMode(GameMode.SURVIVAL);
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
            // Restore player gamemode
            setGameMode(GameMode.SURVIVAL);

            // Save lastTimeOnline and timeOnline
            offlinePlayer.setLastTimeOnline(System.currentTimeMillis());
            offlinePlayer.addPlayTime(Math.abs(System.currentTimeMillis() - loginTime));

            // Set as not logged in
            this.loggedIn = false;
        }

        // Queue saving all profiles since player is registered
        if (isRegistered())
            synchronized (playerProfiles) {
                // Clear all profiles and store them
                playerProfiles.forEach(((profileType, profile) -> profile.destroyProfile()));
                LobsterCraft.profileService.storeProfiles(getPlayerId(), playerProfiles.values());
                playerProfiles.clear();
            }

        // Save player data
        player.saveData();

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
        offlinePlayer.setPassword(encryptedPassword);
        offlinePlayer.setLastIp(player.getAddress().getAddress().getHostAddress()); // this was null (updated on forceLogin(), only)

        // Save profile, retrieve playerId
        try {
            offlinePlayer.registerPlayerId();
        } catch (SQLException e) {
            e.printStackTrace();
            return LoginResponse.ERROR_OCCURRED;
        }

        // Assure playerId is greater than 0
        if (getPlayerId() < 0) return LoginResponse.ERROR_OCCURRED;

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
        if (!offlinePlayer.getPassword().equals(encryptedPassword))
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
        FutureTask<Set<Profile>> retrieveProfiles = LobsterCraft.profileService.retrieveProfiles(getPlayerId());
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
                offlinePlayer.setLastIp(player.getAddress().getAddress().getHostAddress());
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
        if (!offlinePlayer.getPassword().equals(encryptedPassword))
            return ChangeNameResponse.WRONG_PASSWORD;

        // Make sure new player can log in with this name
        if (LobsterCraft.playerHandlerService.blockedNames.contains(newPlayerName.toLowerCase()) || Util.checkStringCharactersAndLength(newPlayerName, 3, 16))
            return ChangeNameResponse.PLAYER_NAME_NOT_VALID;

        {
            NameChangeEntry nameChangeEntry = LobsterCraft.playerHandlerService.getNameChangeEntry(getPlayerId());

            // Check if player can change name
            if (nameChangeEntry != null && !nameChangeEntry.canPlayerChangeUsername())
                return ChangeNameResponse.CANT_CHANGE_NAME_YET;
            // Player can restore its name after a change because we do not check if username is available when using his NameChangeEntry
        }

        // Check if name is already available
        for (NameChangeEntry nameChangeEntry : LobsterCraft.playerHandlerService.getNameChangeEntries()) {
            // If another player owned the name and it is this name
            if (nameChangeEntry.getPlayerId() != getPlayerId() && nameChangeEntry.getOldPlayerName().equalsIgnoreCase(newPlayerName)) { // If it is this name
                // If name isn't available
                if (!nameChangeEntry.isNameAvailable())
                    return ChangeNameResponse.PLAYER_NAME_NOT_AVAILABLE;
            }
        }

        {
            OfflinePlayerHandler offlinePlayer = LobsterCraft.playerHandlerService.getOfflinePlayer(newPlayerName);
            // Check if player is registered (there's a player that have the same name using it)
            if (offlinePlayer.isRegistered())
                return ChangeNameResponse.PLAYER_NAME_NOT_AVAILABLE;
        }

        try {
            // Insert NameChangeEntry on database
            NameChangeEntry nameChangeEntry = LobsterCraft.playerHandlerService.getNameChangeEntry(getPlayerId());
            NameChangeEntry.saveNameChangeEntry(nameChangeEntry == null, nameChangeEntry == null ? new NameChangeEntry(offlinePlayer) : nameChangeEntry.setOldPlayerName(offlinePlayer.getPlayerName()));

            // Update playerName (this will update OfflineProfiles map
            offlinePlayer.setPlayerName(newPlayerName);

            Permission permission = LobsterCraft.permission;
            String primaryGroup = permission.getPrimaryGroup(player);

            // Remove player from group and add new player to the group
            permission.playerRemoveGroup(player, primaryGroup);
            //noinspection deprecation
            permission.playerAddGroup(player.getWorld().getName(), Bukkit.getOfflinePlayer(newPlayerName), primaryGroup);
        } catch (SQLException e) {
            e.printStackTrace();
            return ChangeNameResponse.ERROR_OCCURRED;
        }

        BukkitScheduler.runTask(() -> player.kickPlayer("§6Seu nome foi alterado!\n§6Entre com o outro usuário."));
        return ChangeNameResponse.SUCCESSFULLY_CHANGED;
    }

    public ChangePasswordResponse changePlayerPassword(@NotNull final String oldPassword, @NotNull final String newPassword) {
        String encryptedPassword;
        try {
            // Check if the old password corresponds to the new one
            if (!Util.encryptString(oldPassword).equals(offlinePlayer.getPassword()))
                return ChangePasswordResponse.WRONG_OLD_PASSWORD;

            // Encrypt new password
            encryptedPassword = Util.encryptString(newPassword);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return ChangePasswordResponse.ERROR_OCCURRED;
        }

        // Change password
        offlinePlayer.setPassword(encryptedPassword);
        return ChangePasswordResponse.SUCCESSFULLY_CHANGED;
    }

    public <T extends Profile> T getProfile(Class<T> profileClass) {
        ProfileType profileType = ProfileType.getType(profileClass);
        if (profileType == null) throw new IllegalStateException("ProfileType is not registered!");
        return (T) profileClass.cast(playerProfiles.get(profileType));
    }

    public OfflinePlayerHandler getOfflinePlayer() {
        return offlinePlayer;
    }

    public String getPlayerName() {
        return offlinePlayer.getPlayerName();
    }

    public long getPlayerId() {
        return offlinePlayer.getPlayerId();
    }

    public DatabaseState getDatabaseState() {
        return offlinePlayer.getDatabaseState();
    }

    public boolean isRegistered() {
        return offlinePlayer.isRegistered();
    }

    public Player getPlayer() {
        return player;
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
        return buildMode.getType() == ProtectionType.ADMIN_PROTECTION ? ((AdministratorBuildMode) buildMode).getConstructionId() : getPlayerId();
    }

    public void sendMessage(@NotNull final String message) {
        player.sendMessage(message);
    }

    public boolean isInvisible() {
        return LobsterCraft.vanishManager.isVanished(player);
    }

    public CommandSender getLastWhisper() {
        return lastWhisper;
    }

    public void setLastWhisper(@Nullable final CommandSender lastWhisper) {
        this.lastWhisper = lastWhisper;
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

    public boolean setGameMode(GameMode gameMode) {
        // Do nothing if we're changing to he same gamemode
        if (gameMode == player.getGameMode()) return false;

        // Restore player's last state
        if (gamemodeState != null) {
            // Clear current inventory
            player.getInventory().clear();
            // Restore player inventory and state
            gamemodeState.restorePlayerState();
            gamemodeState = null;
        }

        // If isn't going to change to survival
        if (gameMode != GameMode.SURVIVAL) {
            // Store player's current state (after restoring the past one, if necessary)
            gamemodeState = new PlayerState(this);
            gamemodeState.clearPlayer();
        }

        // Finally, update its game mode
        player.setGameMode(gameMode);
        return true;
    }

    public ConditionController getConditionController() {
        return conditionController;
    }

    public TeleportBuilder.Teleport getPendingTeleport() {
        return pendingTeleport;
    }

    public void setPendingTeleport(@Nullable TeleportBuilder.Teleport pendingTeleport) {
        this.pendingTeleport = pendingTeleport;
    }

    public LinkedList<OreBlockLocation> getOreLocationHistory() {
        return oreLocationHistory;
    }

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
        // TODO check this when offline player
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(7, 17);
        if (!isLoggedIn())
            hashCodeBuilder.append(getPlayerName());
        else if (isRegistered())
            hashCodeBuilder.append(getPlayerId());
        else
            throw new IllegalStateException("Player is logged in without playerId");
        return hashCodeBuilder.toHashCode();
    }


    public enum ChangeNameResponse {
        SUCCESSFULLY_CHANGED,
        CANT_CHANGE_NAME_YET,
        PLAYER_NAME_NOT_VALID,
        PLAYER_NAME_NOT_AVAILABLE,
        WRONG_PASSWORD,
        NOT_REGISTERED,
        ERROR_OCCURRED;
    }


    public enum ChangePasswordResponse {
        SUCCESSFULLY_CHANGED,
        WRONG_OLD_PASSWORD,
        ERROR_OCCURRED;
    }


    public enum LoginResponse {
        SUCCESSFULLY_LOGGED_IN,
        PASSWORD_DO_NOT_MATCH,
        ALREADY_REGISTERED,
        NOT_REGISTERED,
        ERROR_OCCURRED;
    }
}
