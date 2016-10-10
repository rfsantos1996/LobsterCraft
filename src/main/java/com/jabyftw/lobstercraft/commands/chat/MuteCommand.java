package com.jabyftw.lobstercraft.commands.chat;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.player.ChatProfile;
import com.jabyftw.lobstercraft.player.OfflinePlayer;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.util.Util;

import java.util.Set;

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
public class MuteCommand extends CommandExecutor {

    public MuteCommand() {
        super("mute", Permissions.CHAT_MUTE.toString(), "Permite ao jogador silenciar outros jogadores", "/mute (jogador)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onMuteList(OnlinePlayer onlinePlayer) {
        // This set is a copy, therefore it doesn't need to be synchronized
        Set<Integer> muteEntries = onlinePlayer.getProfile(ChatProfile.class).getMuteEntries();

        if (muteEntries.isEmpty()) {
            onlinePlayer.getPlayer().sendMessage("§6Não há ninguém silenciado. Use §c/mute (jogador)§6 para silenciar");
            return true;
        }

        // Start message build
        StringBuilder messageBuilder = new StringBuilder("§6Silenciados: ");
        boolean first = true;

        // Iterate through entries
        for (Integer mutedId : muteEntries) {
            if (!first) messageBuilder.append("§6, ");
            first = false;
            messageBuilder.append("§c").append(LobsterCraft.servicesManager.playerHandlerService.getOfflinePlayer(mutedId).getPlayerName());
        }

        // Send message to player
        onlinePlayer.getPlayer().sendMessage(messageBuilder.toString());
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private HandleResponse onMute(OnlinePlayer onlinePlayer, OfflinePlayer target) {
        switch (onlinePlayer.getProfile(ChatProfile.class).mutePlayer(target)) {
            case PLAYER_NOT_FOUND:
                onlinePlayer.getPlayer().sendMessage("§cJogador não encontrado.");
                return HandleResponse.RETURN_TRUE;
            case CAN_NOT_MUTE_THIS_PLAYER:
                return HandleResponse.RETURN_NO_PERMISSION;
            case MAXIMUM_AMOUNT_OF_MUTE_ENTRIES:
                onlinePlayer.getPlayer().sendMessage("§cVocê já silenciou muitos jogadores, limite máximo atingido.");
                return HandleResponse.RETURN_TRUE;
            case PLAYER_ALREADY_MUTED:
                onlinePlayer.getPlayer().sendMessage("§cJogador já silenciado.");
                return HandleResponse.RETURN_TRUE;
            case SUCCESSFULLY_MUTED_PLAYER:
                onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§6Você silenciou §c", target.getPlayerName()));
                return HandleResponse.RETURN_TRUE;
        }
        return HandleResponse.RETURN_HELP;
    }
}
