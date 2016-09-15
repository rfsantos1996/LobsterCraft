package com.jabyftw.lobstercraft.services;

import com.jabyftw.lobstercraft.player.PlayerHandlerService;
import com.jabyftw.lobstercraft.world.CityService;
import com.jabyftw.lobstercraft.world.WorldService;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.sql.SQLException;

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
public class ServicesManager implements Listener {

    // Services
    public final PlayerHandlerService playerHandlerService;
    public final CityService cityService;
    public final WorldService worldService;

    /**
     * Initialize services
     */
    public ServicesManager() throws SQLException, IOException, ClassNotFoundException {
        playerHandlerService = new PlayerHandlerService();
        cityService = new CityService();
        // WorldService requires CityService to be loaded
        worldService = new WorldService();
    }

    /**
     * This method will close every service
     */
    public void onDisable() {
        playerHandlerService.onDisable();
    }
}
