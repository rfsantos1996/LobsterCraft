package com.jabyftw.lobstercraft.commands.ban;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OfflinePlayer;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.PlayerHandlerService;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.player.TriggerController;
import com.jabyftw.lobstercraft.util.Util;
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
public class BanCommand extends CommandExecutor {

    public BanCommand() {
        super("ban", Permissions.BAN_PLAYER_PERMANENTLY.toString(), "Permite ao moderador banir jogadores", "/ban (jogador) (motivo)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    private HandleResponse onBanPlayer(CommandSender commandSender, OfflinePlayer offlinePlayer, String... reasonArray) {
        OnlinePlayer onlinePlayer = offlinePlayer.getOnlinePlayer(null);

        // Check if a not-op is trying to ban a player that have the permission
        if (!commandSender.isOp() && onlinePlayer != null && LobsterCraft.permission.has(onlinePlayer.getPlayer(), Permissions.BAN_EXCEPTION.toString()))
            return HandleResponse.RETURN_NO_PERMISSION;

        OnlinePlayer moderator;
        // If player is offline, asks the sender if he is sure to ban player
        if (commandSender instanceof Player) {
            moderator = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) commandSender, OnlinePlayer.OnlineState.LOGGED_IN);
            if (onlinePlayer == null && moderator.getTriggerController().sendMessageIfNotTriggered(TriggerController.TemporaryTrigger.BAN_PLAYER,
                    Util.appendStrings("§cJogador offline, tem certeza de que quer banir §4\"", offlinePlayer.getPlayerName()), "\"§c? §6Se sim, utilize o comando novamente."))
                return HandleResponse.RETURN_TRUE;
        } else {
            moderator = null;
        }

        // Ban asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(LobsterCraft.plugin, () -> {
            String reason = Util.retrieveMessage(reasonArray);
            switch (LobsterCraft.servicesManager.playerHandlerService.kickPlayer(
                    offlinePlayer,
                    PlayerHandlerService.BanType.PLAYER_PERMANENTLY_BANNED,
                    reason,
                    moderator != null ?
                            moderator.getOfflinePlayer().getPlayerId() : null,
                    null
            )) {
                case SUCCESSFULLY_EXECUTED:
                    commandSender.sendMessage(Util.appendStrings("§6Jogador §c", offlinePlayer.getPlayerName(), " §6foi banido permanentemente."));
                    break;
                case INVALID_REASON_LENGTH:
                    commandSender.sendMessage("§cMotivo inválido: texto muito curto ou muito longo");
                    break;
                case PLAYER_NOT_REGISTERED:
                    commandSender.sendMessage("§cJogador não registrado.");
                    break;
                case ERROR_OCCURRED:
                    commandSender.sendMessage("§4Ocorreu um erro!");
                    break;
            }
        });
        return HandleResponse.RETURN_TRUE;
    }
}
