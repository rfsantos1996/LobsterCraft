package com.jabyftw.lobstercraft.commands.chat;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OfflinePlayerHandler;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.chat.ChatProfile;
import com.jabyftw.lobstercraft.player.chat.MuteEntry;
import com.jabyftw.lobstercraft.player.util.Permissions;

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
        super("mute", Permissions.CHAT_MUTE, "Permite ao jogador silenciar outros jogadores", "/mute (jogador)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onMuteList(PlayerHandler playerHandler) {
        // This set is a copy, therefore it doesn't need to be synchronized
        Set<MuteEntry> muteEntries = playerHandler.getProfile(ChatProfile.class).getMuteEntries();

        if (muteEntries.isEmpty()) {
            playerHandler.sendMessage("§6Não há ninguém silenciado. Use §c/mute (jogador)§6 para silenciar");
            return true;
        }

        // Start message build
        StringBuilder messageBuilder = new StringBuilder("§6Silenciados: ");
        boolean first = true;

        // Iterate through entries
        for (MuteEntry muteEntry : muteEntries) {
            if (!first) messageBuilder.append("§6, ");
            first = false;
            messageBuilder.append("§c").append(LobsterCraft.playerHandlerService.getOfflinePlayer(muteEntry.getMutedPlayerId()).getPlayerName());
        }

        // Send message to player
        playerHandler.sendMessage(messageBuilder.toString());
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)  // TODO OfflinePlayerHandler
    public boolean onMute(PlayerHandler playerHandler, OfflinePlayerHandler target) {
        if (!target.isRegistered()) {
            playerHandler.sendMessage("§cJogador não encontrado.");
            return true;
        }

        //noinspection deprecation
        if (LobsterCraft.permission.has(playerHandler.getPlayer().getWorld(), target.getPlayerName(), Permissions.CHAT_MUTE_EXCEPTION)) {
            playerHandler.sendMessage("§cVocê não pode silenciar esta pessoa.");
            return true;
        }

        switch (playerHandler.getProfile(ChatProfile.class).mutePlayer(target.getPlayerId())) {
            case FULL_MUTE_LIST:
                playerHandler.sendMessage("§cVocê já silenciou muitos jogadores. Limite máximo atingido.");
                return true;
            case SUCCESSFULLY_MUTED:
                playerHandler.sendMessage("§6Você silenciou §c" + target.getPlayerName());
                return true;
            case ALREADY_MUTED:
                playerHandler.sendMessage("§cJogador já silenciado.");
                return true;
        }
        return false;
    }
}
