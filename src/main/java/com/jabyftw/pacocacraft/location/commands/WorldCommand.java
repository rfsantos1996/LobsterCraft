package com.jabyftw.pacocacraft.location.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.location.TeleportBuilder;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

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
public class WorldCommand extends CommandExecutor {

    public WorldCommand() {
        super(PacocaCraft.pacocaCraft, "world", Permissions.TELEPORT_WORLD, "§6Permite ao jogador mudar de mundo", "§c/world (§4nome§c)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onWorldList(CommandSender commandSender) {
        List<World> worlds = Bukkit.getWorlds();
        String[] worldNames = new String[worlds.size()];

        // Get world
        for(int i = 0; i < worlds.size(); i++) {
            worldNames[i] = "§c" + worlds.get(i).getName() + "§r";
        }

        commandSender.sendMessage("§6Mundos: " + Arrays.asList(worldNames));
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onWorldTeleport(PlayerHandler playerHandler, World world) {
        TeleportBuilder.getBuilder(playerHandler).setLocation(world.getSpawnLocation()).registerLastLocation(true).warnTeleportingPlayer(true).waitBeforeListenerTriggers(true).execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.TELEPORT_WORLD_OTHERS)
    public boolean onWorldTeleportOther(CommandSender commandSender, PlayerHandler playerHandler, World world) {
        TeleportBuilder.getBuilder(playerHandler).setLocation(world.getSpawnLocation()).registerLastLocation(true).setInstantaneousTeleport(true).execute();
        commandSender.sendMessage(playerHandler.getPlayer().getDisplayName() + "§6 foi teleportado para §c" + world.getName());
        return true;
    }
}
