package com.jabyftw.pacocacraft.player.chat.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.util.Permissions;
import com.jabyftw.profile_util.PlayerHandler;
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
public class MuteCommand extends CommandExecutor {

    public MuteCommand() {
        super(PacocaCraft.pacocaCraft, "mute", Permissions.PLAYER_MUTE, "§6Permite ao jogador silenciar alguém que não gosta", "§c/mute (§4jogador§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onMute(PlayerHandler playerHandler) {
        return false; // Return a list of muted
    }


    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onMute(PlayerHandler playerHandler, PlayerHandler muted) {
        return false; // mute player
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_MUTE_ALL)
    public boolean onAdminMute(CommandSender commandSender, PlayerHandler muted) {
        return false; // prepare admin mute base
    }
}
