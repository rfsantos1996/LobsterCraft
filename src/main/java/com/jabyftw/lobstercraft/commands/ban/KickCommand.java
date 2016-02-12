package com.jabyftw.lobstercraft.commands.ban;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.BannedPlayerEntry;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
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
public class KickCommand extends CommandExecutor {

    public KickCommand() {
        super("kick", Permissions.BAN_KICK_PLAYER, "Permite ao moderador expulsar jogadores", "/kick (jogador) (motivo)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public HandleResponse onKickPlayer(CommandSender commandSender, PlayerHandler playerHandler, String... reasonArray) {
        if (!(commandSender instanceof Player) && LobsterCraft.permission.has(playerHandler.getPlayer(), Permissions.BAN_KICK_EXCEPTION))
            return HandleResponse.RETURN_NO_PERMISSION;

        BukkitScheduler.runTaskAsynchronously(() -> {
            String reason = Util.retrieveMessage(reasonArray);

            switch (LobsterCraft.playerHandlerService.kickPlayer(
                    playerHandler.getOfflinePlayer(),
                    commandSender instanceof Player ? LobsterCraft.playerHandlerService.getPlayerHandler((Player) commandSender).getPlayerId() : null,
                    BannedPlayerEntry.BanType.PLAYER_KICKED,
                    reason,
                    null
            )) {
                case SUCCESSFULLY_BANNED:
                    commandSender.sendMessage("§6Jogador " + playerHandler.getPlayer().getDisplayName() + " §6foi expulso.");
                    break;
                case INVALID_REASON_LENGTH:
                    commandSender.sendMessage("§cMotivo inválido (muito curto ou longo)");
                    break;
                case PLAYER_NOT_REGISTERED:
                case INVALID_BAN_TYPE:
                case ERROR_OCCURRED:
                    commandSender.sendMessage("§4Ocorreu um erro!");
                    break;
            }
        });
        return HandleResponse.RETURN_TRUE;
    }
}
