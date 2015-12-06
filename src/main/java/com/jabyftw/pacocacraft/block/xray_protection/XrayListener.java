package com.jabyftw.pacocacraft.block.xray_protection;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.pacocacraft.player.PlayerHandler;
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
public class XrayListener implements Listener {

    private static final double MERGE_DISTANCE = NumberConversions.square(5); // distance is squared
    private static final List<Material> oreList = Arrays.asList(
            Material.COAL_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.EMERALD_BLOCK,
            Material.GOLD_BLOCK,
            Material.IRON_BLOCK,
            Material.LAPIS_BLOCK,
            Material.QUARTZ_BLOCK,
            Material.REDSTONE_BLOCK
    );

    // monitor because the event won't check
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent breakEvent) {
        if(oreList.contains(breakEvent.getBlock().getType())) {
            PlayerHandler playerHandler = PacocaCraft.getPlayerHandler(breakEvent.getPlayer());
            Location blockLocation = breakEvent.getBlock().getLocation();

            // Get last location
            OreLocation lastOreLocation = playerHandler.getOreLocationHistory().getLast();
            Location location = lastOreLocation.getLocation();

            // Check if last ore location is close (<5 blocks)
            if(location.getWorld().equals(blockLocation) && location.distanceSquared(blockLocation) <= MERGE_DISTANCE) {
                // Update current location to be the average between blocks
                Location currentLocation = new Location(
                        blockLocation.getWorld(),
                        (blockLocation.getBlockX() + location.getBlockX()) / 2,
                        (blockLocation.getBlockY() + location.getBlockY()) / 2,
                        (blockLocation.getBlockZ() + location.getBlockZ()) / 2
                );
                lastOreLocation.setLocation(currentLocation);
                //playerHandler.getOreLocationHistory().removeLast(); // Not needed anymore
            }

            // Add current location (if the block is broken, player is registered)
            playerHandler.getOreLocationHistory().add(new OreLocation(playerHandler.getProfile(UserProfile.class).getPlayerId(), blockLocation));
        }
    }
}
