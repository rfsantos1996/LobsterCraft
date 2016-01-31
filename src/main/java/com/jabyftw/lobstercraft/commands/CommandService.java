package com.jabyftw.lobstercraft.commands;

import com.jabyftw.lobstercraft.commands.login.LoginCommand;
import com.jabyftw.lobstercraft.commands.login.RegisterCommand;
import com.jabyftw.lobstercraft.commands.player.GamemodeCommand;
import com.jabyftw.lobstercraft.commands.protection.BuildModeCommand;
import com.jabyftw.lobstercraft.util.Service;
import org.bukkit.Bukkit;
import org.bukkit.Server;

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
public class CommandService extends Service {

    @Override
    public boolean onEnable() {
        Server server = Bukkit.getServer();
        {
            // login
            server.getPluginCommand("login").setExecutor(new LoginCommand());
            server.getPluginCommand("register").setExecutor(new RegisterCommand());
            // player
            server.getPluginCommand("gamemode").setExecutor(new GamemodeCommand());
            // protection
            server.getPluginCommand("buildmode").setExecutor(new BuildModeCommand());
        }
        return true;
    }

    @Override
    public void onDisable() {

    }
}
