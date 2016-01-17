package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.Util;
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
public class HatCommand extends CommandExecutor {

    public HatCommand() {
        super(PacocaCraft.pacocaCraft, "hat", Permissions.PLAYER_HAT, "§6Permite ao jogador ficar bonito", "§c/hat");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onHat(PlayerHandler playerHandler) {
        Player player = playerHandler.getPlayer();

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack itemInHand = player.getItemInHand();

        // Check if item in hand isn't air; if is, return
        if(itemInHand == null || itemInHand.getType() == Material.AIR) {
            Util.sendPlayerMessage(playerHandler, "§cVocê não tem item na sua mão!");
            return true;
        }

        // Check if helmet exists and store it
        if(helmet != null && helmet.getType() != Material.AIR)
            playerHandler.addItem(true, helmet);

        // Set helmet as item on hand isn't air
        itemInHand.setAmount(1);
        player.getInventory().remove(itemInHand);
        player.getInventory().setHelmet(itemInHand);
        Util.sendPlayerMessage(playerHandler, "§6Aproveite seu novo chapéu!");
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER, additionalPermissions = Permissions.PLAYER_HAT_OTHERS)
    public boolean onHatOthers(PlayerHandler sender, PlayerHandler target) {
        Player targetPlayer = target.getPlayer();
        Player senderPlayer = sender.getPlayer();

        // We will set SENDER's item in hand as TARGET's hat
        ItemStack helmet = targetPlayer.getInventory().getHelmet();
        ItemStack itemInHand = senderPlayer.getItemInHand();

        // Check if item in hand isn't air; if is, return
        if(itemInHand == null || itemInHand.getType() == Material.AIR) {
            Util.sendPlayerMessage(sender, "§cVocê não tem item na sua mão!");
            return true;
        }

        // Check if helmet exists and store it
        if(helmet != null && helmet.getType() != Material.AIR)
            target.addItem(true, helmet);

        // Set helmet as it isn't air
        itemInHand.setAmount(1);
        senderPlayer.getInventory().remove(itemInHand);
        targetPlayer.getInventory().setHelmet(itemInHand);
        Util.sendPlayerMessage(target, "§6Aproveite seu novo chapéu dado por " + senderPlayer.getDisplayName() + "§6!");
        Util.sendPlayerMessage(sender, "§6O novo chapéu de " + targetPlayer.getDisplayName() + "§6 é um §c" + itemInHand.getType().name().toLowerCase().replaceAll("_", " "));
        return true;
    }
}
