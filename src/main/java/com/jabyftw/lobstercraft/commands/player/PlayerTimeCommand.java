package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.Util;

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
        super("ptime", Permissions.PLAYER_PLAYER_TIME, "Permite ao jogador alterar seu proprio horario", "/ptime (horas do dia - 7h28m)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onPlayerTime(PlayerHandler playerHandler) {
        playerHandler.getPlayer().resetPlayerTime();
        playerHandler.sendMessage("§cTempo padrão restaurado!");
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onPlayerTime(PlayerHandler playerHandler, Long timeDifference) {
        long minecraftTime = Util.getMinecraftTime(timeDifference / TimeUnit.SECONDS.toMillis(1));
        LobsterCraft.logger.info(
                "playerTime=" + playerHandler.getPlayer().getPlayerTime() + ", " +
                        "parsedMinecraftTime=" + minecraftTime + ", " +
                        "worldTime=" + playerHandler.getPlayer().getWorld().getTime()
        );

        playerHandler.getPlayer().setPlayerTime(minecraftTime, false);
        playerHandler.sendMessage("§6Tempo atualizado! Use §c/ptime§6 para restaurar.");
        return true;
    }
}
