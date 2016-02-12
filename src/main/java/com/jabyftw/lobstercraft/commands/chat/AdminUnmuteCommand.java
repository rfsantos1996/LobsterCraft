package com.jabyftw.lobstercraft.commands.chat;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OfflinePlayerHandler;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.command.CommandSender;

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
public class AdminUnmuteCommand extends CommandExecutor {

    public AdminUnmuteCommand() {
        super("adminunmute", Permissions.CHAT_ADMIN_MUTE, "Permite ao jogador dessilenciar um jogador de todo o servidor", "/adminunmute (jogador)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onAdminUnmute(CommandSender commandSender, OfflinePlayerHandler target) {
        if (!target.isRegistered()) {
            commandSender.sendMessage("§cJogador não encontrado.");
            return true;
        }

        if (LobsterCraft.chatService.moderatorUnmutePlayer(target.getPlayerId())) {
            if (target.isOnline())
                target.getPlayerHandler().sendMessage("§6Você foi dessilenciado por §c" + commandSender.getName());
            commandSender.sendMessage("§6Jogador " + target.getPlayerName() + "§6 foi dessilenciado");
            return true;
        } else {
            commandSender.sendMessage("§cEste jogador não está silenciado");
            return true;
        }
    }
}
