package com.jabyftw.pacocacraft.player.invisibility;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.ServerService;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;

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
public class InvisibilityService implements ServerService {

    /**
     * Every player is hidden from player
     */
    protected static final ArrayList<Player> alonePlayers = new ArrayList<>();
    protected static final ArrayList<Player> hiddenPlayers = new ArrayList<>();

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(new InvisibilityListener(), PacocaCraft.pacocaCraft);
    }

    @Override
    public void onDisable() {
    }

    public static void hideEveryoneFromPlayer(@NotNull Player alonePlayer) {
        // Check if server is already hidden
        if(isPlayerAlone(alonePlayer))
            return;
        alonePlayers.add(alonePlayer.getPlayer());

        // Hide each online player to alone player
        Bukkit.getServer().getOnlinePlayers().forEach(alonePlayer::hidePlayer);
    }

    public static void showEveryoneToPlayer(@NotNull Player alonePlayer) {
        // Check if server is already shown
        if(!isPlayerAlone(alonePlayer))
            return;
        alonePlayers.remove(alonePlayer.getPlayer());

        // Show each online player to alone player
        Bukkit.getServer().getOnlinePlayers().forEach(alonePlayer::showPlayer);
    }

    public static void hidePlayerFromEveryone(@NotNull Player hiddenPlayer) {
        // Check if it is already hidden
        if(isPlayerHidden(hiddenPlayer))
            return;
        hiddenPlayers.add(hiddenPlayer.getPlayer());

        // Hide player to each online player
        for(Player player : Bukkit.getServer().getOnlinePlayers())
            player.hidePlayer(hiddenPlayer);
    }

    public static void showPlayerToEveryone(@NotNull Player hiddenPlayer) {
        // Check if it is already shown
        if(!isPlayerHidden(hiddenPlayer))
            return;
        hiddenPlayers.remove(hiddenPlayer);

        // Show player to each online player
        for(Player player : Bukkit.getServer().getOnlinePlayers())
            player.showPlayer(hiddenPlayer);
    }

    public static boolean isPlayerHidden(Player player) {
        return hiddenPlayers.contains(player);
    }

    public static boolean isPlayerAlone(Player player) {
        return alonePlayers.contains(player);
    }

    /*public static final Object storedProfileLock = new Object();
    private final HashBasedTable<String, ProfileType, BasePlayerProfile> storedProfiles = HashBasedTable.create();
    private BukkitTask storedProfilesTask;


    /**
     * Store profile instance in case player rejoins in a few minutes
     * UserLoginService will save it on destruction
     *
     * @param userProfile given user profile of a logged off player
     *
    public void storeProfile(@NotNull UserProfile userProfile) {
        // Make sure player is removed
        if(userProfile.getPlayer() != null)
            throw new IllegalStateException("Tried to store a logged on user profile: " + userProfile.getPlayer().getName() + " which " +
                    (userProfile.getPlayer().isOnline() ? "is online." : "is offline."));

        storedProfiles.put(userProfile.getPlayerName().toLowerCase(), userProfile);
    }

    private class StoredProfilesSavingTask implements Runnable {
        // TODO stored user profiles: set last time online on UserProfile and check its lifetime (if passed some time, save it and remove from cache)

        private final long timeNeededToProfile = TimeUnit.SECONDS.toMillis(ConfigValue.LOGIN_PROFILE_WAITING_TIME.<Long>getValue());

        @Override
        public void run() {
            // Iterate while has next
            Iterator<UserProfile> iterator = storedProfiles.values().iterator();
            while(iterator.hasNext()) {
                UserProfile userProfile = iterator.next();

                // If profile has been already been queued for long
                if((System.currentTimeMillis() - userProfile.getLastTimeOnlineMillis()) > timeNeededToProfile) {
                    iterator.remove();
                }
            }
        }
    }*/

}
