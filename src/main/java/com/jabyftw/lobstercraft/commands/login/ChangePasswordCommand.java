package com.jabyftw.lobstercraft.commands.login;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.util.Util;

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
    public boolean onChangePassword(PlayerHandler playerHandler, String oldPassword, String password1, String password2) {
        // Check if passwords match
        if (!password1.equals(password2)) {
            playerHandler.sendMessage("§cAs novas senhas não coincidem.");
            return true;
        }

        // Check password length
        if (!Util.checkStringLength(password1, 3, 16)) {
            playerHandler.sendMessage("§cSenha inválida: tamanho inapropriado");
            return true;
        }

        switch (playerHandler.changePlayerPassword(oldPassword, password1)) {
            case WRONG_OLD_PASSWORD:
                playerHandler.sendMessage("§cSenha atual incorreta!");
                return true;
            case ERROR_OCCURRED:
                playerHandler.sendMessage("§4Ocorreu um erro! §cTente novamente mais tarde.");
                return true;
            case SUCCESSFULLY_CHANGED:
                playerHandler.sendMessage("§6Sua senha foi alterada!");
                return true;
        }
        throw new IllegalStateException("Didn't handled a case at password change");
    }
}
