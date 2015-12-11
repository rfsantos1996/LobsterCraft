package com.jabyftw.pacocacraft.login.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.command.CommandSender;

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
public class RegisterCommand extends CommandExecutor {

    public RegisterCommand() {
        super(PacocaCraft.pacocaCraft, "register", Permissions.JOIN_PLAYER_LOGIN, "§6Permite o jogador a entrar no servidor com sua senha.", "§c/register (§4senha§c) (§4senha§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onDefaultRegister(final PlayerHandler playerHandler, final String password1, String password2) {
        // Check if password is valid
        if(!isPasswordValid(playerHandler.getPlayer(), password1, password2))
            return true;

        BukkitScheduler.runTaskAsynchronously(PacocaCraft.pacocaCraft, () -> {
            try {
                // Register player (he will be already warned)
                playerHandler.getProfile(UserProfile.class).registerPlayer(Util.encryptString(password1));
            } catch(NoSuchAlgorithmException e) {
                e.printStackTrace();
                playerHandler.getPlayer().sendMessage("§4Um erro ocorreu! §cTente novamente mais tarde.");
            }
        });
        return true;
    }

    @CommandHandler(senderType = SenderType.CONSOLE, additionalPermissions = Permissions.JOIN_OTHER_ACCOUNT_REGISTRATION)
    public boolean onConsoleRegister(CommandSender commandSender, PlayerHandler playerHandler, String password1, String password2) {
        // Check if password is valid
        if(!isPasswordValid(commandSender, password1, password2))
            return true;

        BukkitScheduler.runTaskAsynchronously(PacocaCraft.pacocaCraft, () -> {
            try {
                // Register player and warn sender if succeeded
                if(playerHandler.getProfile(UserProfile.class).registerPlayer(Util.encryptString(password1)))
                    commandSender.sendMessage("§aRegistro de " + playerHandler.getPlayer().getName() + "§a foi bem sucedido!");
                else
                    commandSender.sendMessage("§cRegistro de " + playerHandler.getPlayer().getName() + "§c falhou!");
            } catch(NoSuchAlgorithmException e) {
                e.printStackTrace();
                playerHandler.getPlayer().sendMessage("§4Um erro ocorreu! §cTente novamente mais tarde.");
            }
        });
        return true;
    }

    public static boolean isPasswordValid(CommandSender commandSender, String password1, String password2) {
        // Check if passwords match
        if(!password1.equals(password2)) {
            commandSender.sendMessage("§4As senhas não coincidem!");
            return false;
        }

        // Check password length (shouldn't be less than 4 or more than 22 - encrypted password has fixed value of 64 characters)
        if(!Util.checkStringLength(password1, 4, 22)) {
            commandSender.sendMessage("§4Senha muito grande ou muito curta! §cDeve ter entre 4 e 22 caracteres");
            return false;
        }
        return true;
    }
}
