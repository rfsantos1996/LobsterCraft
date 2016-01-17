package com.jabyftw.pacocacraft.player.chat.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.pacocacraft.player.chat.ChatProfile;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.Permissions;
import com.jabyftw.profile_util.PlayerHandler;

import java.sql.SQLException;
import java.util.List;

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
public class MuteCommand extends CommandExecutor {

    public MuteCommand() {
        super(PacocaCraft.pacocaCraft, "mute", Permissions.PLAYER_MUTE, "§6Permite ao jogador silenciar alguém que não gosta", "§c/mute (§4jogador§c)");
    }

    @SuppressWarnings("deprecation")
    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onMute(final PlayerHandler playerHandler) {
        BukkitScheduler.runTaskAsynchronously(() -> {
            List<String> mutedPlayers = null;

            // Try to fetch
            try {
                mutedPlayers = playerHandler.getProfile(ChatProfile.class).getMutedPlayers();
            } catch(SQLException e) {
                e.printStackTrace();
                Util.sendPlayerMessage(playerHandler, "§4Falha ao procurar jogadores silenciados.");
                return;
            }

            // Build string builder
            StringBuilder stringBuilder = new StringBuilder("§6Jogadores silenciados: ");

            // Iterate through all names
            for(int i = 0; i < mutedPlayers.size(); i++) {
                // Append a red player name
                stringBuilder.append("§c").append(mutedPlayers.get(i));
                // If not the last, append a default yellow comma
                if(i != mutedPlayers.size() - 1) stringBuilder.append("§6, ");
            }

            // Finish string builder and send player message
            Util.sendPlayerMessage(playerHandler, stringBuilder.toString());
        });
        return true;
    }


    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onMute(final PlayerHandler playerHandler, final PlayerHandler muted) {
        String messageSent = muted.getPlayer().getDisplayName();

        // Switch for each mute responses; if none handled, throw exception
        switch(playerHandler.getProfile(ChatProfile.class).mutePlayer(playerHandler.getProfile(UserProfile.class).getPlayerId())) {
            case ALREADY_MUTED:
                messageSent += "§c já está silenciado.";
                break;
            case FULL_MUTE_LIST:
                messageSent = "§4A lista de silenciados está cheia.";
                break;
            case SUCCESSFULLY_MUTED:
                messageSent += "§6 foi silenciado.";
                break;
            default:
                throw new IllegalStateException("Failed to mute player (default response @ MuteCommand#onmute)");
        }

        // Send message to sender
        Util.sendPlayerMessage(playerHandler, messageSent);

        return true;
    }
}
