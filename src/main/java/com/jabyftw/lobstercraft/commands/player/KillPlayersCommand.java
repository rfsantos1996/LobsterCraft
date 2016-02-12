package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.Permissions;
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
public class KillPlayersCommand extends CommandExecutor {

    public KillPlayersCommand() {
        super("kill", Permissions.PLAYER_KILL, "Permite ao jogador matar jogadores", "/kill (jogador)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public boolean onKill(CommandSender commandSender, PlayerHandler playerHandler) {
        playerHandler.getPlayer().damage(Double.MAX_VALUE - 1);

        // Make sure he is dead
        if (playerHandler.getPlayer().getHealth() > 0d)
            playerHandler.getPlayer().setHealth(0d);

        playerHandler.sendMessage("§cVocê foi morto por §4" + commandSender.getName() + "§c através de comando.");
        commandSender.sendMessage("§cVocê matou " + playerHandler.getPlayer().getDisplayName());
        return true;
    }
}
