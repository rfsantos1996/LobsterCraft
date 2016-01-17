package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.invisibility.InvisibilityService;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;

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
public class ListCommand extends CommandExecutor {

    public ListCommand() {
        super(PacocaCraft.pacocaCraft, "list", Permissions.PLAYER_LIST, "§6Permite ao jogador ver a lista de pessoas online", "§c/list");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onList(CommandSender commandSender) {
        ArrayList<String> playerNames = new ArrayList<>();

        //noinspection Convert2streamapi
        for(Player player : Bukkit.getOnlinePlayers()) {
            // Add if player don't have permission to be hidden or if player isn't hidden
            if(!PacocaCraft.permission.has(player, Permissions.PLAYER_LIST_HIDE) && !InvisibilityService.isPlayerHidden(player))
                playerNames.add(player.getDisplayName() + "§r");
        }

        Util.sendCommandSenderMessage(commandSender, "§6Jogadores: §r" + playerNames);
        return true;
    }
}
