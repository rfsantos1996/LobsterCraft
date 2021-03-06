package com.jabyftw.lobstercraft.commands.chat;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.player.ChatProfile;
import com.jabyftw.lobstercraft.player.OfflinePlayer;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.util.Util;

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
public class UnmuteCommand extends CommandExecutor {

    public UnmuteCommand() {
        super("unmute", Permissions.CHAT_MUTE.toString(), "Permite ao jogador desilenciar algum jogador", "/unmute (jogador)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onUnmute(OnlinePlayer playerHandler, OfflinePlayer target) {
        switch (playerHandler.getProfile(ChatProfile.class).unmutePlayer(target)) {
            case PLAYER_NOT_FOUND:
                playerHandler.getPlayer().sendMessage("§cJogador não encontrado!");
                return true;
            case PLAYER_ALREADY_UNMUTED:
                playerHandler.getPlayer().sendMessage("§cJogador não está silenciado.");
                return true;
            case SUCCESSFULLY_UNMUTED_PLAYER:
                playerHandler.getPlayer().sendMessage(Util.appendStrings("§6Você agora pode ouvir §c", target.getPlayerName()));
                return true;
        }
        return false;
    }
}
