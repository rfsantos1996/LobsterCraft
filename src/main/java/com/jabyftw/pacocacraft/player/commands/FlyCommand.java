package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
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
public class FlyCommand extends CommandExecutor {

    public FlyCommand() {
        super(PacocaCraft.pacocaCraft, "fly", Permissions.PLAYER_FLY, "§6Permite ao jogador a permissão de voar", "§c/fly");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public HandleResponse onFly(PlayerHandler playerHandler, boolean fly) {
        playerHandler.getPlayer().setAllowFlight(fly);
        playerHandler.getPlayer().sendMessage(fly ? "§6Vôo ativado." : "§cVôo desativado.");
        return HandleResponse.RETURN_TRUE;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public HandleResponse onFly(PlayerHandler playerHandler) {
        return onFly(playerHandler, !playerHandler.getPlayer().getAllowFlight());
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermission = Permissions.PLAYER_FLY_OTHERS)
    public HandleResponse onFlyOthers(CommandSender commandSender, PlayerHandler target, boolean fly) {
        onFly(target, fly);
        commandSender.sendMessage((fly ? "§6" : "§c") + target.getPlayer().getDisplayName() + (fly ? "§6 está voando" : "§c não está voando."));
        return HandleResponse.RETURN_TRUE;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermission = Permissions.PLAYER_FLY_OTHERS)
    public HandleResponse onFlyOthers(CommandSender commandSender, PlayerHandler target) {
        return onFlyOthers(commandSender, target, !target.getPlayer().getAllowFlight());
    }
}
