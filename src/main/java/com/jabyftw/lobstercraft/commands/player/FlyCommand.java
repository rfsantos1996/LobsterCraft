package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.util.Permissions;
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
        super("fly", Permissions.PLAYER_FLY.toString(), "Permite ao jogador a permissão de voar", "/fly");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onFly(OnlinePlayer onlinePlayer, boolean fly) {
        onlinePlayer.getPlayer().setAllowFlight(fly);
        onlinePlayer.getPlayer().sendMessage(fly ? "§6Vôo ativado." : "§cVôo desativado.");
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onFly(OnlinePlayer onlinePlayer) {
        return onFly(onlinePlayer, !onlinePlayer.getPlayer().getAllowFlight());
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_FLY_OTHERS)
    private boolean onFlyOthers(CommandSender commandSender, OnlinePlayer target, boolean fly) {
        onFly(target, fly);
        commandSender.sendMessage((fly ? "§6" : "§c") + target.getPlayer().getDisplayName() + (fly ? "§6 está voando" : "§c não está voando."));
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_FLY_OTHERS)
    private boolean onFlyOthers(CommandSender commandSender, OnlinePlayer target) {
        return onFlyOthers(commandSender, target, !target.getPlayer().getAllowFlight());
    }
}
