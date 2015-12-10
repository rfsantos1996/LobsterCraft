package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
public class ClearEnchantmentCommand extends CommandExecutor {

    public ClearEnchantmentCommand() {
        super(PacocaCraft.pacocaCraft, "clearenchantment", Permissions.PLAYER_CLEAR_ENCHANTMENT, "§6Permite ao jogador tirar encantamento dos itens", "§c/cenchant");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onClearEnchantment(PlayerHandler playerHandler) {
        Player player = playerHandler.getPlayer();
        ItemStack itemInHand = player.getItemInHand();

        // Check if item exists
        if(itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage("§cNão tem itens na sua mão!");
            return true;
        }

        // Check if item contains any enchantments
        if(itemInHand.getEnchantments().isEmpty()) {
            player.sendMessage("§cO item na sua mão não está encantado!");
            return true;
        }

        // Acquire all enchantments (on a copy because while removing it may change the KeySet and throw exceptions) and remove them
        ArrayList<Enchantment> enchantments = new ArrayList<>(itemInHand.getEnchantments().keySet());
        enchantments.forEach(itemInHand::removeEnchantment);

        player.sendMessage("§6Encantamentos removidos com sucesso!");
        return true;
    }
}
