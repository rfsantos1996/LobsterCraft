package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.PlayerHandler;
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
public class HealCommand extends CommandExecutor {

    public HealCommand() {
        super(PacocaCraft.pacocaCraft, "heal", Permissions.PLAYER_HEAL, "§6Permite ao jogador recuperar sua vida", "§c/heal");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onHealOthers(PlayerHandler playerHandler) {
        Player player = playerHandler.getPlayer();
        player.setHealth(player.getMaxHealth());
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_HEAL_OTHERS)
    public boolean onHealOthers(CommandSender commandSender, PlayerHandler playerHandler) {
        onHealOthers(playerHandler);
        commandSender.sendMessage(playerHandler.getPlayer().getDisplayName() + "§6 foi curado.");
        playerHandler.getPlayer().sendMessage("§c" + commandSender.getName() + "§6 te curou.");
        return true;
    }
}
