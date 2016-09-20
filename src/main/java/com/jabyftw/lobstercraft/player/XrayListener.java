package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.BlockLocation;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.TimeUnit;

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
class XrayListener implements Listener {

    private static final List<Material> oreMaterials = Arrays.asList(
            Material.COAL_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.EMERALD_BLOCK,
            Material.GOLD_BLOCK,
            Material.IRON_BLOCK,
            Material.LAPIS_BLOCK,
            Material.QUARTZ_BLOCK,
            Material.REDSTONE_BLOCK
    );
    private static final double
            DISTANCE_CONSIDER_SAME_MINE_SQUARED = NumberConversions.square(LobsterCraft.configuration.getDouble(ConfigurationValues.XRAY_DISTANCE_CONSIDER_SAME_MINE.toString())),
            DISTANCE_TO_MERGE_NEAR_ORES_SQUARED = NumberConversions.square(7);
    private static final long
            TIME_TO_CONSIDER_SAME_MINE = TimeUnit.SECONDS.toMillis(LobsterCraft.configuration.getLong(ConfigurationValues.XRAY_TIME_TO_CONSIDER_SAME_MINING_SECONDS.toString()));
    private static final Vector
            versorX = new Vector(1, 0, 0),
            versorY = new Vector(0, 1, 0),
            versorZ = new Vector(0, 0, 1);

    /**
     * Check, based on the turns made on mining sessions, probable locations for xray using.<br>
     * Note: should NOT run asynchronously without synchronization of onlinePlayer.oreLocationHistory
     *
     * @param onlinePlayer player that might be using xray
     * @return a list of possible blocks
     */
    @SuppressWarnings("unchecked")
    public static Set<OreBlockLocation> checkProbableXrayLocations(@NotNull OnlinePlayer onlinePlayer) {
        HashSet<OreBlockLocation> locationsXrayLike = new HashSet<>();

        ArrayList<Float>[] angleValues = new ArrayList[]{new ArrayList<Float>(), new ArrayList<Float>(), new ArrayList<Float>()}; // x, y, z respectively
        Iterator<OreBlockLocation> iterator = onlinePlayer.oreLocationHistory.iterator();

        if (iterator.hasNext()) {
            OreBlockLocation currentOreLocation = iterator.next();
            // If block wouldn't be considered on the same mine, remove it already
            if (System.currentTimeMillis() - currentOreLocation.getBreakDate() > TIME_TO_CONSIDER_SAME_MINE)
                iterator.remove();

            // Iterate through pool, setting current ore location after calculations
            while (iterator.hasNext()) {
                OreBlockLocation nextOreLocation = iterator.next();

                // Check if it is on the same mining session
                if (Math.abs(nextOreLocation.getBreakDate() - currentOreLocation.getBreakDate()) <= TIME_TO_CONSIDER_SAME_MINE &&
                        currentOreLocation.distanceSquared(nextOreLocation) <= DISTANCE_CONSIDER_SAME_MINE_SQUARED) {
                    Vector currentVector = currentOreLocation.toBukkitLocation().toVector();
                    Vector nextVector = nextOreLocation.toBukkitLocation().toVector();

                    // Next - current = difference, pointing to the next vector
                    Vector subtraction = nextVector.subtract(currentVector);//.normalize();

                    // Calculate the angle between the difference vector and x, y and z axis (don't need to normalize)
                    float
                            angleX = subtraction.angle(versorX),
                            angleY = subtraction.angle(versorY),
                            angleZ = subtraction.angle(versorZ);

                    // Add the absolute value of the angle
                    angleValues[0].add(Math.abs(angleX));
                    angleValues[1].add(Math.abs(angleY));
                    angleValues[2].add(Math.abs(angleZ));
                } else if (isXrayLike(angleValues)) {
                    // Add to list and repeat with next mining session
                    locationsXrayLike.add(currentOreLocation);
                    for (ArrayList<Float> arrayList : angleValues)
                        arrayList.clear();
                }
                // If not on same mining session, don't calculate anything and wait for next location
                currentOreLocation = nextOreLocation;
                // Remove calculated blocks
                iterator.remove();
            }
        }

        return Collections.unmodifiableSet(locationsXrayLike);
    }

    private static boolean isXrayLike(ArrayList<Float>[] angleList) {
        float[] averageVariation = new float[3];

        // Calculate the average variation
        for (int index = 0; index < angleList.length; index++) {
            // Check if there are blocks on the list
            if (angleList[index].isEmpty()) return false;

            // Calculate the average absolute variation of the absolute angles between its axis
            float currentAverage = 0;

            // Sum the absolute difference of absolute angles
            for (int current = 0; current < angleList[index].size() - 1; current++)
                for (int next = 1; next < angleList[index].size(); next++)
                    currentAverage += Math.abs(angleList[index].get(current) - angleList[index].get(next));

            // Divide by the size of elements
            currentAverage /= (float) angleList[index].size();
            averageVariation[index] = currentAverage;
        }

        // Check if it is xray-like (high variations)
        // TODO: debug: I need values to check this
        LobsterCraft.logger.info(Util.appendStrings("Average delta: ",
                "avgX = ", Util.formatDecimal(averageVariation[0]), ", ",
                "avgY = ", Util.formatDecimal(averageVariation[1]), ", ",
                "avgZ = ", Util.formatDecimal(averageVariation[2]), " | ",
                "2*PI = ", Util.formatDecimal(Math.PI * 2.0D)));
        return false;
    }

    /*
     * Listen to BreakEvent
     */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBreakEvent(BlockBreakEvent event) {
        OnlinePlayer onlinePlayer;
        // Check if material is a ore and player is online
        if (oreMaterials.contains(event.getBlock().getType()) &&
                (onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), OnlinePlayer.OnlineState.LOGGED_IN)) != null) {
            // Get blocks
            OreBlockLocation currentBlockLocation = new OreBlockLocation(event.getBlock().getLocation());
            OreBlockLocation lastOreLocation = onlinePlayer.oreLocationHistory.getLast();

            // Check if last ore location is close so we can create an average
            if (lastOreLocation != null && lastOreLocation.getWorld().equals(currentBlockLocation)
                    && lastOreLocation.distanceSquared(currentBlockLocation) <= DISTANCE_TO_MERGE_NEAR_ORES_SQUARED) {

                // Update current location to be the average between blocks
                OreBlockLocation averageLocation = new OreBlockLocation(new Location(
                        currentBlockLocation.getWorld(),
                        (currentBlockLocation.getX() + lastOreLocation.getX()) / 2.0D,
                        (currentBlockLocation.getY() + lastOreLocation.getY()) / 2.0D,
                        (currentBlockLocation.getZ() + lastOreLocation.getZ()) / 2.0D
                ));

                // Update last ore on list
                onlinePlayer.oreLocationHistory.pollLast();
                currentBlockLocation = averageLocation;
            }

            // Add current location
            onlinePlayer.oreLocationHistory.add(currentBlockLocation);
        }
    }

    /*
     * OreBlockLocation class
     */

    protected static class OreBlockLocation extends BlockLocation {

        private final long breakDate;

        public OreBlockLocation(@NotNull Location location) {
            super(location);
            this.breakDate = System.currentTimeMillis();
        }

        public long getBreakDate() {
            return breakDate;
        }
    }
}
