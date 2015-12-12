package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
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
public class LevelCommand extends CommandExecutor {

    public LevelCommand() {
        super(PacocaCraft.pacocaCraft, "level", Permissions.PLAYER_SET_LEVEL, "§6Permite ao jogador mudar seu level", "§c/level (§4quantidade§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onLevel(PlayerHandler playerHandler, int amount) {
        if(amount <= 0) {
            playerHandler.getPlayer().sendMessage("§cA quantidade deve ser maior que zero.");
            return true;
        }

        // Store values, update level and warn players
        int level = playerHandler.getPlayer().getLevel();
        playerHandler.getPlayer().setLevel(amount);
        playerHandler.getPlayer().sendMessage("§6Você tem agora §c" + amount + "§6 leveis (§c" + level + " §6-> §c" + playerHandler.getPlayer().getLevel() + ")");
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_SET_LEVEL_OTHERS)
    public boolean onExpOthers(CommandSender commandSender, PlayerHandler playerHandler, int amount) {
        if(amount <= 0) {
            commandSender.sendMessage("§cA quantidade deve ser maior que zero.");
            return true;
        }
        Player player = playerHandler.getPlayer();

        // Store value and update level
        int level = player.getLevel();
        player.setLevel(level + amount);

        // Warn players
        player.sendMessage("§c" + commandSender.getName() + "§6 mudou seus leveis para §c" + amount + "§6 (§c" + level + " §6-> §c" + player.getLevel() + ")");
        commandSender.sendMessage(player.getDisplayName() + "§6 tem agora §c" + amount + "§6 leveis (§c" + level + " §6-> §c" + player.getLevel() + ")");
        return true;
    }
}