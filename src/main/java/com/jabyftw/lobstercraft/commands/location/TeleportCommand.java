package com.jabyftw.lobstercraft.commands.location;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.TeleportBuilder;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.Util;
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
public class TeleportCommand extends CommandExecutor {

    public TeleportCommand() {
        super("teleport", null, "Permite ao jogador teleportar a lugares e outros jogadores", "/tp (jogador) §6ou §c/tp (localização)");
    }

    @CommandHandler(senderType = SenderType.PLAYER, additionalPermissions = Permissions.LOCATION_TELEPORT_TO_PLAYER)
    private boolean onTeleportToPlayer(OnlinePlayer teleportingPlayer, OnlinePlayer target) {
        TeleportBuilder.getBuilder(teleportingPlayer)
                .setPlayerLocation(target)
                .warnTargetPlayer(true)
                .warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true)
                .execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER, additionalPermissions = Permissions.LOCATION_TELEPORT_TO_LOCATION)
    private boolean onTeleportToLocation(OnlinePlayer teleportingPlayer, Location location) {
        TeleportBuilder.getBuilder(teleportingPlayer)
                .setLocation(location)
                .warnTargetPlayer(true)
                .warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true)
                .execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.LOCATION_TELEPORT_TO_LOCATION_OTHERS)
    private boolean onTeleportOthersToPlayer(CommandSender commandSender, OnlinePlayer teleporting, Location location) {
        TeleportBuilder.getBuilder(teleporting)
                .setLocation(location)
                .warnTargetPlayer(true)
                .setInstantaneousTeleport(true)
                .execute();
        commandSender.sendMessage(Util.appendStrings(teleporting.getPlayer().getDisplayName(), "§6 foi teleportado para §c", Util.locationToString(location)));
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.LOCATION_TELEPORT_TO_PLAYER_OTHERS)
    private boolean onTeleportOthersToLocation(CommandSender commandSender, OnlinePlayer teleporting, OnlinePlayer target) {
        TeleportBuilder.getBuilder(teleporting)
                .setPlayerLocation(target)
                .warnTargetPlayer(true)
                .setInstantaneousTeleport(true)
                .execute();
        commandSender.sendMessage(Util.appendStrings(teleporting.getPlayer().getDisplayName(), "§6 foi teleportado para ", target.getPlayer().getDisplayName()));
        return true;
    }
}
