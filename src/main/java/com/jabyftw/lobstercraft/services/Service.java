package com.jabyftw.lobstercraft.services;

import com.jabyftw.lobstercraft.LobsterCraft;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Email address: rafael.sartori96@gmail.com
 */
public abstract class Service implements Listener {

    protected Service() {
        Bukkit.getServer().getPluginManager().registerEvents(this, LobsterCraft.plugin);
    }

    public abstract void onDisable();

}
