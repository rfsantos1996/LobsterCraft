package com.jabyftw.pacocacraft2.block.block_protection;

import com.jabyftw.pacocacraft.ConfigValue;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft2.PacocaCraft;
import com.jabyftw.pacocacraft2.block.block_protection.util.ProtectedWorldLocation;
import com.jabyftw.pacocacraft2.location.util.BlockLocation;
import com.jabyftw.pacocacraft2.location.util.ChunkLocation;
import com.sun.istack.internal.NotNull;
import org.bukkit.util.NumberConversions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
public class WorldBlockController {

    public static final int
            DEFAULT_LOAD_SIZE = PacocaCraft.config.getInt(ConfigValue.BLOCK_LOAD_CHUNK_RANGE.getPath()),
            MINIMUM_LOAD_SIZE = 1;
    private static final long CHUNK_LOAD_PERIOD = PacocaCraft.config.getLong(ConfigValue.BLOCK_LOAD_CHUNK_PERIOD.getPath());

    // Need synchronization in iterators
    private final Set<ChunkLocation> pendingLoadings = Collections.synchronizedSet(new LinkedHashSet<>());
    // Need  synchronization in putAll, removeAll
    private final ConcurrentHashMap<ChunkLocation, HashSet<ProtectedWorldLocation>> blockStorage = new ConcurrentHashMap<>();

    public WorldBlockController() {
        BukkitScheduler.runTaskTimerAsynchronously(new LoadingTask(), CHUNK_LOAD_PERIOD, CHUNK_LOAD_PERIOD);
    }

    /**
     * Get near blocks if world has loaded the near chunks
     *
     * @param blockLocation  given block location to distance for
     * @param searchDistance the search distance
     * @return null if chunk is loading; a set of blocks otherwise
     */
    public Set<ProtectedWorldLocation> getNearBlocks(@NotNull final BlockLocation blockLocation, double searchDistance) {
        final ConcurrentSkipListSet<ProtectedWorldLocation> worldLocations = new ConcurrentSkipListSet<>();
        int searchChunkRange = NumberConversions.ceil((searchDistance * 2d) / 16d);

        // If chunk is ready, create set and search blocks
        if (loadNearChunks(blockLocation.getChunkLocation(), searchChunkRange)) {
            long startTime = System.nanoTime();
            final double searchDistanceSquared = NumberConversions.square(searchDistance);

            // Iterate through chunks getting all blocks
            blockLocation.getChunkLocation().getNearChunks(searchChunkRange).parallelStream() // the search distance won't allow missing blocks
                    .forEach((chunkLocation) ->
                            worldLocations.addAll(
                                    blockStorage.get(chunkLocation).stream()
                                            .filter((block) -> block.distanceSquared(blockLocation) <= searchDistanceSquared)
                                            .collect(Collectors.toSet())
                            )
                    );

            // Announce how much it took
            long deltaTime = System.nanoTime() - startTime;
            String format = new DecimalFormat("0.0000").format(deltaTime / (double) TimeUnit.MILLISECONDS.toNanos(1));
            System.out.println("It took " + format + "ms to search for " + worldLocations.size() + " blocks.");
        } else
            // Will return null if chunk is still loading
            return null;

        return worldLocations;
    }

    public boolean loadNearChunks(@NotNull final ChunkLocation chunkLocation, int searchChunkRange) {
        // Request the non-loaded pieces
        synchronized (pendingLoadings) {
            pendingLoadings.addAll(
                    chunkLocation.getNearChunks(Math.max(DEFAULT_LOAD_SIZE, searchChunkRange)).stream()
                            .filter(location -> !blockStorage.containsKey(location)).collect(Collectors.toList())
            );
        }

        boolean missingPiece = false;
        // Check if the needed chunks are loaded
        for (ChunkLocation location : chunkLocation.getNearChunks(MINIMUM_LOAD_SIZE)) {
            if (!blockStorage.containsKey(location))
                missingPiece = true;
        }

        // If it isn't missing, return that you can change the environment
        return !missingPiece;
    }

    private class LoadingTask implements Runnable {

        private final int CHUNK_LIMIT_PER_RUN = PacocaCraft.config.getInt(ConfigValue.BLOCK_LOAD_CHUNK_LIMIT_PER_RUN.getPath());
        private Connection connection;

        public LoadingTask() {
            try {
                connection = PacocaCraft.dataSource.getConnection();
            } catch (SQLException e) {
                e.printStackTrace();
                PacocaCraft.logger.severe("Server couldn't start WorldBlockController's Chunk Loader.");
            }
        }

        @Override
        public void run() {
            // Before everything, check if it needs something
            if (!pendingLoadings.isEmpty()) {
                // Check if it is alive
                try {
                    if (!connection.isValid(1)) {
                        connection.close();
                        connection = PacocaCraft.dataSource.getConnection();
                    }
                } catch (SQLException ignored) {
                }

                try {
                    // Initialize query
                    StringBuilder stringBuilder = new StringBuilder("SELECT * FROM minecraft.world_blocks WHERE (worlds_worldId, chunkX, chunkZ) IN (");

                    // Append all pending loading
                    synchronized (pendingLoadings) {
                        int listBounds = Math.min(CHUNK_LIMIT_PER_RUN, pendingLoadings.size());
                        int index = 0;

                        // Iterate through a linked set until bounds are reached
                        for (ChunkLocation chunkLocation : pendingLoadings) {
                            // Append all information needed
                            stringBuilder
                                    .append('(')
                                    .append(chunkLocation.getWorldId()).append(", ")
                                    .append(chunkLocation.getChunkX()).append(", ")
                                    .append(chunkLocation.getChunkZ())
                                    .append(')');


                            // Add a comma if we aren't finished, else break for-each
                            if (index++ != listBounds - 1) stringBuilder.append(", ");
                            else break;
                        }
                    }

                    // Close query
                    stringBuilder.append(");");

                    // Prepare statement
                    PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());


                    // Execute statement
                    ResultSet resultSet = preparedStatement.executeQuery();

                    // Prepare HashMap for insertion
                    HashMap<ChunkLocation, HashSet<ProtectedWorldLocation>> loadedBlocks = new HashMap<>();

                    // Pass through all results
                    while (resultSet.next()) {
                        ChunkLocation chunkLocation = new ChunkLocation(resultSet.getLong("worlds_worldId"), resultSet.getInt("chunkX"), resultSet.getInt("chunkZ"));

                        // Create block
                        ProtectedWorldLocation blockLocation = new ProtectedWorldLocation(
                                chunkLocation,
                                resultSet.getByte("blockX"),
                                resultSet.getShort("blockY"),
                                resultSet.getByte("blockZ"),
                                resultSet.getLong("user_ownerId")
                        );

                        // Make sure chunk is loaded
                        if (!loadedBlocks.containsKey(chunkLocation))
                            loadedBlocks.put(chunkLocation, new HashSet<>());

                        // Add block to storage
                        loadedBlocks.get(chunkLocation).add(blockLocation);
                    }

                    // After finishing up, add it to the normal chunk list and remove it from pending one
                    synchronized (blockStorage) {
                        blockStorage.putAll(loadedBlocks);
                        // This must be synchronized here because it may cause some problem when checking blocks right after loading them
                        synchronized (pendingLoadings) {
                            pendingLoadings.removeAll(loadedBlocks.keySet());
                        }
                    }

                    // Close everything
                    resultSet.close();
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
