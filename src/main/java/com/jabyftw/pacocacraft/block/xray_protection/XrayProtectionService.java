package com.jabyftw.pacocacraft.block.xray_protection;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.ServerService;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
public class XrayProtectionService implements ServerService {

    private static final long TIME_TO_CONSIDER_SAME_MINE = PacocaCraft.config.getLong(ConfigValue.XRAY_TIME_TO_CONSIDER_SAME_MINE.getPath());
    private static final double DISTANCE_NEEDED_TO_CONSIDER_SAME_MINE = NumberConversions.square(PacocaCraft.config.getDouble(ConfigValue.XRAY_DISTANCE_TO_CONSIDER_SAME_MINE.getPath()));
    protected final static Vector
            versorX = new Vector(1, 0, 0).normalize(),
            versorY = new Vector(0, 1, 0).normalize(),
            versorZ = new Vector(0, 0, 1).normalize();

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(new XrayListener(), PacocaCraft.pacocaCraft);
        PacocaCraft.logger.info("Enabled " + getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
    }

    public static List<OreLocation> checkProbableXrayLocations(@NotNull PlayerHandler playerHandler) {
        ArrayList<OreLocation> likelyXrayLocation = new ArrayList<>();

        @SuppressWarnings("unchecked")
        ArrayList<Float>[] angleValues = new ArrayList[]{new ArrayList<Double>(), new ArrayList<Double>(), new ArrayList<Double>()}; // x, y, z respectively
        Iterator<OreLocation> iterator = playerHandler.getOreLocationHistory().iterator();

        if(iterator.hasNext()) {
            OreLocation currentOreLocation = iterator.next();

            // Iterate through pool, setting current ore location after calculations
            while(iterator.hasNext()) {
                OreLocation nextOreLocation = iterator.next();

                // Check if it is on the same mining session
                if(TimeUnit.MILLISECONDS.toSeconds(Math.abs(nextOreLocation.getDate() - currentOreLocation.getDate())) <= TIME_TO_CONSIDER_SAME_MINE &&
                        currentOreLocation.getLocation().distanceSquared(nextOreLocation.getLocation()) <= DISTANCE_NEEDED_TO_CONSIDER_SAME_MINE) {
                    Vector currentVector = currentOreLocation.getLocation().toVector();
                    Vector nextVector = nextOreLocation.getLocation().toVector();

                    // Next - current = difference, pointing to the next vector
                    Vector subtraction = nextVector.subtract(currentVector);//.normalize();

                    // Calculate the angle between the difference vector and x, y and z axis [I don't need to normalize, I believe]
                    float angleX = subtraction.angle(versorX), angleY = subtraction.angle(versorY), angleZ = subtraction.angle(versorZ);

                    // Add the absolute value of the angle
                    angleValues[0].add(Math.abs(angleX));
                    angleValues[1].add(Math.abs(angleY));
                    angleValues[2].add(Math.abs(angleZ));
                } else if(isXrayLike(angleValues)) {
                    // Add to list and repeat with next mining session
                    likelyXrayLocation.add(currentOreLocation);
                    for(ArrayList arrayList : angleValues)
                        arrayList.clear();
                }
                // If not on same mining session (else), don't calculate anything and wait for next location
                currentOreLocation = nextOreLocation;
            }
        }

        return likelyXrayLocation;
    }

    private static boolean isXrayLike(ArrayList<Float>[] angleList) {
        float[] averageVariation = new float[3];

        // Get average delta
        for(int index = 0; index < angleList.length; index++) {
            // Calculate the average absolute variation of the absolute angles between its axis
            float currentAverage = 0;

            // Check for existence conditions
            if(angleList[index].isEmpty())
                return false; // It isn't xray-like because there are no blocks here

            // Absolute of the difference of absolutes
            for(int i = 0; i < angleList[index].size() - 1; i++) {
                for(int i2 = 1; i2 < angleList[index].size(); i2++) {
                    currentAverage += Math.abs(angleList[index].get(i) - angleList[index].get(i2));
                }
            }

            // Divide by the size of elements
            currentAverage /= (float) angleList[index].size();
            averageVariation[index] = currentAverage;
        }

        // Check if it is xray-like (high delta variations)

        // TODO: DEBUG, I need values to check this
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        PacocaCraft.logger.info("Average delta: " +
                "deltaX=" + decimalFormat.format(averageVariation[0]) + ", " +
                "deltaY=" + decimalFormat.format(averageVariation[1]) + ", " +
                "deltaZ=" + decimalFormat.format(averageVariation[2]) + "; " +
                "2.pi = " + decimalFormat.format(Math.PI * 2d));
        return false;
    }
}
