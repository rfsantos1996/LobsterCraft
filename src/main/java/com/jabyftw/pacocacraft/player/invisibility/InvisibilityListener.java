package com.jabyftw.pacocacraft.player.invisibility;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
public class InvisibilityListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent joinEvent) {
        // Keep hidden players alone
        for(Player hiddenPlayer : InvisibilityService.hiddenPlayers)
            joinEvent.getPlayer().hidePlayer(hiddenPlayer);

        // Keep alone players alone
        for(Player alonePlayer : InvisibilityService.alonePlayers)
            alonePlayer.hidePlayer(joinEvent.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent quitEvent) {
        // Don't let Player instances on the list
        InvisibilityService.showPlayerToEveryone(quitEvent.getPlayer());
        InvisibilityService.showEveryoneToPlayer(quitEvent.getPlayer());
    }
}
