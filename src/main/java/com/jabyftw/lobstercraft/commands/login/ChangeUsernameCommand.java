package com.jabyftw.lobstercraft.commands.login;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.BukkitScheduler;

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
public class ChangeUsernameCommand extends CommandExecutor {

    public ChangeUsernameCommand() {
        super("changeusername", Permissions.JOIN_CHANGE_USERNAME, "Permite ao jogador alterar seu nome", "/changeusername (novo nome) (novo nome) (senha atual)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onChangeUsername(PlayerHandler playerHandler, String username1, String username2, String password) {
        // Check (ignoring case) both usernames
        if (!username1.equalsIgnoreCase(username2)) {
            playerHandler.sendMessage("§cOs nomes de usuário não coincidem");
            return true;
        }

        BukkitScheduler.runTaskAsynchronously(() -> {
            switch (playerHandler.changePlayerName(username1, password)) {
                case SUCCESSFULLY_CHANGED:
                    // Player will be kicked
                    return;
                case CANT_CHANGE_NAME_YET:
                    playerHandler.sendMessage("§cVocê ainda não pode trocar de nome. Espere alguns dias.");
                    return;
                case PLAYER_NAME_NOT_VALID:
                    playerHandler.sendMessage("§4Nome inválido: §ctente outro respeitando tamanho e caractéres (min: 3, max: 6, sem espaços ou traços, _ é permitido).");
                    return;
                case PLAYER_NAME_NOT_AVAILABLE:
                    playerHandler.sendMessage("§cNome não disponível atualmente.");
                    return;
                case WRONG_PASSWORD:
                    playerHandler.sendMessage("§cSenha incorreta!");
                    return;
                case NOT_REGISTERED:
                    playerHandler.sendMessage("§4Você não está registrado");
                    return;
                case ERROR_OCCURRED:
                    playerHandler.sendMessage("§4Ocorreu um erro, tente mais tarde.");
                    return;
            }
            throw new IllegalArgumentException("Option non handled at changePlayerName command");
        });
        return true;
    }
}
