package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.NumberConversions;

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
public class BlockLocation {

    private final ChunkLocation chunkLocation;
    private final byte x, z;
    private final short y;

    public BlockLocation(@NotNull final ChunkLocation chunkLocation, byte x, short y, byte z) {
        this.chunkLocation = chunkLocation;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockLocation(@NotNull final BlockLocation blockLocation) {
        this.chunkLocation = blockLocation.getChunkLocation();
        this.x = blockLocation.getRelativeX();
        this.y = blockLocation.getY();
        this.z = blockLocation.getRelativeZ();
    }

    public BlockLocation(@NotNull final Location location) {
        this.chunkLocation = new ChunkLocation(location.getChunk());
        this.x = (byte) (location.getBlockX() - (location.getChunk().getX() * 16) - ((location.getBlockX() < 0) ? 1 : 0));
        this.y = (short) location.getBlockY();
        this.z = (byte) (location.getBlockZ() - (location.getChunk().getZ() * 16) - ((location.getBlockZ() < 0) ? 1 : 0));
    }

    public ChunkLocation getChunkLocation() {
        return chunkLocation;
    }

    public World getWorld() {
        return chunkLocation.getWorld();
    }

    public byte getRelativeX() {
        return x;
    }

    public byte getRelativeZ() {
        return z;
    }

    /**
     * This will return block's X coordinate.<br>
     * <p>
     * This value is stored as relative-to-chunk coordinates, given by:<br>
     * relativeX = blockX - (chunkX * 16) - ((blockX < 0) ? 1 : 0)<br>
     * <p>
     * Doing the reverse, we have that:<br>
     * blockX = relativeX + (chunkX * 16) + ((blockX < 0 ?) 1 : 0)<br>
     * <p>
     * Note that blockX will simply check if the coordinates is on the negative side of the world, on our code we check that using chunkX. The result will be the
     * same since negative chunks will give negative coordinates too.
     *
     * @return block's X relative to the world (not relative to chunk)
     */
    public int getX() {
        return x + (chunkLocation.getChunkX() * 16) + (chunkLocation.getChunkX() < 0 ? 1 : 0);
    }

    public short getY() {
        return y;
    }

    /**
     * @return block's Z relative to the world (not relative to chunk)
     * @see BlockLocation#getX()
     */
    public int getZ() {
        return z + (chunkLocation.getChunkZ() * 16) + (chunkLocation.getChunkZ() < 0 ? 1 : 0);
    }

    public double distance(@NotNull final BlockLocation blockLocation) {
        return Math.sqrt(distanceSquared(blockLocation));
    }

    public double distanceSquared(@NotNull final BlockLocation blockLocation) {
        return NumberConversions.square(blockLocation.getX() - getX()) +
                NumberConversions.square(blockLocation.y - y) + // As y don't need transformation, the raw value should be fine
                NumberConversions.square(blockLocation.getZ() - getZ());
    }

    public double distanceXZ(@NotNull final BlockLocation blockLocation) {
        return Math.sqrt(distanceXZSquared(blockLocation));
    }

    public double distanceXZSquared(@NotNull final BlockLocation blockLocation) {
        return NumberConversions.square(blockLocation.getX() - getX()) +
                NumberConversions.square(blockLocation.getZ() - getZ());
    }

    public Location toBukkitLocation() {
        return new Location(getChunkLocation().getWorld(), getX(), getY(), getZ());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(5, 19)
                .append(chunkLocation.hashCode()) // This will make each block unique even with the same relative coordinates
                .append(x)
                .append(y)
                .append(z)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof BlockLocation &&
                ((BlockLocation) obj).chunkLocation.equals(chunkLocation) && // This will make each block unique even with the same relative coordinates
                ((BlockLocation) obj).x == x &&
                ((BlockLocation) obj).y == y &&
                ((BlockLocation) obj).z == z;
    }

    @Override
    public String toString() {
        return Util.appendStrings("x=", getX(), ", y=", getY(), ", z=", getZ(), " @ world='", chunkLocation.getWorld().getName(), "'");
    }
}
