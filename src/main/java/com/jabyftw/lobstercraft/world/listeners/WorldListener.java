package com.jabyftw.lobstercraft.world.listeners;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.world.util.location_util.ChunkLocation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.sql.SQLException;

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
public class WorldListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldCreate(WorldLoadEvent event) {
        // Check if world is ignored
        if (!LobsterCraft.worldService.isWorldIgnored(event.getWorld()))
            try {
                // Load world
                LobsterCraft.worldService.updateWorldCache();
                LobsterCraft.logger.info("Loaded post-initialization world " + event.getWorld().getName());
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // Check if world is ignored
        if (!LobsterCraft.worldService.isWorldIgnored(event.getWorld())) {
            // Get our ChunkLocation
            ChunkLocation chunkLocation = new ChunkLocation(event.getChunk());

            // TODO add to unload lists on common block protection
        }
    }
}
