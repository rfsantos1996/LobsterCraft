package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.Permissions;
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
        super("pweather", Permissions.PLAYER_PLAYER_WEATHER.toString(), "Permite ao jogador mudar o clima individual", "/pweather (tipo de clima)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onChangeWeather(OnlinePlayer onlinePlayer) {
        onlinePlayer.getPlayer().resetPlayerWeather();
        onlinePlayer.getPlayer().sendMessage("§cClima restaurado!");
        return true;
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onChangeWeather(OnlinePlayer onlinePlayer, WeatherType weatherType) {
        onlinePlayer.getPlayer().setPlayerWeather(weatherType);
        onlinePlayer.getPlayer().sendMessage("§6Clima atualizado! Use §c/pweather§6 para restaurar.");
        return true;
    }
}
