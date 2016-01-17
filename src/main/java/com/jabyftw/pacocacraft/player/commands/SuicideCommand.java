package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.Permissions;

import java.util.ArrayList;

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
public class SuicideCommand extends CommandExecutor {

    private final static long SUICIDE_CONFIRMATION_TICKS = PacocaCraft.config.getLong(ConfigValue.PLAYER_TIME_SUICIDE_CONFIRMATION.getPath()) * 20L; // Seconds * 20 -> ticks
    private final static ArrayList<String> usedCommand = new ArrayList<>();

    public SuicideCommand() {
        super(PacocaCraft.pacocaCraft, "suicide", Permissions.PLAYER_SUICIDE, "§6Permite ao jogador tirar a propria vida", "§c/suicide");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onSuicide(PlayerHandler playerHandler) {
        String playerName = playerHandler.getPlayer().getName().toLowerCase();

        if(usedCommand.contains(playerName)) {
            playerHandler.getPlayer().damage(Double.MAX_VALUE - 1);

            // Make sure he is dead
            if(playerHandler.getPlayer().getHealth() > 0d)
                playerHandler.getPlayer().setHealth(0d);

            Util.sendPlayerMessage(playerHandler, "§4Você ingeriu 264mg de cianeto de hidrogênio!");
            usedCommand.remove(playerName);
        } else {
            Util.sendPlayerMessage(playerHandler, "§cVocê tem certeza que quer se matar? §6Se sim, use o comando novamente; mas a vida é tão boa.");
            usedCommand.add(playerName);
            BukkitScheduler.runTaskLater(() -> usedCommand.remove(playerName), SUICIDE_CONFIRMATION_TICKS);
        }
        return true;
    }
}
