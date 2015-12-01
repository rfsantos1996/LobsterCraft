package com.jabyftw.pacocacraft.player;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.entity.Player;

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
    private long playerId;
    private String playerName;
    private String password = null;

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
        // Check for errors and put player on online player list
        if(this.player != null && player != null)
            throw new IllegalStateException("User " + this.player.getName() + " is trying to be set as " + player.getName());

        // Apply changes to player list before changing user
        if(player != null) {                // Add player to list (player is set)
            PacocaCraft.playerMap.put(player, this);
        } else if(this.player != null) {    // else remove player from list (player isn't set and current player isn't null)
            PacocaCraft.playerMap.remove(this.player);
        }

        // Apply changes to intern player
        this.player = player;

        if(player == null) { // Empty profiles (reset variables)
            // TODO
        } else {            // Initialize player stuff
            // TODO
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
}
