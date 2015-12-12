package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.GameMode;
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
public class GameModeCommand extends CommandExecutor {

    public GameModeCommand() {
        super(PacocaCraft.pacocaCraft, "gamemode", Permissions.PLAYER_GAMEMODE, "§6Altera o modo de jogo do jogador", "§c/gamemode (§4modo de jogo: SURVIVAL, CREATIVE etc§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onGameMode(PlayerHandler playerHandler, GameMode gameMode) {
        if(playerHandler.getPlayer().getGameMode() != gameMode) {
            playerHandler.getPlayer().setGameMode(gameMode);
            playerHandler.getPlayer().sendMessage("§6Seu modo de jogo foi alterado para " + gameMode.name());
        } else {
            playerHandler.getPlayer().sendMessage("§cSeu modo de jogo já é este!");
        }
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GAMEMODE_OTHERS)
    public boolean onGameModeOther(CommandSender commandSender, PlayerHandler target, GameMode gameMode) {
        if(target.getPlayer().getGameMode() != gameMode) {
            target.getPlayer().setGameMode(gameMode);
            target.getPlayer().sendMessage("§6Seu modo de jogo foi alterado para " + gameMode.name());
            commandSender.sendMessage("§6O modo de jogo de §c" + target.getPlayer().getName() + "§6 foi alterado para " + gameMode.name());
        } else {
            commandSender.sendMessage("§cO modo de jogo de " + target.getPlayer().getName() + " já é este!");
        }
        return true;
    }
}
