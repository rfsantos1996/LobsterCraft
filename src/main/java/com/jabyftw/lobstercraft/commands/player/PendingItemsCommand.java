package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.InventoryProfile;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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
        super("itens", Permissions.PLAYER_PENDING_ITEMS.toString(), "Permite ao jogador receber os itens pendentes", "/itens");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onPendingItems(OnlinePlayer onlinePlayer) {
        InventoryProfile inventoryProfile = onlinePlayer.getProfile(InventoryProfile.class);

        // Copy the list
        ArrayList<ItemStack> itemStacks = new ArrayList<>(inventoryProfile.getRemainingItems());
        inventoryProfile.getRemainingItems().clear();

        // Iterate through all items
        Iterator<ItemStack> iterator = itemStacks.iterator();

        while (iterator.hasNext()) {
            ItemStack next = iterator.next();
            // Remove if here because loop might break
            iterator.remove();

            // Break
            if (!inventoryProfile.addItems(false, next)) break;
        }

        // If there are remaining items, it should be back here
        inventoryProfile.getRemainingItems().addAll(itemStacks);

        onlinePlayer.getPlayer().sendMessage(inventoryProfile.getRemainingItems().isEmpty() ? "§6Todos os itens foram entregues!" :
                "§cAinda restam itens a serem entregues!");
        return true;
    }
}
