package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.Permissions;
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
        super("ptime", Permissions.PLAYER_PLAYER_TIME.toString(), "Permite ao jogador alterar seu proprio horario", "/ptime (horas do dia - 7h28m)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onPlayerTime(OnlinePlayer onlinePlayer) {
        onlinePlayer.getPlayer().resetPlayerTime();
        onlinePlayer.getPlayer().sendMessage("§cTempo padrão restaurado!");
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onPlayerTime(OnlinePlayer onlinePlayer, Long timeDifference) {
        long minecraftTime = Util.getMinecraftTime(timeDifference / TimeUnit.SECONDS.toMillis(1));
        LobsterCraft.logger.info(Util.appendStrings(
                "playerTime=", onlinePlayer.getPlayer().getPlayerTime(), ", ",
                "parsedMinecraftTime=", minecraftTime, ", ",
                "worldTime=", onlinePlayer.getPlayer().getWorld().getTime()
        ));

        onlinePlayer.getPlayer().setPlayerTime(minecraftTime, false);
        onlinePlayer.getPlayer().sendMessage("§6Tempo atualizado! Use §c/ptime§6 para restaurar.");
        return true;
    }
}
