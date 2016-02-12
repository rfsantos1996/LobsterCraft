package com.jabyftw.lobstercraft.commands.ban;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OfflinePlayerHandler;
import com.jabyftw.lobstercraft.player.util.BannedPlayerEntry;
import com.jabyftw.lobstercraft.player.util.Permissions;
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
        super("banhistory", Permissions.BAN_SEE_HISTORY, "Permite ao moderador verificar o histórico do jogador", "/banhistory (jogador)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onList(CommandSender commandSender, OfflinePlayerHandler target) {
        if (!target.isRegistered()) {
            commandSender.sendMessage("§cJogador não registrado.");
            return true;
        }

        Set<BannedPlayerEntry> banEntries = LobsterCraft.playerHandlerService.getBanEntriesFromPlayer(target.getPlayerId());

        if (banEntries.isEmpty()) {
            commandSender.sendMessage("§cNenhum ban encontrado para §6" + target.getPlayerName());
        }

        StringBuilder stringBuilder = new StringBuilder("§c=== §6Histórico de §4" + target.getPlayerName() + " §c===");
        boolean first = true;

        for (BannedPlayerEntry banEntry : banEntries) {
            if (!first) stringBuilder.append("§c, ");
            first = true;

            stringBuilder
                    .append("§c").append(Util.formatDate(banEntry.getRecordDate())).append("§6 | ")
                    .append("§c").append(banEntry.getBanType().getTypeName()).append("§6 | ")
                    .append("§c").append(LobsterCraft.playerHandlerService.getOfflinePlayer(banEntry.getResponsibleId()).getPlayerName()).append("§6 | ")
                    .append("§c\"").append(banEntry.getReason()).append("\"");
            if (!banEntry.isPermanent())
                stringBuilder.append("§6 | §cbanido até ").append(Util.formatDate(banEntry.getUnbanDate()));
        }

        return true;
    }
}
