package com.jabyftw.pacocacraft.login.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;

import java.security.NoSuchAlgorithmException;

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
public class ChangePasswordCommand extends CommandExecutor {

    public ChangePasswordCommand() {
        super(PacocaCraft.pacocaCraft, "changepass", Permissions.JOIN_CHANGE_PASSWORD, "§6Permite ao jogador alterar sua senha", "§c/changepass (§4antiga senha§c) (§4nova senha§c) (§4nova senha§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onChangePassword(PlayerHandler playerHandler, String oldPassword, String password1, String password2) {
        // Check if password is valid (this method already warns the player)
        if(!RegisterCommand.isPasswordValid(playerHandler.getPlayer(), password1, password2))
            return true;

        try {
            UserProfile userProfile = playerHandler.getProfile(UserProfile.class);

            // Check if old password match
            if(!userProfile.getEncryptedPassword().equals(Util.encryptString(oldPassword))) {
                playerHandler.getPlayer().sendMessage("§cSenha antiga não coincide!");
                return true;
            }

            // Change player's password and warn player
            userProfile.setPassword(Util.encryptString(password1));
            playerHandler.getPlayer().sendMessage("§6Senha alterada com sucesso!");
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
            playerHandler.getPlayer().sendMessage("§cFalha ao tentar alterar sua senha.");
            return true;
        }

        return true;
    }
}
