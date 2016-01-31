package com.jabyftw.pacocacraft2.location.commands;


import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft2.PacocaCraft;
import com.jabyftw.pacocacraft2.location.TeleportBuilder;
import com.jabyftw.pacocacraft2.location.TeleportProfile;
import com.jabyftw.pacocacraft2.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft2.util.Permissions;
import com.jabyftw.pacocacraft2.util.Util;
import org.bukkit.Location;

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
public class BackCommand extends CommandExecutor {

    public BackCommand() {
        super(PacocaCraft.pacocaCraft, "back", Permissions.TELEPORT_BACK, "§6Permite ao jogador voltar a sua antiga localização", "§c/back");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onBack(PlayerHandler playerHandler) {
        Location lastLocation = playerHandler.getProfile(TeleportProfile.class).getLastLocation();

        // Check if location is null and teleport player if not
        if (lastLocation == null)
            Util.sendPlayerMessage(playerHandler, "§cSua ultima localização não está definida!");
        else
            TeleportBuilder.getBuilder(playerHandler).setLocation(lastLocation).registerLastLocation(true).warnTeleportingPlayer(true).waitBeforeListenerTriggers(true).execute();
        return true;
    }
}
