package com.jabyftw.pacocacraft.player;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.block.xray_protection.OreLocation;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.pacocacraft.player.invisibility.InvisibilityService;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
public class PlayerHandler {

    // Player instance
    private final Player player;

    // Profile interface
    private final Map<ProfileType, BasePlayerProfile> playerProfiles = Collections.synchronizedMap(new EnumMap<>(ProfileType.class));

    // Variables
    private boolean godMode = false;
    private final LinkedList<OreLocation> oreLocationHistory = new LinkedList<>();

    /**
     * Create PlayerHandle instance (holder of all profiles and variables)
     * Initiated on PlayerJoinEvent (lowest priority - the first one) and destroyed on PlayerQuitEvent (monitor priority)
     *
     * @param player      Bukkit's Player instance
     * @param userProfile base player's UserProfile so he can log in
     *
     * @see PlayerService#storeProfile(PlayerProfile) is where profiles are sent after player log off
     */
    public PlayerHandler(@NotNull Player player, @NotNull UserProfile userProfile) {
        this.player = player;

        // Add player to list (player is set)
        PacocaCraft.playerMap.put(player, this);

        // Add user profile (acquired at AsyncPreLogin - before PlayerJoin)
        applyProfile(userProfile);
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

    public void destroy() {
        Iterator<BasePlayerProfile> iterator = playerProfiles.values().iterator();
        while(iterator.hasNext()) {
            BasePlayerProfile profile = iterator.next();

            // Remove player handler for profile (prepare for destruction/reconstruction)
            profile.setPlayerHandler(null);

            // Send all PlayerProfiles to storage (as it is compatible)
            if(profile instanceof UserProfile) {
                // Send UserProfile (BasePlayerProfile) to its personal storage
                PacocaCraft.userLoginService.storeProfile((UserProfile) profile);
            } else if(profile instanceof PlayerProfile) {
                // Send other profiles (PlayerProfile) to general storage
                PacocaCraft.playerService.storeProfile((PlayerProfile) profile);
            } else {
                PacocaCraft.logger.warning(profile.getProfileType().name() + " is not handled at PlayerHandler#destroy");
            }

            // Remove profile from player handler
            iterator.remove();
        }

        // Remove player from list (instance will be removed)
        if(!PacocaCraft.playerMap.remove(this.player, this))
            throw new IllegalStateException("Tried to remove PlayerHandler but it is stored as something else");
    }

    public Player getPlayer() {
        return player;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasePlayerProfile> T getProfile(@NotNull ProfileType profileType) {
        return getProfile((Class<T>) profileType.getProfileClass());
    }

    /**
     * Get requested profile
     * <b><i>NOTE:</i> caller must be sure to check existence conditions</b> (player must be logged in so the profile can be loaded)
     *
     * @param profileClass profile's class
     * @param <T>          class type of the requested profile (must be a BasePlayerProfile)
     *
     * @return requested profile
     *
     * @see BasePlayerProfile as the base of every profile
     * @see ClassCastException will probably be thrown if profile isn't loaded
     */
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
        return godMode;
    }

    /**
     * Sets the player in or out of god mode
     * NOTE: this already warns the player about its state
     *
     * @param godMode true if player should be set to god mode
     */
    public boolean setGodMode(boolean godMode, @Nullable CommandSender otherPlayer) {
        // If he is and is set back to normal mode, warn player
        if(isGodMode() && !godMode)
            player.sendMessage(otherPlayer != null ? "§c" + otherPlayer.getName() + "§c te tirou do modo deus (god mode)." : "§cVocê saiu do modo deus (god mode).");
        else if(!isGodMode() && godMode)
            player.sendMessage(otherPlayer != null ? "§6" + otherPlayer.getName() + "§6 te colocou em modo deus (god mode)." : "§6Você entrou no modo deus (god mode).");
        else return isGodMode(); // Nothing changed

        this.godMode = godMode;
        return isGodMode();
    }

    public LinkedList<OreLocation> getOreLocationHistory() {
        return oreLocationHistory;
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
