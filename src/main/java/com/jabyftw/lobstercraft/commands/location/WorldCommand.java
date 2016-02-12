package com.jabyftw.lobstercraft.commands.location;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.location.TeleportBuilder;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.List;

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
public class WorldCommand extends CommandExecutor {

    public WorldCommand() {
        super("world", Permissions.LOCATION_CHANGE_WORLD, "Permite ao jogador mudar de mundo", "/world (jogador) (nome)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onWorldList(CommandSender commandSender) {
        StringBuilder stringBuilder = new StringBuilder("§6Mundos: ");
        List<World> worlds = Bukkit.getWorlds();

        // Iterate through all worlds
        for (int i = 0; i < worlds.size(); i++) {
            stringBuilder.append("§c").append(worlds.get(i).getName());
            if (i < worlds.size() - 1) stringBuilder.append("§6, ");
        }

        commandSender.sendMessage(stringBuilder.toString());
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onWorldTeleport(PlayerHandler playerHandler, World world) {
        TeleportBuilder.getBuilder(playerHandler)
                .setLocation(world.getSpawnLocation())
                .registerLastLocation(true)
                .warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true)
                .execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.LOCATION_CHANGE_WORLD_OTHERS)
    public boolean onWorldTeleportOther(CommandSender commandSender, PlayerHandler playerHandler, World world) {
        TeleportBuilder.getBuilder(playerHandler).setLocation(world.getSpawnLocation()).registerLastLocation(true).setInstantaneousTeleport(true).execute();

        commandSender.sendMessage(playerHandler.getPlayer().getDisplayName() + "§6 foi teleportado para §c" + world.getName());
        return true;
    }
}
