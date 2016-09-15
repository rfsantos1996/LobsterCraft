package com.jabyftw.lobstercraft.commands.login;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.PlayerHandlerService;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.Bukkit;

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
public class ChangeUsernameCommand extends CommandExecutor {

    public ChangeUsernameCommand() {
        super("changeusername", Permissions.JOIN_CHANGE_USERNAME.toString(), "Permite ao jogador alterar seu nome", "/changeusername (novo nome) (novo nome) (senha atual)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onChangeUsername(OnlinePlayer onlinePlayer, String username1, String username2, String password) {
        // Check (ignoring case) both usernames
        if (!username1.equalsIgnoreCase(username2)) {
            onlinePlayer.getPlayer().sendMessage("§cOs nomes de usuário não coincidem");
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(LobsterCraft.plugin, () -> {
            try {
                PlayerHandlerService.ChangeNameResponse response = onlinePlayer.changePlayerName(username1, Util.encryptString(password));
                switch (response) {
                    case SUCCESSFULLY_CHANGED:
                        // Player will be kicked
                        return;
                    case NAME_INVALID:
                        onlinePlayer.getPlayer().sendMessage("§4Nome inválido: §ctente outro respeitando tamanho e caracteres (min: 3, max: 6, sem espaços ou traços, mas _ é permitido).");
                        return;
                    case NAME_PROTECTED:
                        onlinePlayer.getPlayer().sendMessage("§cNome não disponível atualmente.");
                        return;
                    case NAME_UNAVAILABLE:
                        onlinePlayer.getPlayer().sendMessage("§cJogador com este nome já foi registrado.");
                        return;
                    case CANT_CHANGE_NAME_YET:
                        onlinePlayer.getPlayer().sendMessage("§cVocê ainda não pode trocar de nome. Espere alguns dias.");
                        return;
                    case ERROR_OCCURRED:
                        onlinePlayer.getPlayer().sendMessage("§4Ocorreu um erro, tente mais tarde.");
                        return;
                    case INCORRECT_PASSWORD:
                        onlinePlayer.getPlayer().sendMessage("§cSenha atual incorreta!");
                        return;
                }
                throw new IllegalStateException(Util.appendStrings("Option not handled at changePlayerName command: ", response.name()));
            } catch (NoSuchAlgorithmException exception) {
                exception.printStackTrace();
                onlinePlayer.getPlayer().sendMessage("§4Um erro ocorreu, tente mais tarde.");
            }
        });
        return true;
    }
}
