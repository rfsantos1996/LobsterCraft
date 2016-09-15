package com.jabyftw.lobstercraft.commands.login;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.Bukkit;

import java.security.NoSuchAlgorithmException;

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
public class LoginCommand extends CommandExecutor {

    public LoginCommand() {
        super("login", null, "Permite ao jogador logar no servidor de modo seguro.", "/login (senha) §7[sem parênteses]");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onLogin(final OnlinePlayer onlinePlayer, final String password) {
        try {
            OnlinePlayer.LoginResponse response = onlinePlayer.attemptLoginPlayer(Util.encryptString(password));
            switch (response) {
                case PASSWORD_DO_NOT_MATCH:
                    onlinePlayer.getPlayer().sendMessage("§cSenha não coincide.");
                    return true;
                case NOT_REGISTERED:
                    onlinePlayer.getPlayer().sendMessage("§cVocê não está registrado!");
                    return true;
                case ALREADY_LOGGED_IN:
                    onlinePlayer.getPlayer().sendMessage("§cVocê já está online!");
                    return true;
                case ALREADY_REGISTERED:
                case LOGIN_WENT_ASYNCHRONOUS_SUCCESSFULLY:
                    // Do nothing/not possible to occur
                    return false;
                default:
                    throw new IllegalStateException(Util.appendStrings("Option not handled on player login: ", response));
            }
        } catch (NoSuchAlgorithmException exception) {
            exception.printStackTrace();
            onlinePlayer.getPlayer().sendMessage("§4Um erro ocorreu! §cTente novamente mais tarde.");
            return true;
        }
    }
}
