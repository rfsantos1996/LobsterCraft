package com.jabyftw.lobstercraft.player.listeners;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.chat.ChatProfile;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
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
public class ChatListener implements Listener {

    private static final String CHAT_FORMAT = "%1$s§r: %2$s";

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        // Ignore previously cancelled events
        if (event.isCancelled()) return;

        // Cancel all events
        event.setCancelled(true);

        // Do not allow vanished players to talk
        if (LobsterCraft.vanishManager.isVanished(event.getPlayer()))
            return;

        PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer());

        // Do not allow not logged in players
        if (playerHandler == null) return;

        // TODO check if player is moderator-muted

        // Useless, but set format for event in case someone uses it
        event.setFormat(CHAT_FORMAT);

        // Format chat
        String message = String.format(CHAT_FORMAT, event.getPlayer().getDisplayName(), event.getMessage());
        boolean heard = false;

        // Iterate through LOGGED IN players
        for (PlayerHandler onlinePlayer : LobsterCraft.playerHandlerService.getOnlinePlayers()) {
            // Ignore itself
            if (onlinePlayer.equals(playerHandler)) continue;

            // Check if onlinePlayer muted playerHandler
            if (onlinePlayer.getProfile(ChatProfile.class).isPlayerMuted(playerHandler.getPlayerId()))
                continue;

            // Send message (doesn't matter that player is invisible)
            onlinePlayer.sendMessage(message);

            // Check as heard if other player is visible
            if (!onlinePlayer.isInvisible())
                heard = true;
        }

        if (heard) {
            playerHandler.sendMessage(message);
            BukkitScheduler.runTask(() -> event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ORB_PICKUP, 10f, 1f));
        }
    }
}
