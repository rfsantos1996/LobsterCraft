package com.jabyftw.lobstercraft.commands.ban;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OfflinePlayer;
import com.jabyftw.lobstercraft.player.PlayerHandlerService;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.command.CommandSender;

import java.util.Set;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
public class BanHistoryCommand extends CommandExecutor {

    public BanHistoryCommand() {
        super("banhistory", Permissions.BAN_SEE_HISTORY.toString(), "Permite ao moderador verificar o histórico do jogador", "/banhistory (jogador)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    private boolean onList(CommandSender commandSender, OfflinePlayer target) {
        if (!target.isRegistered()) {
            commandSender.sendMessage("§cJogador não registrado.");
            return true;
        }

        Set<PlayerHandlerService.BannedPlayerEntry> banEntries = LobsterCraft.servicesManager.playerHandlerService.getPlayerBanEntries(target.getPlayerId());
        if (banEntries.isEmpty()) {
            commandSender.sendMessage("§cNenhum ban encontrado para §6" + target.getPlayerName());
            return true;
        } else {
            StringBuilder stringBuilder = new StringBuilder("§c=== §6Histórico de §4").append(target.getPlayerName()).append(" §c===");
            boolean first = true;

            for (PlayerHandlerService.BannedPlayerEntry banEntry : banEntries) {
                if (!first) stringBuilder.append("§c, ");
                first = false;

                stringBuilder
                        .append("§c").append(Util.formatDate(banEntry.getRecordDate())).append("§6 | ")
                        .append("§c").append(banEntry.getBanType().getTypeName()).append("§6 | ");
                stringBuilder.append("§c").append(banEntry.getModeratorId() != null ?
                        LobsterCraft.servicesManager.playerHandlerService.getOfflinePlayer(banEntry.getModeratorId()).getPlayerName()
                        : "console").append("§6 | ");
                stringBuilder.append("§c\"").append(banEntry.getReason()).append("\"");
                if (banEntry.getBanType() == PlayerHandlerService.BanType.PLAYER_TEMPORARILY_BANNED)
                    stringBuilder.append("§6 | §cbanido até ").append(Util.formatDate(banEntry.getUnbanDate()));
            }
            commandSender.sendMessage(stringBuilder.toString());
            return true;
        }
    }
}
