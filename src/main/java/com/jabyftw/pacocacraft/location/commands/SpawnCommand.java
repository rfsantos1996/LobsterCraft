package com.jabyftw.pacocacraft.location.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.location.TeleportBuilder;
import com.jabyftw.pacocacraft.location.TeleportService;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

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
public class SpawnCommand extends CommandExecutor {

    public SpawnCommand() {
        super(PacocaCraft.pacocaCraft, "spawn", Permissions.TELEPORT_SPAWN, "§6Permite ao jogador teleportar para o spawn", "§c/spawn\n§c/spawn (§4mundo§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public HandleResponse onSpawn(PlayerHandler playerHandler) {
        TeleportBuilder.getBuilder(playerHandler).setLocation(playerHandler.getPlayer().getWorld().getSpawnLocation()).registerLastLocation(true).warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true).execute();
        return HandleResponse.RETURN_TRUE;
    }

    @CommandHandler(senderType = SenderType.PLAYER, additionalPermissions = Permissions.TELEPORT_WORLD)
    public HandleResponse onSpawnAtWorld(PlayerHandler playerHandler, World world) {
        return TeleportService.worldCommand.onWorldTeleport(playerHandler, world);
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.TELEPORT_SPAWN_OTHERS)
    public HandleResponse onSpawnOther(CommandSender commandSender, PlayerHandler playerHandler) {
        TeleportBuilder.getBuilder(playerHandler).setLocation(playerHandler.getPlayer().getWorld().getSpawnLocation()).registerLastLocation(true).setInstantaneousTeleport(true).execute();
        return HandleResponse.RETURN_TRUE;
    }

    // Hope I don't have to {""} on every additional permissions
    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = {Permissions.TELEPORT_SPAWN_OTHERS, Permissions.TELEPORT_WORLD, Permissions.TELEPORT_WORLD_OTHERS})
    public HandleResponse onSpawnOtherAtWorld(CommandSender commandSender, PlayerHandler playerHandler, World world) {
        return TeleportService.worldCommand.onWorldTeleportOther(commandSender, playerHandler, world);
    }
}
