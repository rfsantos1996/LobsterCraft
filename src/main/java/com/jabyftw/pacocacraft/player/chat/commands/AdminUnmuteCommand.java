package com.jabyftw.pacocacraft.player.chat.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.pacocacraft.player.chat.ChatService;
import com.jabyftw.pacocacraft.player.chat.ModeratorMuteEntry;
import com.jabyftw.pacocacraft.util.Permissions;
import com.jabyftw.profile_util.PlayerHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
public class AdminUnmuteCommand extends CommandExecutor {

    public AdminUnmuteCommand() {
        super(PacocaCraft.pacocaCraft, "adminunmute", Permissions.PLAYER_MUTE_ADMIN, "", "§c/aunmute (§4jogador§c)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onAdminUnmute(final CommandSender commandSender, final PlayerHandler unmuted) {
        long playerId = unmuted.getProfile(UserProfile.class).getPlayerId();
        ModeratorMuteEntry muteEntry = ChatService.mutedPlayersStorage.get(playerId);

        // Handle un-existing mute entry
        if(muteEntry == null || ChatService.pendingUnmutePlayers.contains(playerId)) {
            Util.sendCommandSenderMessage(commandSender, "§cJogador não está silenciado.");
            return true;
        }

        // Remove the player and insert it on the pending unmute list
        ChatService.pendingUnmutePlayers.add(playerId);
        Util.sendCommandSenderMessage(commandSender, unmuted.getPlayer().getDisplayName() + "§6 foi removido da lista de silenciados.");
        Util.sendPlayerMessage(unmuted, "§6Você foi tirado da lista de silenciados por §c" + commandSender.getName() + "§6.");

        return true;
    }
}
