package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
public class RepairCommand extends CommandExecutor {

    public RepairCommand() {
        super(PacocaCraft.pacocaCraft, "repair", Permissions.PLAYER_REPAIR, "§6Permite ao jogador reparar seus itens", "§c/repair");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onRepair(PlayerHandler playerHandler) {
        Player player = playerHandler.getPlayer();
        ItemStack itemInHand = player.getItemInHand();

        // Check if item exists
        if(itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage("§cVocê não tem item na sua mão!");
            return true;
        }

        // Check if it is possible to be repaired
        if(itemInHand.getType().isBlock() || itemInHand.getType().getMaxDurability() == 0) {
            player.sendMessage("§cEste item não pode ser reparado!");
        } else {
            itemInHand.setDurability((short) 0);
            player.sendMessage("§6Item reparado! <3");
        }
        return true;
    }
}
