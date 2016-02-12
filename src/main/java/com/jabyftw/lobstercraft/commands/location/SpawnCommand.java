package com.jabyftw.lobstercraft.commands.location;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.commands.CommandService;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.location.TeleportBuilder;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
        super("spawn", Permissions.LOCATION_SPAWN, "Permite ao jogador voltar ao spawn", "/spawn (jogador) (mundo)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onSpawn(PlayerHandler playerHandler) {
        TeleportBuilder.getBuilder(playerHandler)
                .setLocation(playerHandler.getPlayer().getWorld().getSpawnLocation())
                .registerLastLocation(true)
                .warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true)
                .execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER, additionalPermissions = Permissions.LOCATION_CHANGE_WORLD)
    public boolean onSpawnAtWorld(PlayerHandler playerHandler, World world) {
        return CommandService.worldCommand.onWorldTeleport(playerHandler, world);
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.LOCATION_SPAWN_OTHERS)
    public boolean onSpawnOther(CommandSender commandSender, PlayerHandler playerHandler) {
        TeleportBuilder.getBuilder(playerHandler)
                .setLocation(playerHandler.getPlayer().getWorld().getSpawnLocation())
                .registerLastLocation(true)
                .setInstantaneousTeleport(true)
                .execute();
        return true;
    }

    // Hope I don't have to {""} on every additional permissions
    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = {Permissions.LOCATION_SPAWN_OTHERS, Permissions.LOCATION_CHANGE_WORLD, Permissions.LOCATION_CHANGE_WORLD_OTHERS})
    public boolean onSpawnOtherAtWorld(CommandSender commandSender, PlayerHandler playerHandler, World world) {
        return CommandService.worldCommand.onWorldTeleportOther(commandSender, playerHandler, world);
    }
}
