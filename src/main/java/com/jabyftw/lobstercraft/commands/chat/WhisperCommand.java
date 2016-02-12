package com.jabyftw.lobstercraft.commands.chat;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.chat.ChatProfile;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.command.CommandSender;

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
public class WhisperCommand extends CommandExecutor {

    protected static final String
            WHISPER_RECEIVER_FORMAT = "§c%playerName% §6->§c Você§r: %message%",
            WHISPER_SENDER_FORMAT = "§cVocê §6->§c %playerName%§r: %message%";

    protected static volatile PlayerHandler consoleLastWhisper = null;

    public WhisperCommand() {
        super("whisper", Permissions.CHAT_WHISPER, "Permite ao jogador mandar mensagens privadas", "/whisper (jogador) (mensagem)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public HandleResponse sendPrivateMessage(PlayerHandler sender, PlayerHandler receiver, String... strings) {
        // Check if target is logged in or player is muted
        if (!receiver.isLoggedIn() || receiver.getProfile(ChatProfile.class).isPlayerMuted(sender.getPlayerId()))
            return HandleResponse.RETURN_HELP;

        // Send private message to player
        String message = Util.retrieveMessage(strings);
        receiver.sendMessage(WHISPER_RECEIVER_FORMAT.replaceAll("%message%", message).replaceAll("%playerName%", sender.getPlayer().getDisplayName()));
        sender.sendMessage(WHISPER_SENDER_FORMAT.replaceAll("%message%", message).replaceAll("%playerName%", receiver.getPlayer().getDisplayName()));

        return HandleResponse.RETURN_TRUE;
    }

    @CommandHandler(senderType = SenderType.CONSOLE)
    public boolean sendPrivateMessage(CommandSender commandSender, PlayerHandler receiver, String... strings) {
        // Send private message to player
        String message = Util.retrieveMessage(strings);
        receiver.sendMessage(WHISPER_RECEIVER_FORMAT.replaceAll("%message%", message).replaceAll("%playerName%", commandSender.getName()));
        commandSender.sendMessage(WHISPER_SENDER_FORMAT.replaceAll("%message%", message).replaceAll("%playerName%", receiver.getPlayer().getDisplayName()));

        // Update last whisper for easy replies
        consoleLastWhisper = receiver;
        return true;
    }
}
