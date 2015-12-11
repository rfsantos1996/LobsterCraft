package com.jabyftw.pacocacraft.location.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.block.StorableLocation;
import com.jabyftw.pacocacraft.location.TeleportBuilder;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.Location;
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
public class TeleportCommand extends CommandExecutor {

    public TeleportCommand() {
        super(PacocaCraft.pacocaCraft, "teleport", Permissions.TELEPORT_TELEPORT_TO,
                "§6Permite ao jogador teleportar para outros jogadores e lugares.",
                "§c/teleport (§4jogador§c)\n§c/teleport (§4localização§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onTeleportToLocation(PlayerHandler playerHandler, Location location) {
        TeleportBuilder.getBuilder(playerHandler).setLocation(location).registerLastLocation(true).warnTargetPlayer(true).warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true).execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onTeleportToPlayer(PlayerHandler playerHandler, PlayerHandler target) {
        TeleportBuilder.getBuilder(playerHandler).setPlayerLocation(target).registerLastLocation(true).warnTargetPlayer(true).warnTeleportingPlayer(true)
                .waitBeforeListenerTriggers(true).execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.TELEPORT_TELEPORT_TO_OTHERS)
    public boolean onTeleportOthersToLocation(CommandSender commandSender, PlayerHandler teleporting, Location location) {
        TeleportBuilder.getBuilder(teleporting).setLocation(location).registerLastLocation(true).warnTargetPlayer(true).setInstantaneousTeleport(true).execute();
        commandSender.sendMessage(teleporting.getPlayer().getDisplayName() + "§6 foi teleportado para §c" + new StorableLocation(location).toString());
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.TELEPORT_TELEPORT_TO_OTHERS)
    public boolean onTeleportOthersToLocation(CommandSender commandSender, PlayerHandler teleporting, PlayerHandler target) {
        TeleportBuilder.getBuilder(teleporting).setPlayerLocation(target).registerLastLocation(true).warnTargetPlayer(true).setInstantaneousTeleport(true).execute();
        commandSender.sendMessage(teleporting.getPlayer().getDisplayName() + "§6 foi teleportado para " + target.getPlayer().getDisplayName());
        return true;
    }
}
