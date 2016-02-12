package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.inventory.InventoryProfile;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
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
public class GiveCommand extends CommandExecutor {

    public GiveCommand() {
        super("give", Permissions.PLAYER_GIVE, "Permite ao jogador obter itens", "/give (material) (quantidade) (damage)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onGive(PlayerHandler playerHandler, Material material) {
        return onGive(playerHandler, material, 1);
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onGive(PlayerHandler playerHandler, Material material, int amount) {
        return onGive(playerHandler, material, amount, (short) 0);
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onGive(PlayerHandler playerHandler, Material material, int amount, short damage) {
        playerHandler.getProfile(InventoryProfile.class).addItems(true, new ItemStack(material, amount, damage));
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GIVE_OTHERS)
    public boolean onGive(CommandSender commandSender, PlayerHandler playerHandler, Material material) {
        return onGive(commandSender, playerHandler, material, 1, true, (short) 0);
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GIVE_OTHERS)
    public boolean onGive(CommandSender commandSender, PlayerHandler playerHandler, Material material, boolean warnPlayer) {
        return onGive(commandSender, playerHandler, material, 1, warnPlayer, (short) 0);
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GIVE_OTHERS)
    public boolean onGive(CommandSender commandSender, PlayerHandler playerHandler, Material material, int amount, boolean warnPlayer) {
        return onGive(commandSender, playerHandler, material, amount, warnPlayer, (short) 0);
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GIVE_OTHERS)
    public boolean onGive(CommandSender commandSender, PlayerHandler playerHandler, Material material, int amount, boolean warnPlayer, short damage) {
        ItemStack itemStack = new ItemStack(material, amount, damage);
        playerHandler.getProfile(InventoryProfile.class).addItems(warnPlayer, itemStack);

        commandSender.sendMessage(
                "§c" + amount + "§6 de §c" + material.name().toLowerCase().replaceAll("_", " ") + ":" + damage + "§6 entregue a " + playerHandler.getPlayer().getDisplayName()
        );
        return true;
    }
}
