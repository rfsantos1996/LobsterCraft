package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.Permissions;
import org.bukkit.command.CommandSender;

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
public class GodModeCommand extends CommandExecutor {

    public GodModeCommand() {
        super("godmode", Permissions.PLAYER_GOD_MODE, "Faz os jogadores ficarem imortais", "/godmode");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onGodMode(PlayerHandler playerHandler) {
        return onGodMode(playerHandler, !playerHandler.isGodMode()); // toggle
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onGodMode(PlayerHandler playerHandler, boolean godMode) {
        playerHandler.sendMessage(
                playerHandler.setGodMode(godMode) ? "§6Você entrou no modo deus (god mode)."
                        : "§cVocê saiu do modo deus (god mode)."
        );
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GOD_MODE_OTHERS)
    public boolean onGodModeByOther(CommandSender commandSender, PlayerHandler targetPlayer) {
        return onGodModeByOther(commandSender, targetPlayer, !targetPlayer.isGodMode()); // toggle
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GOD_MODE_OTHERS)
    public boolean onGodModeByOther(CommandSender commandSender, PlayerHandler targetPlayer, boolean godMode) {
        boolean isGodMode = targetPlayer.setGodMode(godMode);

        commandSender.sendMessage(
                "§6Jogador " + targetPlayer.getPlayer().getName() +
                        (isGodMode ? " está em modo deus (god mode)." : " saiu do modo deus (god mode).")
        );
        targetPlayer.sendMessage(
                isGodMode ? "§6Você entrou no modo deus (god mode) por §c" + commandSender.getName()
                        : "§cVocê saiu do modo deus (god mode) por §c" + commandSender.getName()
        );
        return true;
    }
}
