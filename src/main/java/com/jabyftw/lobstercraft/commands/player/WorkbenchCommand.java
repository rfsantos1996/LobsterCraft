package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.Permissions;
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
public class WorkbenchCommand extends CommandExecutor {

    public WorkbenchCommand() {
        super("workbench", Permissions.PLAYER_WORKBENCH.toString(), "Permite ao jogador acessar um workbench", "/workbench");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onWorkbench(OnlinePlayer onlinePlayer) {
        onlinePlayer.getPlayer().closeInventory();
        onlinePlayer.getPlayer().openWorkbench(null, true);
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_WORKBENCH_OTHERS)
    private boolean onWorkbenchOthers(CommandSender commandSender, OnlinePlayer onlinePlayer) {
        onWorkbench(onlinePlayer);
        commandSender.sendMessage(Util.appendStrings("§6Você abriu um workbench para ", onlinePlayer.getPlayer().getDisplayName()));
        onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§6", commandSender.getName(), " abriu um workbench para você"));
        return true;
    }
}
