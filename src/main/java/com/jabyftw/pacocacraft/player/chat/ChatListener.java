package com.jabyftw.pacocacraft.player.chat;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.invisibility.InvisibilityService;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.Permissions;
import com.jabyftw.profile_util.PlayerHandler;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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
public class ChatListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST) // the last priority
    public void onChat(AsyncPlayerChatEvent chatEvent) {
        // Cancel all events that got here (cancelled before are just ignored)
        chatEvent.setCancelled(true);

        // Return if player is hidden and don't have permission to talk
        if(InvisibilityService.isPlayerHidden(chatEvent.getPlayer()) && !PacocaCraft.permission.has(chatEvent.getPlayer(), Permissions.PLAYER_TALK_HIDDEN))
            return;

        // Send player a "chat sound" (this volume is actually louder than the normal exp, should be fine)
        BukkitScheduler.runTask(PacocaCraft.pacocaCraft, () -> chatEvent.getPlayer().playSound(chatEvent.getPlayer().getLocation(), Sound.ORB_PICKUP, 10, 1));

        PlayerHandler sender = PacocaCraft.getPlayerHandler(chatEvent.getPlayer());
        boolean heard = false;

        // Send messages to every player
        for(PlayerHandler playerHandler : PacocaCraft.getOnlinePlayers()) {
            if(playerHandler.equals(sender)) continue;
            if(playerHandler.getProfile(ChatProfile.class).sendChatMessage(sender, chatEvent.getMessage())) heard = true;
        }

        // If any player heard him, allow message
        if(heard) sender.getProfile(ChatProfile.class).sendChatMessage(sender, chatEvent.getMessage());
    }
}
