package com.jabyftw.pacocacraft.login.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.login.JoinListener;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

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
public class ChangeUsernameCommand extends CommandExecutor {

    private final static long REQUIRED_TIME_CHANGE_NAME = TimeUnit.DAYS.toMillis(PacocaCraft.config.getInt(ConfigValue.LOGIN_REQUIRED_TIME_CHANGE_USERNAME.getPath()));

    public ChangeUsernameCommand() {
        super(PacocaCraft.pacocaCraft, "changeuser", Permissions.JOIN_CHANGE_USER, "§6Permite ao jogador alterar seu nome de usuário", "§c/changeuser (§4novo nome§c) (§4novo nome§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onChangeUsername(PlayerHandler playerHandler, String username1, String username2) {
        Player player = playerHandler.getPlayer();

        // Check if username are equal (upper case will be ignored anyways)
        if(!username1.equalsIgnoreCase(username2)) {
            player.sendMessage("§cAs contas não coincidem!");
            return true;
        }

        // Check for special characters and length
        if(!JoinListener.isValidPlayerName(username1)) {
            player.sendMessage("§cNome de usuário com caracteres especiais, muito longo ou muito curto!\n" +
                    "§cDeve estar entre 3 e 16 caracteres, contendo §4A-Z§c,§4 a-z§c incluindo §4_");
            return true;
        }

        UserProfile profile = playerHandler.getProfile(UserProfile.class);
        long timeSinceLastChange = Math.abs(System.currentTimeMillis() - profile.getUsernameChangeDate()); // Actual time - change date

        // If player isn't changing to its past username AND its username change date is less than the required time
        if(profile.getLastUsername() != null && timeSinceLastChange <= REQUIRED_TIME_CHANGE_NAME) {
            // NOTE: do not allow non-stopping changes (not even to the last username)
            player.sendMessage("§cVocê só pode alterar seu nome novamente daqui §4" + Util.parseTimeInMillis(Math.abs(REQUIRED_TIME_CHANGE_NAME - timeSinceLastChange)));
            return true;
        }

        // Asynchronously check if username is available
        BukkitScheduler.runTaskAsynchronously(PacocaCraft.pacocaCraft, () -> {
            try {
                // Check if username is available
                if(UserProfile.isUsernameAvailable(username1)) {
                    // Set player name (it'll be marked as changed and be saved on database upon log out)
                    if(!profile.setPlayerName(username1))
                        player.sendMessage("§cOcorreu um erro!");
                } else {
                    player.sendMessage("§cUsuário já existente!");
                }
            } catch(SQLException e) {
                e.printStackTrace();
                player.sendMessage("§4Falha ao procurar usuário no banco de dados!");
            }
        });

        return true;
    }
}
