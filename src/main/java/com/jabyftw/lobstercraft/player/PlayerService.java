package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.listeners.CustomEventsListener;
import com.jabyftw.lobstercraft.player.listeners.PlayerListener;
import com.jabyftw.lobstercraft.player.listeners.TeleportListener;
import com.jabyftw.lobstercraft.util.Service;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
public class PlayerService extends Service {

    public PlayerListener playerListener;

    @Override
    public boolean onEnable() {
        PluginManager pluginManager = Bukkit.getServer().getPluginManager();
        { // Register everything
            pluginManager.registerEvents(new TeleportListener(), LobsterCraft.lobsterCraft);
            pluginManager.registerEvents(playerListener = new PlayerListener(), LobsterCraft.lobsterCraft);
            pluginManager.registerEvents(new CustomEventsListener(), LobsterCraft.lobsterCraft); // handles custom events
        }
        return true;
    }

    @Override
    public void onDisable() {
    }
}
