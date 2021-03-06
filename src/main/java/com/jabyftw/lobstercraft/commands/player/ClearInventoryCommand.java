package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.TriggerController;
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
public class ClearInventoryCommand extends CommandExecutor {

    public ClearInventoryCommand() {
        super("clear", Permissions.PLAYER_CLEAR_INVENTORY.toString(), "Permite ao jogador se livrar de itens", "/clear");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onClear(OnlinePlayer onlinePlayer) {

        if (onlinePlayer.getTriggerController().sendMessageIfTriggered(
                TriggerController.TemporaryTrigger.PLAYER_CLEAR_INVENTORY_CHECK,
                "§cVocê tem certeza de que quer limpar §4TODO §co seu inventário? §6Se sim, use o comando novamente."
        )) return true;

        onlinePlayer.getPlayer().getInventory().clear();
        onlinePlayer.getPlayer().sendMessage("§6Inventário limpo!");
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_CLEAR_INVENTORY_OTHERS)
    private boolean onClearOther(CommandSender commandSender, OnlinePlayer onlinePlayer) {
        onlinePlayer.getPlayer().getInventory().clear();
        onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§6Inventário limpo por ", commandSender.getName()));
        commandSender.sendMessage(Util.appendStrings("§6Inventário de ", onlinePlayer.getPlayer().getDisplayName(), "§6 foi limpo."));
        return true;
    }
}
