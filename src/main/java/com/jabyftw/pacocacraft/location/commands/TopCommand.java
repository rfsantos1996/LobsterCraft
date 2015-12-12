package com.jabyftw.pacocacraft.location.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.location.TeleportBuilder;
import com.jabyftw.profile_util.PlayerHandler;
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
public class TopCommand extends CommandExecutor {

    public TopCommand() {
        super(PacocaCraft.pacocaCraft, "top", Permissions.TELEPORT_TOP, "§6Permite ao jogador teleportar ao bloco mais alto", "§c/top");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onTop(PlayerHandler playerHandler) {
        TeleportBuilder.getBuilder(playerHandler).setLocation(getHighestLocation(playerHandler)).warnTeleportingPlayer(true).waitBeforeListenerTriggers(true).execute();
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.TELEPORT_TOP_OTHERS)
    public boolean onTopOthers(CommandSender commandSender, PlayerHandler playerHandler) {
        TeleportBuilder.getBuilder(playerHandler).setLocation(getHighestLocation(playerHandler)).setInstantaneousTeleport(true).execute();
        return true;
    }

    private Location getHighestLocation(PlayerHandler playerHandler) {
        Location location = playerHandler.getPlayer().getLocation();
        int highestBlockYAt = location.getChunk().getChunkSnapshot(true, false, false).getHighestBlockYAt(location.getBlockX(), location.getBlockY());
        location.setY(++highestBlockYAt);
        return location;
    }
}
