package com.jabyftw.pacocacraft2.player.chat.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft2.PacocaCraft;
import com.jabyftw.pacocacraft2.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft2.util.Permissions;
import com.jabyftw.pacocacraft2.util.Util;
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

    protected static final Object playerHandlerLock = new Object();
    protected static PlayerHandler lastPlayerHandler = null;

    public WhisperCommand() {
        super(PacocaCraft.pacocaCraft, "whisper", Permissions.PLAYER_WHISPER, "§6Permite ao jogador mandar mensagens privadas", "§c/whisper (§4jogador§c) (§4mensagem§c)");
    }

    public static String retrieveMessage(String... strings) {
        StringBuilder stringBuilder = new StringBuilder();

        // Append all words
        for (String string : strings)
            stringBuilder.append(string).append(' ');

        // Return final string
        return stringBuilder.toString();
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean sendPrivateMessage(PlayerHandler playerSender, PlayerHandler receiver, String... strings) {
        String message = retrieveMessage(strings);

        // Send private message to player
        if (!receiver.getProfile(ChatProfile.class).sendPrivateMessage(playerSender, message))
            Util.sendPlayerMessage(playerSender, "§cFalha ao mandar mensagem para " + receiver.getPlayer().getDisplayName());

        return true;
    }

    @CommandHandler(senderType = SenderType.CONSOLE)
    public boolean sendPrivateMessage(CommandSender commandSender, PlayerHandler receiver, String... strings) {
        String message = retrieveMessage(strings);

        // Send private message to player
        if (!receiver.getProfile(ChatProfile.class).sendPrivateMessage(commandSender, message))
            commandSender.sendMessage("§cFalha ao mandar mensagem para " + receiver.getPlayer().getDisplayName());

        synchronized (playerHandlerLock) {
            lastPlayerHandler = receiver;
        }

        return true;
    }
}
