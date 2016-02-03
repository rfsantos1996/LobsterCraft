package com.jabyftw.lobstercraft.commands.login;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.util.BukkitScheduler;

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
    public boolean onLogin(final PlayerHandler playerHandler, final String password1, final String password2) {
        // Check if passwords match
        if (!password1.equals(password2)) {
            playerHandler.sendMessage("§cSenhas não coincidem.");
            return true;
        }

        BukkitScheduler.runTaskAsynchronously(() -> {
            switch (playerHandler.attemptRegister(password1)) {
                case ERROR_OCCURRED:
                    BukkitScheduler.runTask(() -> playerHandler.getPlayer().kickPlayer("§4Ocorreu um erro durante login!\n§cTente novamente mais tarde."));
                    break;
                case ALREADY_REGISTERED:
                    playerHandler.sendMessage("§cVocê já está registrado!");
                    break;
                default:
                    break;
            }
        });
        return true;
    }
}
