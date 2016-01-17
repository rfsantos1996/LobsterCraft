package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.WeatherType;

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
public class PlayerWeatherCommand extends CommandExecutor {

    public PlayerWeatherCommand() {
        super(PacocaCraft.pacocaCraft, "pweather", Permissions.PLAYER_INDIVIDUAL_WEATHER_CHANGE, "§6Permite ao jogador mudar o clima individual", "§c/pweather (§4tipo de clima§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onChangeWeather(PlayerHandler playerHandler) {
        playerHandler.getPlayer().resetPlayerWeather();
        Util.sendPlayerMessage(playerHandler, "§cClima restaurado!");
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onChangeWeather(PlayerHandler playerHandler, WeatherType weatherType) {
        playerHandler.getPlayer().setPlayerWeather(weatherType);
        Util.sendPlayerMessage(playerHandler, "§6Clima atualizado! Use §c/pweather§6 para restaurar.");
        return true;
    }
}
