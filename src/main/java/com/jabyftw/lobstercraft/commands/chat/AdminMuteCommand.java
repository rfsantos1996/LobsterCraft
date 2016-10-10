package com.jabyftw.lobstercraft.commands.chat;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.player.OfflinePlayer;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.persistence.Lob;

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
        super("adminmute", Permissions.BAN_PLAYER_MUTE.toString(), "Permite ao jogador silenciar um jogador de todo o servidor", "/adminmute (jogador) (tempo silenciado) (motivo)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    private boolean onAdministratorMute(CommandSender commandSender, OfflinePlayer target, Long timeSilenced, String... reasonArray) {
        Bukkit.getScheduler().runTaskAsynchronously(LobsterCraft.plugin,
                () -> {
                    String reason = Util.retrieveMessage(reasonArray);

                    switch (LobsterCraft.servicesManager.playerHandlerService.mutePlayer(target,
                            commandSender instanceof Player ?
                                    LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) commandSender, OnlinePlayer.OnlineState.LOGGED_IN) : null,
                            reason,
                            timeSilenced
                    )) {
                        case SUCCESSFULLY_EXECUTED:
                            commandSender.sendMessage(Util.appendStrings("§c", target.getPlayerName(), "§6 foi silenciado!"));
                            break;
                        case INVALID_REASON_LENGTH:
                            commandSender.sendMessage("§cRazão muito curta ou muito longa!");
                            break;
                        case PLAYER_NOT_REGISTERED:
                            commandSender.sendMessage("§cJogador não encontrado!");
                            break;
                        case ERROR_OCCURRED:
                            commandSender.sendMessage("§4Ocorreu um erro!");
                            break;
                    }
                }
        );
        return true;
    }
}
