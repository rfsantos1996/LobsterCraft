package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.util.Util;
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
        super("godmode", Permissions.PLAYER_GOD_MODE.toString(), "Faz os jogadores ficarem imortais", "/godmode");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onGodMode(OnlinePlayer playerHandler) {
        return onGodMode(playerHandler, !playerHandler.isGodMode()); // toggle
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onGodMode(OnlinePlayer onlinePlayer, boolean godMode) {
        onlinePlayer.getPlayer().sendMessage(
                onlinePlayer.setGodMode(godMode) ? "§6Você entrou no modo deus (god mode)."
                        : "§cVocê saiu do modo deus (god mode)."
        );
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GOD_MODE_OTHERS)
    private boolean onGodModeByOther(CommandSender commandSender, OnlinePlayer targetPlayer) {
        return onGodModeByOther(commandSender, targetPlayer, !targetPlayer.isGodMode()); // toggle
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_GOD_MODE_OTHERS)
    private boolean onGodModeByOther(CommandSender commandSender, OnlinePlayer targetPlayer, boolean godMode) {
        boolean isGodMode = targetPlayer.setGodMode(godMode);

        commandSender.sendMessage(
                Util.appendStrings("§6Jogador ", targetPlayer.getPlayer().getName(),
                        isGodMode ? " está em modo deus (god mode)." : " saiu do modo deus (god mode)."
                ));
        targetPlayer.getPlayer().sendMessage(Util.appendStrings(isGodMode ? "§6Você entrou no modo deus (god mode) por §c" : "§cVocê saiu do modo deus (god mode) por §c",
                commandSender.getName()));
        return true;
    }
}
