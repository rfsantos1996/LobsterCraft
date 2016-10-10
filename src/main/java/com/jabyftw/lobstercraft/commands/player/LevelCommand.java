package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.util.Util;
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
        super("level", Permissions.PLAYER_LEVEL_CHANGE.toString(), "Permite ao jogador mudar seu level", "/level (quantidade)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onLevel(OnlinePlayer onlinePlayer, int amount) {
        if (amount <= 0) {
            onlinePlayer.getPlayer().sendMessage("§cA quantidade deve ser maior que zero.");
            return true;
        }

        // Store values, update level and warn players
        int level = onlinePlayer.getPlayer().getLevel();
        onlinePlayer.getPlayer().setLevel(amount);
        onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§6Você tem agora §c", amount, "§6 leveis (§c", level, " §6-> §c", onlinePlayer.getPlayer().getLevel(), "§6)"));
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_LEVEL_CHANGE_OTHERS)
    private boolean onExpOthers(CommandSender commandSender, OnlinePlayer onlinePlayer, int amount) {
        if (amount <= 0) {
            commandSender.sendMessage("§cA quantidade deve ser maior que zero.");
            return true;
        }
        Player player = onlinePlayer.getPlayer();

        // Store value and update level
        int level = player.getLevel();
        player.setLevel(level + amount);

        // Warn players
        onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§c", commandSender.getName(), "§6 mudou seus leveis para §c", amount, "§6 (§c", level, " §6-> §c",
                player.getLevel(), "§6)"));
        commandSender.sendMessage(Util.appendStrings(player.getDisplayName(), "§6 tem agora §c", amount, "§6 leveis (§c", level, " §6-> §c", player.getLevel(), "§6)"));
        return true;
    }
}
