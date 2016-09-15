package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Iterator;

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
        super("list", Permissions.PLAYER_LIST.toString(), "Permite ao jogador ver a lista de pessoas online", "/list");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    private boolean onList(CommandSender commandSender) {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

        StringBuilder stringBuilder = new StringBuilder("§6Jogadores ")
                .append("(§c").append(onlinePlayers.size()).append("§6/§c").append(Bukkit.getMaxPlayers()).append("):");

        // Iterate through all players
        Iterator<? extends Player> iterator = onlinePlayers.iterator();

        while (iterator.hasNext()) {
            Player player = iterator.next();

            // Add if player don't have permission to be hidden or if player isn't hidden
            if (!LobsterCraft.permission.has(player, Permissions.PLAYER_LIST_HIDDEN.toString()) && !LobsterCraft.vanishManager.isVanished(player)) {
                stringBuilder.append("§c").append(player.getDisplayName());
                if (iterator.hasNext()) stringBuilder.append("§6, ");
            }
        }

        commandSender.sendMessage(stringBuilder.toString());
        return true;
    }
}
