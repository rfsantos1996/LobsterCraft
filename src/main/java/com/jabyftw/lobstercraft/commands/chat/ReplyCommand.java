package com.jabyftw.lobstercraft.commands.chat;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.player.ChatProfile;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.util.Util;
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
        super("reply", Permissions.CHAT_WHISPER.toString(), "Permite ao jogador responder mensagens privadas rapidamente", "/r (mensagem)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onPlayerReply(OnlinePlayer onlinePlayer, String... strings) {
        CommandSender lastPrivateSender = onlinePlayer.getProfile(ChatProfile.class).getLastWhisper();

        // Check if there is someone online to reply
        if (lastPrivateSender == null || (lastPrivateSender instanceof Player && !((Player) lastPrivateSender).isOnline())) {
            onlinePlayer.getProfile(ChatProfile.class).setLastWhisper(null);
            onlinePlayer.getPlayer().sendMessage("§cNão há jogador para responder.");
            return true;
        }

        // Send message
        String message = Util.retrieveMessage(strings);

        lastPrivateSender.sendMessage(WhisperCommand.WHISPER_RECEIVER_FORMAT.replaceAll("%message%", message).replaceAll("%playerName%", onlinePlayer.getPlayer().getDisplayName()));
        onlinePlayer.getPlayer().sendMessage(WhisperCommand.WHISPER_SENDER_FORMAT.replaceAll("%message%", message)
                .replaceAll("%playerName%", lastPrivateSender instanceof Player ? ((Player) lastPrivateSender).getDisplayName() : lastPrivateSender.getName()));

        // Check if last whisper is from console
        if (!(lastPrivateSender instanceof Player))
            LobsterCraft.servicesManager.commandService.whisperCommand.consoleLastWhisper = onlinePlayer.getPlayer();
        return true;
    }

    @CommandHandler(senderType = SenderType.CONSOLE)
    private boolean onPlayerReply(CommandSender sender, String... strings) {
        Player lastWhisper = LobsterCraft.servicesManager.commandService.whisperCommand.consoleLastWhisper;
        if (lastWhisper == null ||
                (lastWhisper.isOnline() && LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(lastWhisper, OnlinePlayer.OnlineState.LOGGED_IN) != null)) {
            sender.sendMessage("§6Jogador não está online");
            LobsterCraft.servicesManager.commandService.whisperCommand.consoleLastWhisper = null;
            return true;
        }

        String message = Util.retrieveMessage(strings);

        lastWhisper.sendMessage(WhisperCommand.WHISPER_RECEIVER_FORMAT.replaceAll("%message%", message).replaceAll("%playerName%", sender.getName()));
        sender.sendMessage(WhisperCommand.WHISPER_SENDER_FORMAT.replaceAll("%message%", message).replaceAll("%playerName%", lastWhisper.getPlayer().getDisplayName()));
        return true;
    }
}
