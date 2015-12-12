package com.jabyftw.pacocacraft.login.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.login.UserLoginService;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.profile_util.PlayerHandler;
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
public class LoginCommand extends CommandExecutor {

    public LoginCommand() {
        super(PacocaCraft.pacocaCraft, "login", Permissions.JOIN_PLAYER_LOGIN, "§6Permite o jogador entrar no servidor usando sua senha", "§c/login (§4senha§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onDefaultLogin(PlayerHandler playerHandler, String password) {
        BukkitScheduler.runTaskAsynchronously(PacocaCraft.pacocaCraft, () -> {
            try {
                playerHandler.getProfile(UserProfile.class).attemptLogin(Util.encryptString(password));
            } catch(NoSuchAlgorithmException e) {
                e.printStackTrace();
                playerHandler.getPlayer().sendMessage("§4Ocorreu um erro! §cTente novamente mais tarde");
            }
        });

        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER, additionalPermissions = Permissions.JOIN_OTHER_ACCOUNT_REGISTRATION)
    public boolean onRegisterLogin(PlayerHandler playerHandler, String password1, String password2) {
        return UserLoginService.registerCommand.onDefaultRegister(playerHandler, password1, password2);
    }

    /**
     * Yes, the server may use your account. AND ONLY THE SERVER CONSOLE may allow this.
     *
     * @param commandSender command sender (console, in this case)
     * @param playerHandler desired player
     *
     * @return a possible HandleResponse
     */
    @CommandHandler(senderType = SenderType.CONSOLE, additionalPermissions = Permissions.JOIN_OTHER_ACCOUNT_LOOKUP)
    public HandleResponse onConsoleLogin(CommandSender commandSender, final PlayerHandler playerHandler) {
        // Check if playerHandler can be looked up
        if(PacocaCraft.permission.playerHas(playerHandler.getPlayer(), Permissions.JOIN_PREVENT_ACCOUNT_LOOKUP))
            return HandleResponse.RETURN_NO_PERMISSION;

        BukkitScheduler.runTaskAsynchronously(PacocaCraft.pacocaCraft, () -> {
            UserProfile profile = playerHandler.getProfile(UserProfile.class);
            if(profile.attemptLogin(profile.getEncryptedPassword()))
                commandSender.sendMessage("§aLogin de §6" + playerHandler.getPlayer().getName() + "§a foi bem sucedido");
            else
                commandSender.sendMessage("§cLogin de §6" + playerHandler.getPlayer().getName() + "§c falhou");
        });

        return HandleResponse.RETURN_TRUE;
    }
}
