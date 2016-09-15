package com.jabyftw.lobstercraft.commands.chat;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft_old.LobsterCraft;
import com.jabyftw.lobstercraft_old.player.OfflinePlayerHandler;
import com.jabyftw.lobstercraft_old.player.util.Permissions;
import com.jabyftw.lobstercraft.util.Util;
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
public class AdminMuteCommand extends CommandExecutor {

    public AdminMuteCommand() {
        super("adminmute", Permissions.CHAT_ADMIN_MUTE, "Permite ao jogador silenciar um jogador de todo o servidor", "/adminmute (jogador) (tempo silenciado) (motivo)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onAdminMute(CommandSender commandSender, OfflinePlayerHandler target, Long timeSilenced, String... reasonArray) {
        String reason = Util.retrieveMessage(reasonArray);

        if (!Util.checkStringLength(reason, 4, 128)) {
            commandSender.sendMessage("§cMotivo está muito longo ou muito curto.");
            return true;
        }

        if (!target.isRegistered()) {
            commandSender.sendMessage("§cJogador não encontrado.");
            return true;
        }

        long unmuteDate = LobsterCraft.chatService.moderatorMutePlayer(
                target.getPlayerId(),
                commandSender instanceof Player ? LobsterCraft.playerHandlerService.getPlayerHandler((Player) commandSender).getPlayerId() : null,
                reason,
                timeSilenced
        );

        String format = Util.formatDate(unmuteDate);

        if (target.isOnline())
            target.getPlayerHandler().sendMessage("§6Você foi silenciado por §c" + commandSender.getName() + "§6 até §c" + format + "§6 pelo motivo: §c\"" + reason + "\"");
        commandSender.sendMessage(target.getPlayerName() + "§6 foi silenciado até §c" + format);
        return true;
    }
}
