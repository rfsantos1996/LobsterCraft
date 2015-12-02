package com.jabyftw.pacocacraft.player;

import com.jabyftw.Util;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.location.TeleportProfile;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

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
public class UserProfile {

    // Player instance
    private Player player = null;

    // Profile interface
    private final Map<ProfileType, PlayerProfile> playerProfiles = Collections.synchronizedMap(new EnumMap<>(ProfileType.class));

    // Login information/base profile
    private long playerId = -1;
    private String playerName;
    private String password = null;
    private long lastTimeOnline = -1;

    // Variables (not database related)
    // If any valuable value is changed, 'modified' must be set true so the profile can be saved upon log off
    private boolean modified = false;
    private boolean loggedIn = false;
    private PlayerMoment preLoginMoment = null;


    /**
     * Create user profile given player's name for first login identification
     * NOTE: player name can be changed ingame
     *
     * @param playerName player's name
     */
    public UserProfile(@NotNull String playerName) {
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
    public UserProfile(@NotNull String playerName, long playerId, @NotNull String password, long lastTimeOnline) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.password = password;
        this.lastTimeOnline = lastTimeOnline;
    }

    /**
     * Set Player instance for user profile (inserted PlayerJoinEvent - Highest priority and removed at PlayerQuitEvent - lowest priority)
     * <p>
     * <b>NOTE: This WON'T keep UserProfile instance alive! This will be handled at UserLoginService</b>
     *
     * @param player null player means player logged off (user profile will be queued by UserLoginService)
     *
     * @throws IllegalStateException if tried to set an already set UserProfile
     * @see com.jabyftw.pacocacraft.login.UserLoginService
     */
    public void setPlayerInstance(@Nullable Player player) {
        // Check for state errors
        if(this.player != null && player != null)
            throw new IllegalStateException("User " + this.player.getName() + " is trying to be set as " + player.getName());
        if(this.player == null && player == null)
            throw new IllegalStateException("User profile " + this.playerName + " is being set null player while it hasn't a player");

        if(player == null) { // Empty profiles - player is logging off (reset variables)

            // Update last time online if logged in (modified = true because even if password is not set, it won't get saved)
            if(isLoggedIn()) {
                lastTimeOnline = System.currentTimeMillis();
                modified = true;
            }

            // Restore login moment if player isn't logged in still
            if(preLoginMoment != null) preLoginMoment.restorePlayerMoment();

            // Set logged in to false in the end (because it may be checked before)
            loggedIn = false;

            // Remove player from list
            PacocaCraft.playerMap.remove(this.player);

            // Apply changes to intern player (after everything is checked)
            this.player = null;

        } else {            // Initialize player stuff

            // Apply changes to intern player (before setting stuff and therefore requiring player != null)
            this.player = player;

            // Add player to list (player is set)
            PacocaCraft.playerMap.put(player, this);

            // Update display name and send greetings
            player.setDisplayName(ChatColor.translateAlternateColorCodes('&', PacocaCraft.chat.getPlayerPrefix(player) + player.getName() + PacocaCraft.chat.getPlayerSuffix(player)));
            player.sendMessage(
                    (password == null ? "§6Bem vindo, " : "§6Bem vindo novamente, ") +
                            player.getDisplayName() + "§6!" +
                            (password == null ? "" : "\n§6Você entrou pela ultima vez em §c" + Util.parseTimeInMillis(lastTimeOnline, "dd/MM/yyyy HH:mm"))
            );

            // Store before login information
            preLoginMoment = new PlayerMoment(player);

            // Set login 'ideal status' (no potion effects, no pending damage etc)
            PlayerMoment.setIdealStatus(player);

            // Teleport to spawn (without saving player's last location but saving server's before-login location)
            PacocaCraft.getUser(player).getProfile(TeleportProfile.class).teleportInstantaneously(player.getWorld().getSpawnLocation(), false);
        }
    }

    /**
     * Apply PlayerProfile to User
     *
     * @param profile desired profile
     *
     * @throws IllegalStateException if profile was already applied or if user already have this profile
     */
    public <T extends PlayerProfile> void applyProfile(@NotNull T profile) {
        // Check if profile isn't already set
        if(profile.userProfile != null)
            throw new IllegalStateException("Attempt to add " + profile.userProfile.playerName + "'s profile on " + playerName);

        // Check if player doesn't have this profile already
        ProfileType profileType = profile.getProfileType();
        if(playerProfiles.containsKey(profileType))
            throw new IllegalStateException("Attempt to override " + profileType);

        // Apply player profile
        profile.userProfile = this;
        playerProfiles.put(profileType, profile);
    }


    /**
     * Get profile given its class
     *
     * @param profileClass profile's class for check up
     * @param <T>          generic type that extends PlayerProfile
     *
     * @return desired profile or null if none found
     *
     * @throws ClassCastException if wrong profile class is given
     */
    @SuppressWarnings("unchecked")
    public <T extends PlayerProfile> T getProfile(Class<T> profileClass) {
        return (T) playerProfiles.getOrDefault(ProfileType.getProfileType(profileClass), null);
    }

    /**
     * Getter for Player ID, used on MySQL for in-database identification
     *
     * @return player's ID
     */
    public long getPlayerId() {
        return playerId;
    }

    /**
     * Getter for Player instance
     *
     * @return Bukkit's player instance
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Check if profile was modified and if it should be saved
     *
     * @return true if profile was modified
     */
    public boolean shouldBeSaved() {
        return modified && password != null;
    }

    /**
     * Check if player is logged on (used to deny movement, most commands, events etc)
     *
     * @return true if player is logged in
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Set player as logged in given password typed by player (if none set, player will be registered and password will be set)
     *
     * @param encryptedPassword encrypted user given password (caller must encrypt the password)
     *
     * @return true if successful log in
     */
    public boolean attemptLogin(@NotNull String encryptedPassword) {
        // If password is registered and it isn't equal as given password, return false
        if(this.password != null && !this.password.equals(encryptedPassword))
            return false;
        // Else, it isn't set or player successfully logged in

        // If password is null, register player
        if(this.password == null) {
            this.password = encryptedPassword;
            modified = true;
        }

        // Log player in (restore items and allow events)
        this.loggedIn = true;
        preLoginMoment.restorePlayerMoment();
        preLoginMoment = null;
        return true;
    }
}
