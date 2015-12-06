package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

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
public class GodModeCommand extends CommandExecutor {

    public GodModeCommand() {
        super(PacocaCraft.pacocaCraft, "godmode", Permissions.PLAYER_GOD_MODE, "§6Faz os jogadores ficarem imortais", "§c/godmode");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public HandleResponse onGodMode(PlayerHandler playerHandler) {
        return onGodMode(playerHandler, !playerHandler.isGodMode()); // toggle
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public HandleResponse onGodMode(PlayerHandler playerHandler, boolean godMode) {
        playerHandler.setGodMode(godMode, null);
        return HandleResponse.RETURN_TRUE;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermission = Permissions.PLAYER_GOD_MODE_OTHERS)
    public HandleResponse onGodModeByOther(CommandSender commandSender, PlayerHandler targetPlayer) {
        return onGodModeByOther(commandSender, targetPlayer, !targetPlayer.isGodMode()); // toggle
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermission = Permissions.PLAYER_GOD_MODE_OTHERS)
    public HandleResponse onGodModeByOther(CommandSender commandSender, PlayerHandler targetPlayer, boolean godMode) {
        if(targetPlayer.setGodMode(!targetPlayer.isGodMode(), commandSender))
            commandSender.sendMessage("§6Jogador " + targetPlayer.getPlayer().getName() + " está em modo deus (god mode).");
        else
            commandSender.sendMessage("§cJogador " + targetPlayer.getPlayer().getName() + " saiu do modo deus (god mode).");
        return HandleResponse.RETURN_TRUE;
    }
}