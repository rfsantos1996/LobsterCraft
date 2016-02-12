package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.PlayerInventory;

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
public class InventorySpyCommand extends CommandExecutor implements Listener {

    private final static ArrayList<Player> watchingInventory = new ArrayList<>();
    // TODO register

    public InventorySpyCommand() {
        super("spyinv", Permissions.PLAYER_INVENTORY_SPY, "Permite ao jogador bisbilhotar o inventário de outros", "/spyinv (jogador)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onInventorySpy(PlayerHandler senderHandler, PlayerHandler targetHandler) {
        PlayerInventory inventory = targetHandler.getPlayer().getInventory();

        // Close current inventory (if any) and open player's inventory
        senderHandler.getPlayer().closeInventory();
        senderHandler.getPlayer().openInventory(inventory);

        // Add player to watching inventories list
        watchingInventory.add(senderHandler.getPlayer());
        return true;
    }

    @EventHandler(ignoreCancelled = false)
    public void onInventoryChange(InventoryInteractEvent event) {
        // If player is watching inventory
        if (event.getWhoClicked() instanceof Player && watchingInventory.contains(event.getWhoClicked())) {

            // If player doesn't have the permission to modify, cancel event; otherwise update inventory for the holder
            if (!LobsterCraft.permission.has(event.getWhoClicked(), Permissions.PLAYER_INVENTORY_SPY_MODIFY)) {
                event.setResult(Event.Result.DENY);
                event.getWhoClicked().sendMessage("§cVocê não tem permissão para alterar o inventário!");
            } else if (event.getInventory().getHolder() instanceof Player) {
                ((Player) event.getInventory().getHolder()).updateInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Remove player that was possibly watching any
        if (event.getPlayer() instanceof Player)
            watchingInventory.remove(event.getPlayer());
    }
}
