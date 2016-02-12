package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
public class ExpCommand extends CommandExecutor {

    public ExpCommand() {
        super("exp", Permissions.PLAYER_EXP, "Permite ao jogador adquirir leveis", "/exp (leveis)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onExp(PlayerHandler playerHandler, int amount) {
        if (amount <= 0) {
            playerHandler.sendMessage("§cA quantidade deve ser maior que zero.");
            return true;
        }

        // Store values, update level and warn players
        int level = playerHandler.getPlayer().getLevel();
        playerHandler.getPlayer().setLevel(level + amount);
        playerHandler.sendMessage("§6Você ganhou §c" + amount + "§6 leveis (§c" + level + " §6-> §c" + playerHandler.getPlayer().getLevel() + ")");
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_EXP_OTHERS)
    public boolean onExpOthers(CommandSender commandSender, PlayerHandler playerHandler, int amount) {
        if (amount <= 0) {
            commandSender.sendMessage("§cA quantidade deve ser maior que zero.");
            return true;
        }
        Player player = playerHandler.getPlayer();

        // Store values and update level
        int level = player.getLevel();
        player.setLevel(level + amount);

        // Warn players
        playerHandler.sendMessage("§6Você ganhou §c" + amount + "§6 leveis de §c" + commandSender.getName() + " §6(§c" + level + " §6-> §c" + player.getLevel() + ")");
        commandSender.sendMessage(player.getDisplayName() + "§6 ganhou §c" + amount + "§6 leveis (§c" + level + " §6-> §c" + player.getLevel() + ")");

        return true;
    }
}
