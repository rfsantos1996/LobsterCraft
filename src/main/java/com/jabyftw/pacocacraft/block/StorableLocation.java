package com.jabyftw.pacocacraft.block;

import com.jabyftw.Util;
import com.sun.istack.internal.NotNull;
import org.bukkit.Location;
import org.bukkit.util.NumberConversions;

import java.text.DecimalFormat;

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
public class StorableLocation {

    private String worldName;
    private double x, y, z;
    private float pitch, yaw;

    /**
     * Create a storable location (not Bukkit dependent) given Bukkit Location
     *
     * @param location Bukkit's location for values
     */
    public StorableLocation(@NotNull Location location) {
        // Set world
        this.worldName = location.getWorld().getName().toLowerCase();
        // Set coordinates
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        // Set direction
        this.pitch = location.getPitch();
        this.yaw = location.getYaw();
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    public int getBlockX() {
        return NumberConversions.floor(x);
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public int getBlockY() {
        return NumberConversions.floor(y);
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public int getBlockZ() {
        return NumberConversions.floor(z);
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    // From CraftWorld.java:
    public int getChunkX() {
        return getBlockX() >> 4;
    }

    public int getChunkZ() {
        return getBlockZ() >> 4;
    }

    public static Location toLocation(@NotNull StorableLocation location) {
        return new Location(Util.parseToWorld(location.worldName), location.x, location.y, location.z, location.yaw, location.pitch);
    }

    @Override
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        return "{" + worldName + ", " + decimalFormat.format(getX()) + ", " + decimalFormat.format(getY()) + ", " + decimalFormat.format(getZ()) + "}";
    }
}
