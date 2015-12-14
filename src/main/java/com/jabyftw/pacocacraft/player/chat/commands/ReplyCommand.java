package com.jabyftw.pacocacraft.player.chat.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.chat.ChatProfile;
import com.jabyftw.pacocacraft.util.Permissions;
import com.jabyftw.profile_util.PlayerHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Copyright (C) 2015  Rafael Sartori for PacocaCraft Plugin
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
public class ReplyCommand extends CommandExecutor {

    public ReplyCommand() {
        super(PacocaCraft.pacocaCraft, "r", Permissions.PLAYER_WHISPER_REPLY_AUTOMATICALLY, "§6Permite ao jogador responder mensagens privadas rapidamente", "§c/r (§4mensagem§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onPlayerReply(PlayerHandler sender, String... strings) {
        ChatProfile chatProfile = sender.getProfile(ChatProfile.class);
        CommandSender lastPrivateSender = chatProfile.getLastPrivateSender();

        // Check if there is someone online to reply
        if(lastPrivateSender == null || (lastPrivateSender instanceof Player && !((Player) lastPrivateSender).isOnline())) {
            chatProfile.setLastPrivateSender(null);
            sender.getPlayer().sendMessage("§cNão há outro jogador para mandar a resposta.");
            return true;
        }

        String message = WhisperCommand.retrieveMessage(strings);

        // Check if player is a console sender and send message
        if(!(lastPrivateSender instanceof Player)) {
            lastPrivateSender.sendMessage(message);

            // Update lastPrivateSender for Console
            synchronized(WhisperCommand.playerHandlerLock) {
                WhisperCommand.lastPlayerHandler = sender;
            }
        } else {
            PacocaCraft.getPlayerHandler((Player) lastPrivateSender).getProfile(ChatProfile.class).sendPrivateMessage(sender, message);
        }
        return true;
    }

    @CommandHandler(senderType = SenderType.CONSOLE)
    public boolean onPlayerReply(CommandSender sender, String... strings) {
        PlayerHandler lastPlayerHandler;

        // Update lastPrivateSender for Console
        synchronized(WhisperCommand.playerHandlerLock) {
            lastPlayerHandler = WhisperCommand.lastPlayerHandler;
        }

        lastPlayerHandler.getProfile(ChatProfile.class).sendPrivateMessage(sender, WhisperCommand.retrieveMessage(strings));
        return true;
    }
}
