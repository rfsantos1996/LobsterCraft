package com.jabyftw.lobstercraft.world.xray_protection;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.world.util.location_util.OreBlockLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.NumberConversions;

import java.util.Arrays;
import java.util.List;

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
public class XrayListener implements Listener {

    private final double MERGE_DISTANCE = NumberConversions.square(7); // distance is squared
    private final List<Material> oreMaterialList = Arrays.asList(
            Material.COAL_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.EMERALD_BLOCK,
            Material.GOLD_BLOCK,
            Material.IRON_BLOCK,
            Material.LAPIS_BLOCK,
            Material.QUARTZ_BLOCK,
            Material.REDSTONE_BLOCK
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakEvent(BlockBreakEvent event) {
        // First of all, check if its Material is an ore
        if (oreMaterialList.contains(event.getBlock().getType())) {
            PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer());
            Location blockLocation = event.getBlock().getLocation();

            // Get last location
            OreBlockLocation lastOreBlockLocation = playerHandler.getOreLocationHistory().getLast();
            Location lastOreLocation;

            // Check if last ore location is close
            if (lastOreBlockLocation != null
                    && (lastOreLocation = lastOreBlockLocation.getLocation()).getWorld().equals(blockLocation)
                    && lastOreLocation.distanceSquared(blockLocation) <= MERGE_DISTANCE) {
                // Update current location to be the average between blocks
                Location currentLocation = new Location(
                        blockLocation.getWorld(),
                        (blockLocation.getBlockX() + lastOreLocation.getBlockX()) / 2,
                        (blockLocation.getBlockY() + lastOreLocation.getBlockY()) / 2,
                        (blockLocation.getBlockZ() + lastOreLocation.getBlockZ()) / 2
                );

                // Update last ore on list
                lastOreBlockLocation.setLocation(currentLocation);
            } else {
                // Add current location (if the block is broken, player is registered)
                playerHandler.getOreLocationHistory().add(new OreBlockLocation(playerHandler, blockLocation));
            }
        }
    }
}
