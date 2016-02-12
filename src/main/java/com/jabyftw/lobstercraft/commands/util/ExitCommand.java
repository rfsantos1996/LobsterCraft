package com.jabyftw.lobstercraft.commands.util;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
public class ExitCommand extends CommandExecutor {

    public ExitCommand() {
        super("exit", Permissions.UTIL_EXIT_SERVER, "Permite ao jogador fechar o servidor", "/exit");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onExit(CommandSender commandSender) {
        // Set as closing so players can't log in again
        LobsterCraft.serverClosing = true;

        // Kick every player
        for (Player player : Bukkit.getServer().getOnlinePlayers())
            // Here the events are still called, shutting down players correctly
            player.kickPlayer("§cServidor sendo reiniciado.");

        // Remove all dropped items that are safe
        LobsterCraft.playerService.playerListener.removeAllSafeItems();

        // Shutdown server after 1 tick;
        BukkitScheduler.runTask(() -> {
            Bukkit.broadcastMessage("Starting to shutdown...");
            Bukkit.getServer().shutdown();
        });
        return true;
    }
}
