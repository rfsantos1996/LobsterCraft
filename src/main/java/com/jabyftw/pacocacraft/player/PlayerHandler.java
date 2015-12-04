package com.jabyftw.pacocacraft.player;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.pacocacraft.player.invisibility.InvisibilityService;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang.builder.HashCodeBuilder;
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
public class PlayerHandler {

    // Player instance
    private final Player player;

    // Profile interface
    private final Map<ProfileType, BasePlayerProfile> playerProfiles = Collections.synchronizedMap(new EnumMap<>(ProfileType.class));

    /**
     * Create PlayerHandle instance (holder of all profiles and variables)
     * Initiated on PlayerJoinEvent (highest priority) and destroyed on PlayerQuitEvent (lowest priority)
     *
     * @param player      Bukkit's Player instance
     * @param userProfile base player's UserProfile so he can log in
     *
     * @see com.jabyftw.pacocacraft.login.UserLoginService Profiles are kept under this queue until save-time
     * // TODO change ^
     */
    public PlayerHandler(@NotNull Player player, @NotNull UserProfile userProfile) {
        this.player = player;

        // Add player to list (player is set)
        PacocaCraft.playerMap.put(player, this);

        // Add user profile (acquired at AsyncPreLogin - before PlayerJoin)
        applyProfile(userProfile);
    }

    public void destroy() {
        // TODO Deliver profiles to somewhere else and use their onPlayerHandleDestruction()

        // Remove player from list (instance will be removed)
        if(!PacocaCraft.playerMap.remove(this.player, this))
            throw new IllegalStateException("Tried to remove PlayerHandler but it is stored as something else");
    }

    /**
     * Apply BasePlayerProfile to PlayerHandler
     *
     * @param profile profile to be applied
     *
     * @throws IllegalStateException if profile was already applied or if user already have this profile
     */
    public <T extends BasePlayerProfile> void applyProfile(@NotNull T profile) throws IllegalStateException {
        // Check if player doesn't have this profile already
        ProfileType profileType = profile.getProfileType();
        if(playerProfiles.containsKey(profileType))
            throw new IllegalStateException("Attempt to override " + profileType.name());

        // Apply player profile
        profile.setPlayerHandler(this);
        playerProfiles.put(profileType, profile);
    }

    public Player getPlayer() {
        return player;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasePlayerProfile> T getProfile(@NotNull ProfileType profileType) {
        return (T) playerProfiles.get(profileType);
    }

    public <T extends BasePlayerProfile> T getProfile(@NotNull Class<T> profileClass) {
        return profileClass.cast(playerProfiles.get(ProfileType.getProfileType(profileClass)));
    }

    /**
     * Conditions:
     * - logged in
     * - visible player
     * - off god mode
     *
     * @return true if player is capable of being damaged
     */
    public boolean isDamageable() {
        // As UserProfile is applied together with PlayerHandler construction, this should be fine
        return getProfile(UserProfile.class).isLoggedIn() && !isInvisible() && !isGodMode();
    }

    public boolean isInvisible() {
        return InvisibilityService.isPlayerHidden(player);
    }

    public boolean isGodMode() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof PlayerHandler && ((PlayerHandler) obj).getPlayer().equals(player);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3, 11)
                .append(player.hashCode())
                .toHashCode();
    }
}
