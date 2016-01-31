package com.jabyftw.pacocacraft2.location.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft2.PacocaCraft;
import com.jabyftw.pacocacraft2.location.TeleportBuilder;
import com.jabyftw.pacocacraft2.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft2.util.Permissions;

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
public class TeleportHereCommand extends CommandExecutor {

    public TeleportHereCommand() {
        super(PacocaCraft.pacocaCraft, "teleporthere", Permissions.TELEPORT_TELEPORT_HERE, "§6Permite chamar jogadores até sua localização", "§c/teleporthere (§4jogador§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onTeleportHere(PlayerHandler playerHandler, PlayerHandler target) {
        TeleportBuilder.getBuilder(target).setPlayerLocation(playerHandler).setInstantaneousTeleport(true).registerLastLocation(true).execute();
        return true;
    }
}
