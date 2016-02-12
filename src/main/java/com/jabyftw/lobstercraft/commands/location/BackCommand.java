package com.jabyftw.lobstercraft.commands.location;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.location.LocationProfile;
import com.jabyftw.lobstercraft.player.location.TeleportBuilder;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.Location;

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
public class BackCommand extends CommandExecutor {

    public BackCommand() {
        super("back", Permissions.LOCATION_TELEPORT_BACK, "Permite ao jogador retornar a antiga localização", "/back");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onBack(PlayerHandler playerHandler) {
        Location lastLocation = playerHandler.getProfile(LocationProfile.class).getLastLocation();

        // Check if location is null and teleport player if not
        if (lastLocation == null)
            playerHandler.sendMessage("§cSua ultima localização não está definida!");
        else
            TeleportBuilder.getBuilder(playerHandler)
                    .setLocation(lastLocation)
                    .registerLastLocation(true)
                    .warnTeleportingPlayer(true)
                    .waitBeforeListenerTriggers(true)
                    .execute();
        return true;
    }

}
