package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.command.CommandSender;

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
public class KillPlayersCommand extends CommandExecutor {

    public KillPlayersCommand() {
        super("kill", Permissions.PLAYER_KILL.toString(), "Permite ao jogador matar jogadores", "/kill (jogador)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    private boolean onKill(CommandSender commandSender, OnlinePlayer onlinePlayer) {
        onlinePlayer.getPlayer().damage(Double.MAX_VALUE - 1);

        // Make sure he is dead
        if (onlinePlayer.getPlayer().getHealth() > 0d)
            onlinePlayer.getPlayer().setHealth(0d);

        onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§cVocê foi morto por §4", commandSender.getName(), "§c através de comando."));
        commandSender.sendMessage(Util.appendStrings("§cVocê matou ", onlinePlayer.getPlayer().getDisplayName()));
        return true;
    }
}
