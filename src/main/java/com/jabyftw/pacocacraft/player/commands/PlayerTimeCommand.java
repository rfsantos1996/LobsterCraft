package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;

import java.util.concurrent.TimeUnit;

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
public class PlayerTimeCommand extends CommandExecutor {

    public PlayerTimeCommand() {
        super(PacocaCraft.pacocaCraft, "ptime", Permissions.PLAYER_INDIVIDUAL_TIME_CHANGE, "§6Permite ao jogador alterar seu proprio horario", "§c/ptime (§4horas do dia - 7h28m§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onPlayerTime(PlayerHandler playerHandler) {
        playerHandler.getPlayer().resetPlayerTime();
        playerHandler.getPlayer().sendMessage("§cTempo restaurado!");
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onPlayerTime(PlayerHandler playerHandler, Long timeDifference) {
        long timeDifferenceSeconds = timeDifference / TimeUnit.SECONDS.toMillis(1); // Millis / (millis / second) => seconds
        playerHandler.getPlayer().setPlayerTime(Math.abs(playerHandler.getPlayer().getWorld().getTime() - Util.getMinecraftTime(timeDifferenceSeconds)), true);
        playerHandler.getPlayer().sendMessage("§6Tempo atualizado! Use §c/ptime§6 para restaurar.");
        return true;
    }
}
