package com.jabyftw.lobstercraft.commands.location;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.TeleportBuilder;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.util.Util;
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
        super("world", Permissions.LOCATION_CHANGE_WORLD.toString(), "Permite ao jogador mudar de mundo", "/world (jogador) (nome)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    private boolean onWorldList(CommandSender commandSender) {
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
    protected boolean onWorldTeleport(OnlinePlayer onlinePlayer, World world) {
        TeleportBuilder.getBuilder(onlinePlayer)
                .setLocation(world.getSpawnLocation())
                .warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true)
                .execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.LOCATION_CHANGE_WORLD_OTHERS)
    protected boolean onWorldTeleportOther(CommandSender commandSender, OnlinePlayer onlinePlayer, World world) {
        TeleportBuilder.getBuilder(onlinePlayer)
                .setLocation(world.getSpawnLocation())
                .setInstantaneousTeleport(true)
                .execute();

        commandSender.sendMessage(Util.appendStrings(onlinePlayer.getPlayer().getDisplayName(), "§6 foi teleportado para §c", world.getName()));
        return true;
    }
}
