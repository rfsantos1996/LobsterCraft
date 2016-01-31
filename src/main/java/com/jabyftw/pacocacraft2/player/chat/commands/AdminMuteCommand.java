package com.jabyftw.pacocacraft2.player.chat.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft2.PacocaCraft;
import com.jabyftw.pacocacraft2.login.UserProfile;
import com.jabyftw.pacocacraft2.player.chat.ChatService;
import com.jabyftw.pacocacraft2.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft2.util.Permissions;
import com.jabyftw.pacocacraft2.util.Util;
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
public class AdminMuteCommand extends CommandExecutor {

    public AdminMuteCommand() {
        super(PacocaCraft.pacocaCraft, "adminmute", Permissions.PLAYER_MUTE_ADMIN, "", "§c/amute (§4jogador§c)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onAdminMute(final CommandSender commandSender, final PlayerHandler muted, final String reason) {
        return onAdminMute(commandSender, muted, -1L, reason);
    }

    @CommandHandler(senderType = SenderType.BOTH) // This Long is parsed timeDifference
    public boolean onAdminMute(final CommandSender commandSender, final PlayerHandler muted, final Long unbanDate, final String... reasonArray) {
        long playerId = muted.getProfile(UserProfile.class).getPlayerId();
        ModeratorMuteEntry muteEntry = ChatService.mutedPlayersStorage.get(playerId);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < reasonArray.length; i++) {
            stringBuilder.append(reasonArray[i]);
            if (i != reasonArray.length - 1) stringBuilder.append(' ');
        }
        String reason = stringBuilder.toString();

        if (!Util.checkStringLength(reason, 4, 128)) {
            Util.sendCommandSenderMessage(commandSender, "§cMotivo está muito longo ou muito curto.");
            return true;
        }

        // Handle existing mute entry
        if (muteEntry != null) {
            // Check if the player was already muted
            if (ChatService.pendingMutePlayers.contains(playerId) || ChatService.mutedPlayers.contains(playerId)) {
                Util.sendCommandSenderMessage(commandSender, "§cJogador já silenciado.");
                return true;
            } else {
                // Remove the pending unmute
                ChatService.pendingUnmutePlayers.remove(playerId);

                // Restore state (removing mute to be re-added)
                ChatService.mutedPlayersStorage.remove(playerId);
            }
        }

        // Create a new mute entry
        muteEntry = new ModeratorMuteEntry(
                playerId,
                commandSender instanceof Player ? PacocaCraft.getPlayerHandler((Player) commandSender).getProfile(UserProfile.class).getPlayerId() : -1, // Moderator id
                reason,
                unbanDate > 0 ? unbanDate + System.currentTimeMillis() : -1
        );

        // Add mute entry to record
        ChatService.pendingMutePlayers.add(playerId);
        ChatService.mutedPlayersStorage.put(playerId, muteEntry);

        // Warn sender and muted player
        Util.sendCommandSenderMessage(commandSender, muted.getPlayer().getDisplayName() + "§6 foi silenciado.");
        Util.sendPlayerMessage(muted, "§cVocê foi silenciado por §4" + commandSender.getName() + "§c: \"" + reason + "\"");

        return true;
    }
}
