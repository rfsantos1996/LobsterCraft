package com.jabyftw.lobstercraft.commands.login;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.util.Util;

import java.security.NoSuchAlgorithmException;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
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
public class ChangePasswordCommand extends CommandExecutor {

    public ChangePasswordCommand() {
        super("changepassword", null, "Permite ao jogador trocar sua senha.", "/changepass (senha atual) (nova senha) (nova senha)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onChangePassword(OnlinePlayer onlinePlayer, String oldPassword, String password1, String password2) {
        // Check if passwords match
        if (!password1.equals(password2)) {
            onlinePlayer.getPlayer().sendMessage("§cAs novas senhas não coincidem.");
            return true;
        }

        // Check password length
        if (!Util.checkStringLength(password1, 3, 16)) {
            onlinePlayer.getPlayer().sendMessage("§cSenha inválida: tamanho inapropriado");
            return true;
        }

        try {
            if (!onlinePlayer.getOfflinePlayer().getEncryptedPassword().equals(Util.encryptString(oldPassword))) {
                onlinePlayer.getPlayer().sendMessage("§4Senha antiga não coincide!");
                return true;
            }
        } catch (NoSuchAlgorithmException exception) {
            exception.printStackTrace();
            onlinePlayer.getPlayer().sendMessage("§4Ocorreu um erro! §cTente novamente mais tarde.");
            return true;
        }

        if (LobsterCraft.servicesManager.playerHandlerService.changePlayerPassword(onlinePlayer.getOfflinePlayer(), password1)) {
            onlinePlayer.getPlayer().sendMessage("§6Sua senha foi alterada!");
            return true;
        } else {
            onlinePlayer.getPlayer().sendMessage("§4Jogador não registrado!");
            return true;
        }
    }
}
