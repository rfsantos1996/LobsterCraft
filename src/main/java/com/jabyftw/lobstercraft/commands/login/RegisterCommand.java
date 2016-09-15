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
public class RegisterCommand extends CommandExecutor {

    public RegisterCommand() {
        super("register", null, "Permite ao jogador assegurar sua conta.", "/register (senha) (senha) §7[sem parênteses]");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onRegister(final OnlinePlayer onlinePlayer, final String password1, final String password2) {
        // Check if passwords match
        if (!password1.equals(password2)) {
            onlinePlayer.getPlayer().sendMessage("§cSenhas não coincidem.");
            return true;
        }

        // Check password length
        if (!Util.checkStringLength(password1, 3, 16)) {
            onlinePlayer.getPlayer().sendMessage("§cSenha inválida: tamanho inapropriado");
            return true;
        }

        try {
            OnlinePlayer.LoginResponse response = onlinePlayer.attemptRegisterPlayer(Util.encryptString(password1));
            switch (response) {
                case ALREADY_REGISTERED:
                    onlinePlayer.getPlayer().sendMessage("§cVocê já está registrado!");
                    return true;
                case LOGIN_WENT_ASYNCHRONOUS_SUCCESSFULLY:
                case PASSWORD_DO_NOT_MATCH:
                case NOT_REGISTERED:
                case ALREADY_LOGGED_IN:
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
