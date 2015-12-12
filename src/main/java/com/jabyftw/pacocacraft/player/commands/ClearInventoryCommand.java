package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

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

    private final static long CLEAR_INVENTORY_CONFIRMATION_TICKS = PacocaCraft.config.getLong(ConfigValue.PLAYER_TIME_CLEAR_INVENTORY_CONFIRMATION.getPath()) * 20L; // Seconds * 20 -> ticks
    private final static ArrayList<String> usedCommand = new ArrayList<>();

    public ClearInventoryCommand() {
        super(PacocaCraft.pacocaCraft, "clear", Permissions.PLAYER_CLEAR_INVENTORY, "§6Permite ao jogador se livrar de itens", "§c/clear");

    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onClear(PlayerHandler playerHandler) {
        String playerName = playerHandler.getPlayer().getName().toLowerCase();

        if(usedCommand.contains(playerName)) {
            playerHandler.getPlayer().getInventory().clear();
            playerHandler.getPlayer().sendMessage("§6Inventário limpo!");
            usedCommand.remove(playerName);
        } else {
            playerHandler.getPlayer().sendMessage("§cVocê tem certeza que quer limpar seu inventário? §6Se sim, use o comando novamente.");
            usedCommand.add(playerName);
            BukkitScheduler.runTaskLater(PacocaCraft.pacocaCraft, () -> usedCommand.remove(playerName), CLEAR_INVENTORY_CONFIRMATION_TICKS);
        }
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_CLEAR_INVENTORY_OTHERS)
    public boolean onClearOther(CommandSender commandSender, PlayerHandler playerHandler) {
        String commandSenderName = commandSender.getName().toLowerCase();

        if(usedCommand.contains(commandSenderName)) {
            playerHandler.getPlayer().getInventory().clear();
            playerHandler.getPlayer().sendMessage("§6Inventário limpo por " + commandSender.getName() + "!");
            commandSender.sendMessage("§6Inventário de " + playerHandler.getPlayer().getDisplayName() + "§6 foi limpo.");
            usedCommand.remove(commandSenderName);
        } else {
            commandSender.sendMessage("§cVocê tem certeza que quer limpar o inventário de " + playerHandler.getPlayer().getDisplayName() + "§c? §6Se sim, use o comando novamente.");
            usedCommand.add(commandSenderName);
            BukkitScheduler.runTaskLater(PacocaCraft.pacocaCraft, () -> usedCommand.remove(commandSenderName), CLEAR_INVENTORY_CONFIRMATION_TICKS);
        }
        return true;
    }
}
