package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.ConditionController;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
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
public class EnchantCommand extends CommandExecutor {

    public EnchantCommand() {
        super("enchant", Permissions.PLAYER_ENCHANTMENT, "Permite ao jogador encantar um item", "/enchant (tipo de encantamento) (level) (seguro - true/false)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onEnchantmentList(CommandSender commandSender) {
        StringBuilder stringBuilder = new StringBuilder();

        // Add a header
        stringBuilder.append("§6Encantamentos: ");

        // Iterate through enchantments
        for (Enchantment enchantment : Enchantment.values()) {
            stringBuilder.append("§6").append(enchantment.getName().toLowerCase()).append(" §r(§c").append(enchantment.getMaxLevel()).append("§r), ");
        }

        // Send to player
        commandSender.sendMessage(stringBuilder.toString());
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onEnchantment(PlayerHandler playerHandler, Enchantment enchantment) {
        return onEnchantment(playerHandler, enchantment, enchantment.getMaxLevel(), false);
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onEnchantment(PlayerHandler playerHandler, Enchantment enchantment, int level) {
        return onEnchantment(playerHandler, enchantment, level, false);
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onEnchantment(PlayerHandler playerHandler, Enchantment enchantment, int level, boolean unsafeEnchantment) {
        Player player = playerHandler.getPlayer();
        ItemStack itemInHand = player.getItemInHand();

        // Check item (there must be one)
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            playerHandler.sendMessage("§cVocê não tem item nas mãos!");
            return true;
        }

        // Check amount (must be a lonely item if not unsafe)
        if (itemInHand.getAmount() > 1 && !unsafeEnchantment) {
            playerHandler.sendMessage("§cVocê deve ter apenas UM item na mão!");
            return true;
        }

        // Some widely used variables
        boolean canEnchantItem = enchantment.canEnchantItem(itemInHand);
        String playerName = player.getName().toLowerCase();

        // Check if its not possible to enchant it AND (player don't want unsafe OR player doesn't have permission to do so)
        if (!canEnchantItem && (!unsafeEnchantment || !LobsterCraft.permission.has(player, Permissions.PLAYER_ENCHANTMENT_UNSAFE))) {
            playerHandler.sendMessage("§cO item em suas mãos não é válido para este encantamento!");
            return true;
        }

        // Ask player's confirmation if enchantment need to be unsafe
        if (unsafeEnchantment && !canEnchantItem && playerHandler.getConditionController().insertConditionIfReady(ConditionController.Condition.PROTECTION_CHECK)) {
            playerHandler.sendMessage("§4AVISO:§c se pretende encantar um item no§l modo inseguro§r§c, digite o comando novamente.");
            return true;
        }

        // Enchant item
        if (unsafeEnchantment && !canEnchantItem)
            itemInHand.addUnsafeEnchantment(enchantment, level);
        else
            itemInHand.addEnchantment(enchantment, level);

        // Send response to player
        playerHandler.sendMessage("§6Encantamento realizado: §c" + enchantment.getName().toLowerCase().replaceAll("_", " ") + "§6 level §c" + level + (unsafeEnchantment ? " §6(§4INSEGURO§6)" : ""));
        return true;
    }
}
