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
public class HealCommand extends CommandExecutor {

    public HealCommand() {
        super("heal", Permissions.PLAYER_HEAL.toString(), "Permite ao jogador recuperar sua vida", "/heal");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onHeal(OnlinePlayer onlinePlayer) {
        Player player = onlinePlayer.getPlayer();
        player.setHealth(player.getMaxHealth());
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_HEAL_OTHERS)
    public boolean onHealOthers(CommandSender commandSender, OnlinePlayer onlinePlayer) {
        onHeal(onlinePlayer);
        commandSender.sendMessage(Util.appendStrings(onlinePlayer.getPlayer().getDisplayName(), "§6 foi curado."));
        onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§c", commandSender.getName(), "§6 te curou."));
        return true;
    }
}
