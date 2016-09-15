package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Email address: rafael.sartori96@gmail.com
 */
public class GameModeCommand extends CommandExecutor {

    public GameModeCommand() {
        super("gamemode", Permissions.PLAYER_GAME_MODE.toString(), "Permite ao jogador mudar de gamemode", "/gamemode (modo de jogo)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onGameMode(OnlinePlayer onlinePlayer, GameMode gameMode) {
        if (onlinePlayer.setGameMode(gameMode)) {
            onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§6Seu modo de jogo foi alterado para ", gameMode.name()));
        } else {
            onlinePlayer.getPlayer().sendMessage("§cSeu modo de jogo já é este!");
        }
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GAME_MODE_OTHERS)
    private boolean onGameModeOther(CommandSender commandSender, OnlinePlayer target, GameMode gameMode) {
        if (target.setGameMode(gameMode)) {
            target.getPlayer().sendMessage(Util.appendStrings("§6Seu modo de jogo foi alterado para ", gameMode.name()));
            commandSender.sendMessage(Util.appendStrings("§6O modo de jogo de §c", target.getPlayer().getName(), "§6 foi alterado para ", gameMode.name()));
        } else {
            commandSender.sendMessage(Util.appendStrings("§cO modo de jogo de ", target.getPlayer().getName(), " já é este!"));
        }
        return true;
    }
}
