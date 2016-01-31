package com.jabyftw.pacocacraft2.player.chat.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft2.PacocaCraft;
import com.jabyftw.pacocacraft2.login.UserProfile;
import com.jabyftw.pacocacraft2.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft2.util.Permissions;
import com.jabyftw.pacocacraft2.util.Util;

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
public class UnmuteCommand extends CommandExecutor {

    public UnmuteCommand() {
        super(PacocaCraft.pacocaCraft, "unmute", Permissions.PLAYER_MUTE, "§6Permite ao jogador tirar o mute de algum jogador", "§c/unmute (§4jogador§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onUnmute(PlayerHandler playerHandler, PlayerHandler unmuted) {
        String messageSent = unmuted.getPlayer().getDisplayName();

        // Switch between responses to player unmute; if none handled, throw exception
        switch (playerHandler.getProfile(ChatProfile.class).unmutePlayer(unmuted.getProfile(UserProfile.class).getPlayerId())) {
            case ALREADY_UNMUTED:
                messageSent += "§c já deixou de ser silenciado.";
                break;
            case SUCCESSFULLY_UNMUTED:
                messageSent += "§6 deixou de ser silenciado";
                break;
            case NEVER_WAS_MUTED:
                messageSent += "§c nunca foi silenciado.";
                break;
            default:
                throw new IllegalStateException("Failed to unmute player (default response @ UnmuteCommand#onUnmute)");
        }

        // Send response to player
        Util.sendPlayerMessage(playerHandler, messageSent);

        return true;
    }
}
