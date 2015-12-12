package com.jabyftw.pacocacraft.location;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.location.commands.*;
import com.jabyftw.pacocacraft.util.ServerService;
import org.bukkit.Bukkit;

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
public class TeleportService implements ServerService {

    public static WorldCommand worldCommand;

    @Override
    public void onEnable() {
        Bukkit.getPluginCommand("teleport").setExecutor(new TeleportCommand());
        Bukkit.getPluginCommand("teleporthere").setExecutor(new TeleportHereCommand());
        Bukkit.getPluginCommand("world").setExecutor((worldCommand = new WorldCommand()));
        Bukkit.getPluginCommand("spawn").setExecutor(new SpawnCommand());
        Bukkit.getPluginCommand("spawnset").setExecutor(new SpawnSetCommand());
        Bukkit.getPluginCommand("back").setExecutor(new BackCommand());
        Bukkit.getPluginCommand("top").setExecutor(new TopCommand());

        Bukkit.getServer().getPluginManager().registerEvents(new TeleportListener(), PacocaCraft.pacocaCraft);
        PacocaCraft.logger.info("Enabled " + getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
    }
}
