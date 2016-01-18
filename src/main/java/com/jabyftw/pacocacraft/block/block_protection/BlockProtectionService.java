package com.jabyftw.pacocacraft.block.block_protection;

import com.jabyftw.pacocacraft.PacocaCraft;
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
@SuppressWarnings("Convert2streamapi")
public class BlockProtectionService implements ServerService {

    public static WorldController worldController;
    public static ChunkController chunkController;

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(new WorldListener(), PacocaCraft.pacocaCraft);

        worldController = new WorldController();
        chunkController = new ChunkController();

        worldController.updateWorldList();

        PacocaCraft.logger.info("Enabled " + getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
    }
}
