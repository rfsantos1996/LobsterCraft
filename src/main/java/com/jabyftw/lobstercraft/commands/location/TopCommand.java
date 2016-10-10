package com.jabyftw.lobstercraft.commands.location;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.TeleportBuilder;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.world.BlockLocation;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;
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
public class TopCommand extends CommandExecutor {

    public TopCommand() {
        super("top", Permissions.LOCATION_TELEPORT_TO_TOP.toString(), "Permite ao jogador ir ao topo de sua localização", "/top");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onTop(OnlinePlayer onlinePlayer) {
        TeleportBuilder.getBuilder(onlinePlayer)
                .setLocation(getHighestLocation(onlinePlayer))
                .warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true)
                .execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.LOCATION_TELEPORT_TO_TOP_OTHERS)
    private boolean onTopOthers(CommandSender commandSender, OnlinePlayer teleportingPlayer) {
        TeleportBuilder.getBuilder(teleportingPlayer)
                .setLocation(getHighestLocation(teleportingPlayer))
                .setInstantaneousTeleport(true)
                .execute();
        return true;
    }

    private Location getHighestLocation(@NotNull final OnlinePlayer onlinePlayer) {
        Location location = onlinePlayer.getPlayer().getLocation();
        BlockLocation blockLocation = new BlockLocation(location);
        int highestBlockYAt = location.getChunk().getChunkSnapshot(true, false, false).getHighestBlockYAt(blockLocation.getRelativeX(), blockLocation.getRelativeZ());
        location.setY(++highestBlockYAt);
        return location;
    }
}
