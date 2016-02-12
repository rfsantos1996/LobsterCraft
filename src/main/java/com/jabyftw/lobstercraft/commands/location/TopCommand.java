package com.jabyftw.lobstercraft.commands.location;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.location.TeleportBuilder;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.world.util.location_util.BlockLocation;
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
        super("top", Permissions.LOCATION_TELEPORT_TO_THE_TOP, "Permite ao jogador ir ao topo de sua localização", "/top");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onTop(PlayerHandler playerHandler) {
        TeleportBuilder.getBuilder(playerHandler)
                .setLocation(getHighestLocation(playerHandler))
                .warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true)
                .execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.LOCATION_TELEPORT_OTHERS_TO_THE_TOP)
    public boolean onTopOthers(CommandSender commandSender, PlayerHandler playerHandler) {
        TeleportBuilder.getBuilder(playerHandler)
                .setLocation(getHighestLocation(playerHandler))
                .setInstantaneousTeleport(true)
                .execute();
        return true;
    }

    private Location getHighestLocation(@NotNull final PlayerHandler playerHandler) {
        Location location = playerHandler.getPlayer().getLocation();
        BlockLocation blockLocation = new BlockLocation(location);
        int highestBlockYAt = location.getChunk().getChunkSnapshot(true, false, false).getHighestBlockYAt(blockLocation.getRelativeX(), blockLocation.getRelativeZ());
        location.setY(++highestBlockYAt);
        return location;
    }
}
