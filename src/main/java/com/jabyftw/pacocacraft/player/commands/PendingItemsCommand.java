package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

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
public class PendingItemsCommand extends CommandExecutor {

    public PendingItemsCommand() {
        super(PacocaCraft.pacocaCraft, "itens", Permissions.PLAYER_PENDING_ITEMS, "§6Permite ao jogador receber os itens pendentes", "§c/itens");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onPendingItems(PlayerHandler playerHandler) {
        Iterator<ItemStack> iterator = playerHandler.getPendingItems().iterator();

        // Iterate through items
        while(iterator.hasNext()) {
            ItemStack next = iterator.next();

            // Add items until it can't handle anymore
            if(playerHandler.addItem(false, next))
                iterator.remove();
            else
                break;
        }
        playerHandler.getPlayer().sendMessage(playerHandler.getPendingItems().isEmpty() ? "§6Todos os itens foram entregues!" : "§cAinda restam itens a serem entregues!");
        return true;
    }
}
