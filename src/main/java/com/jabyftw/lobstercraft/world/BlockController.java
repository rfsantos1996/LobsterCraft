package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Service;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.util.ProtectionType;
import com.jabyftw.lobstercraft.world.util.location_util.*;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.util.NumberConversions;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
public class BlockController extends Service {

    private final DecimalFormat formatter = new DecimalFormat("0.000");

    private static final int MINIMUM_LOAD_SIZE, DEFAULT_LOAD_SIZE, LIMIT_CHUNK_LOAD_SIZE = LobsterCraft.config.getInt(ConfigValue.WORLD_CHUNK_LOAD_LIMIT.getPath());
    private static final long CHUNK_LOAD_PERIOD = LobsterCraft.config.getLong(ConfigValue.WORLD_CHUNK_LOAD_PERIOD.getPath());

    // Check the load sizes
    static {
        int minimumLoadSize = 1;
        for (ProtectionType protectionType : ProtectionType.values()) {
            minimumLoadSize = Math.max(Util.getMinimumChunkSize(protectionType.getSearchDistance()), minimumLoadSize);
        }
        MINIMUM_LOAD_SIZE = minimumLoadSize;
        DEFAULT_LOAD_SIZE = NumberConversions.ceil(minimumLoadSize * LobsterCraft.config.getDouble(ConfigValue.WORLD_DEFAULT_LOAD_SIZE_MULTIPLIER.getPath()));
    }

    private final Set<ChunkLocation>
            pendingLoading = Collections.synchronizedSet(new LinkedHashSet<>()),
            pendingUnloading = Collections.synchronizedSet(new LinkedHashSet<>());
    private final ConcurrentHashMap<ChunkLocation, HashMap<BlockLocation, ProtectedBlockLocation>> blockStorage = new ConcurrentHashMap<>();

    private Connection connection;

    @Override
    public boolean onEnable() {
        LobsterCraft.logger.info("MinimumLoadSize=" + MINIMUM_LOAD_SIZE + ", " +
                "DefaultLoadSize=" + DEFAULT_LOAD_SIZE + ", " +
                "defaultChunksLoaded=" + Util.getNumberOfChunksLoaded(DEFAULT_LOAD_SIZE)
        );

        Bukkit.getServer().getPluginManager().registerEvents(new ChunkListener(), LobsterCraft.lobsterCraft);

        try {
            checkConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        BukkitScheduler.runTaskTimerAsynchronously(new ChunkLoader(), 20L, CHUNK_LOAD_PERIOD);
        BukkitScheduler.runTaskTimerAsynchronously(new ChunkUnloader(), 20L, CHUNK_LOAD_PERIOD);

        return true;
    }

    @Override
    public void onDisable() {
    }

    @SuppressWarnings("unchecked")
    public <T extends ProtectedBlockLocation> T addBlock(@NotNull final Location location, @NotNull final Class<T> protectionTypeClass) {
        try {
            T protectedBlockLocation = (T) protectionTypeClass.getDeclaredConstructor(location.getClass()).newInstance(location);

            T currentValue = (T) blockStorage.get(protectedBlockLocation.getChunkLocation())
                    .putIfAbsent(protectedBlockLocation, protectedBlockLocation);

            // Return, safely, a protected block of given class
            return currentValue == null ? protectedBlockLocation : currentValue;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't create new instance for " + protectionTypeClass.getSimpleName());
        }
    }

    public <T extends ProtectedBlockLocation> T getBlock(@NotNull final Location location) {
        return getBlock(new BlockLocation(location));
    }

    @SuppressWarnings("unchecked")
    public <T extends ProtectedBlockLocation> T getBlock(@NotNull final BlockLocation blockLocation) {
        return (T) blockStorage.get(blockLocation.getChunkLocation()).get(blockLocation);
    }

    public Set<ProtectedBlockLocation> getProtectedBlocks(@NotNull final Location location) {
        return getProtectedBlocks(new BlockLocation(location));
    }

    public Set<ProtectedBlockLocation> getProtectedBlocks(@NotNull final BlockLocation nearBlock) {
        // If it isn't loaded, return
        if (!loadNearChunks(nearBlock.getChunkLocation())) return null;

        HashSet<ProtectedBlockLocation> protectedLocations = new HashSet<>();

        // Iterate through all near chunks
        for (ChunkLocation chunkLocation : nearBlock.getChunkLocation().getNearChunks(MINIMUM_LOAD_SIZE))
            // Iterate through all blocks on these chunks
            for (ProtectedBlockLocation protectedBlockLocation : blockStorage.get(chunkLocation).values()) {
                ProtectionType protectionType = protectedBlockLocation.getType();

                // Check if block is less or equal than the protection distance
                if ((protectionType.checkOnlyXZ() && protectedBlockLocation.distanceXZSquared(nearBlock) <= protectionType.getSearchDistanceSquared())
                        || (!protectionType.checkOnlyXZ() && protectedBlockLocation.distanceSquared(nearBlock) <= protectionType.getSearchDistanceSquared()))
                    // Insert block to list
                    protectedLocations.add(protectedBlockLocation);
            }

        return protectedLocations;
    }

    private boolean loadNearChunks(@NotNull final ChunkLocation chunkLocation) {
        // Check for not loaded ones
        for (ChunkLocation location : chunkLocation.getNearChunks(DEFAULT_LOAD_SIZE))
            // Insert to set if it isn't loaded
            if (!blockStorage.containsKey(location)) pendingLoading.add(location);

        // Check if the minimum are required, return false if it is
        for (ChunkLocation location : chunkLocation.getNearChunks(MINIMUM_LOAD_SIZE))
            if (pendingLoading.contains(location) || !blockStorage.containsKey(location) || pendingUnloading.contains(location))
                return false;

        return true;
    }

    private void checkConnection() throws SQLException {
        if (connection == null || !connection.isValid(1)) {
            if (connection != null) connection.close();
            connection = LobsterCraft.dataSource.getConnection();
        }
    }

    private class ChunkLoader implements Runnable {

        private final HashSet<ChunkLocation> loadedLocations = new HashSet<>(NumberConversions.ceil(LIMIT_CHUNK_LOAD_SIZE * 1.2D));

        @Override
        public void run() {
            // Ignore empty loading queue
            if (pendingLoading.isEmpty()) return;

            try {
                // Prepare connection
                checkConnection();

                // Clear Set
                loadedLocations.clear();

                // Create queries
                StringBuilder
                        playerQuery = new StringBuilder("SELECT * FROM world_blocks WHERE (worlds_worldId, chunkX, chunkZ) IN ("),
                        adminQuery = new StringBuilder("SELECT * FROM world_admin_blocks WHERE (worlds_worldId, chunkX, chunkZ) IN (");

                StringBuilder[] stringBuilders = new StringBuilder[]{playerQuery, adminQuery};

                synchronized (pendingLoading) {
                    int indexBounds = Math.min(LIMIT_CHUNK_LOAD_SIZE, pendingLoading.size() - 1);
                    int index = 0;

                    // Iterate through all items
                    Iterator<ChunkLocation> iterator = pendingLoading.iterator();

                    while (iterator.hasNext() && index <= indexBounds) {
                        ChunkLocation chunkLocation = iterator.next();

                        // Append information
                        for (StringBuilder stringBuilder : stringBuilders) {
                            stringBuilder
                                    .append('(')
                                    .append(chunkLocation.getWorldId()).append(", ")
                                    .append(chunkLocation.getChunkX()).append(", ")
                                    .append(chunkLocation.getChunkZ())
                                    .append(')');

                            // Check boundaries
                            if (index < indexBounds) stringBuilder.append(", ");
                        }
                        index++;

                        // Add to list of loaded
                        loadedLocations.add(chunkLocation);
                    }
                }

                // Close queries
                for (StringBuilder stringBuilder : stringBuilders) stringBuilder.append(");");

                PreparedStatement
                        playerStatement = connection.prepareStatement(playerQuery.toString()),
                        adminStatement = connection.prepareStatement(adminQuery.toString());

                ResultSet
                        playerResultSet = playerStatement.executeQuery(),
                        adminResultSet = adminStatement.executeQuery();

                int blockSize = 0;
                long startTime = System.nanoTime();

                synchronized (blockStorage) {
                    // Iterate through player blocks
                    while (playerResultSet.next()) {
                        // Create chunk
                        ChunkLocation chunkLocation = new ChunkLocation(playerResultSet.getLong("worlds_worldId"), playerResultSet.getInt("chunkX"), playerResultSet.getInt("chunkZ"));

                        // Create block
                        PlayerBlockLocation blockLocation = new PlayerBlockLocation(
                                chunkLocation,
                                playerResultSet.getByte("blockX"),
                                playerResultSet.getShort("blockY"),
                                playerResultSet.getByte("blockZ"),
                                playerResultSet.getLong("user_ownerId")
                        );

                        // Add protected block
                        blockStorage.putIfAbsent(blockLocation.getChunkLocation(), new HashMap<>());
                        blockStorage.get(blockLocation.getChunkLocation()).put(blockLocation, blockLocation);
                        blockSize++;
                    }

                    // Iterate through administrator blocks
                    while (adminResultSet.next()) {
                        // Create chunk
                        ChunkLocation chunkLocation = new ChunkLocation(adminResultSet.getLong("worlds_worldId"), adminResultSet.getInt("chunkX"), adminResultSet.getInt("chunkZ"));

                        // Create block
                        AdministratorBlockLocation blockLocation = new AdministratorBlockLocation(
                                chunkLocation,
                                adminResultSet.getByte("blockX"),
                                adminResultSet.getShort("blockY"),
                                adminResultSet.getByte("blockZ"),
                                adminResultSet.getLong("constructionId")
                        );

                        // Add protected block
                        blockStorage.putIfAbsent(blockLocation.getChunkLocation(), new HashMap<>());
                        blockStorage.get(blockLocation.getChunkLocation()).put(blockLocation, blockLocation);
                        blockSize++;
                    }

                    // Remove all pending loads
                    synchronized (pendingLoading) {
                        pendingLoading.removeAll(loadedLocations);
                    }

                    // Close everything
                    playerResultSet.close();
                    playerStatement.close();
                    adminResultSet.close();
                    adminStatement.close();
                }

                long deltaTime = System.nanoTime() - startTime;
                LobsterCraft.logger.info("It took " + formatter.format(deltaTime / (double) TimeUnit.MILLISECONDS.toNanos(1))
                        + "ms to search for " + blockSize + " blocks.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private class ChunkUnloader implements Runnable {

        private final HashMap<ChunkLocation, HashSet<ProtectedBlockLocation>>
                blocksToInsert = new HashMap<>(),
                blocksToUpdate = new HashMap<>(),
                blocksToDelete = new HashMap<>();

        private final HashSet<ChunkLocation> unloadingChunks = new HashSet<>();

        @Override
        public void run() {
            // Ignore empty unloading queue
            if (pendingUnloading.isEmpty()) return;


            try {
                // Retrieve connection
                checkConnection();

                unloadingChunks.clear();
                int blockSize = 0;
                long start = System.nanoTime();

                // Iterate through all needed chunks
                for (ChunkLocation chunkLocation : pendingUnloading) {
                    if (blockStorage.containsKey(chunkLocation))
                        synchronized (blockStorage) {
                            // Itreate through all blocks
                            for (ProtectedBlockLocation blockLocation : blockStorage.remove(chunkLocation).values()) {
                                DatabaseState databaseState = blockLocation.getDatabaseState();

                                // If databaseState should be saved, increase blockSize
                                if (databaseState.shouldSave()) blockSize++;

                                // Put block on the needed list
                                switch (databaseState) {
                                    case INSERT_TO_DATABASE:
                                        blocksToInsert.putIfAbsent(chunkLocation, new HashSet<>());
                                        blocksToInsert.get(chunkLocation).add(blockLocation);
                                        break;
                                    case UPDATE_DATABASE:
                                        blocksToUpdate.putIfAbsent(chunkLocation, new HashSet<>());
                                        blocksToUpdate.get(chunkLocation).add(blockLocation);
                                        break;
                                    case DELETE_FROM_DATABASE:
                                        blocksToDelete.putIfAbsent(chunkLocation, new HashSet<>());
                                        blocksToDelete.get(chunkLocation).add(blockLocation);
                                        break;
                                    default:
                                        break;
                                }
                            }
                            unloadingChunks.add(chunkLocation);
                        }
                }

                if (!blocksToInsert.isEmpty()) {
                    // Start statement
                    StringBuilder adminQuery = new StringBuilder(
                            "INSERT INTO `minecraft`.`world_admin_blocks` (`constructionId`, `worlds_worldId`, `chunkX`, `chunkZ`, `blockX`, `blockY`, `blockZ`) VALUES "
                    );
                    StringBuilder playerQuery = new StringBuilder(
                            "INSERT INTO `minecraft`.`world_blocks` (`worlds_worldId`, `chunkX`, `chunkZ`, `blockX`, `blockY`, `blockZ`, `user_ownerId`) VALUES "
                    );
                    boolean firstAdmin = true, firstPlayer = true;

                    for (Map.Entry<ChunkLocation, HashSet<ProtectedBlockLocation>> entry : blocksToInsert.entrySet()) {
                        ChunkLocation chunkLocation = entry.getKey();

                        for (ProtectedBlockLocation blockLocation : entry.getValue()) {
                            if (blockLocation.getType() == ProtectionType.ADMIN_PROTECTION) {
                                // Append comma first because I can't know the last one
                                if (!firstAdmin) adminQuery.append(',');
                                firstAdmin = false;

                                // Append information
                                adminQuery
                                        .append('(')
                                        .append(((AdministratorBlockLocation) blockLocation).getConstructionId()).append(',')

                                        .append(chunkLocation.getWorldId()).append(',')
                                        .append(chunkLocation.getChunkX()).append(',')
                                        .append(chunkLocation.getChunkZ()).append(',')

                                        .append(blockLocation.getRelativeX()).append(',')
                                        .append(blockLocation.getY()).append(',')
                                        .append(blockLocation.getRelativeZ())
                                        .append(')');
                                blockLocation.setOnDatabase();
                            } else if (blockLocation.getType() == ProtectionType.PLAYER_PROTECTION) {
                                // Append comma first because I can't know the last one
                                if (!firstPlayer) playerQuery.append(',');
                                firstPlayer = false;

                                // Append information
                                playerQuery
                                        .append('(')
                                        .append(chunkLocation.getWorldId()).append(',')
                                        .append(chunkLocation.getChunkX()).append(',')
                                        .append(chunkLocation.getChunkZ()).append(',')

                                        .append(blockLocation.getRelativeX()).append(',')
                                        .append(blockLocation.getY()).append(',')
                                        .append(blockLocation.getRelativeZ()).append(',')

                                        .append(((PlayerBlockLocation) blockLocation).getOwnerId())
                                        .append(')');
                                blockLocation.setOnDatabase();
                            }
                        }
                    }

                    // Close statement
                    adminQuery.append(';');
                    playerQuery.append(';');

                    if (!firstAdmin) {
                        LobsterCraft.logger.info("Security: " + adminQuery.toString());

                        // Prepare statement
                        PreparedStatement adminStatement = connection.prepareStatement(adminQuery.toString());

                        // Execute and close the statement
                        adminStatement.execute();
                        adminStatement.close();
                    }

                    if (!firstPlayer) {
                        LobsterCraft.logger.info("Security: " + playerQuery.toString());

                        // Prepare statement
                        PreparedStatement playerStatement = connection.prepareStatement(playerQuery.toString());

                        // Execute and close the statement
                        playerStatement.execute();
                        playerStatement.close();
                    }

                    // Clear the list
                    blocksToInsert.clear();
                }
                if (!blocksToUpdate.isEmpty()) {
                    // Start statement
                    PreparedStatement
                            adminStatement = connection.prepareStatement(
                            "UPDATE `minecraft`.`world_admin_blocks` SET `constructionId` = ? WHERE `worlds_worldId` = ?, `chunkX` = ?, `chunkZ` = ?, `blockX` = ?, `blockY` = ?, `blockZ` = ?;"),
                            playerStatement = connection.prepareStatement(
                                    "UPDATE `minecraft`.`world_blocks` SET `user_ownerId` = ? WHERE `worlds_worldId` = ?, `chunkX` = ?, `chunkZ` = ?, `blockX` = ?, `blockY` = ?, `blockZ` = ?;");
                    boolean haveAdmin = false, havePlayer = false;

                    for (Map.Entry<ChunkLocation, HashSet<ProtectedBlockLocation>> entry : blocksToUpdate.entrySet()) {
                        ChunkLocation chunkLocation = entry.getKey();

                        for (ProtectedBlockLocation blockLocation : entry.getValue()) {
                            if (blockLocation.getType() == ProtectionType.ADMIN_PROTECTION) {
                                // Append information
                                adminStatement.setLong(1, ((AdministratorBlockLocation) blockLocation).getConstructionId());

                                adminStatement.setLong(2, chunkLocation.getWorldId());
                                adminStatement.setInt(3, chunkLocation.getChunkX());
                                adminStatement.setInt(4, chunkLocation.getChunkZ());

                                adminStatement.setByte(5, blockLocation.getRelativeX());
                                adminStatement.setShort(6, blockLocation.getY());
                                adminStatement.setByte(7, blockLocation.getRelativeZ());

                                blockLocation.setOnDatabase();
                                adminStatement.addBatch();
                                haveAdmin = true;
                            } else if (blockLocation.getType() == ProtectionType.PLAYER_PROTECTION) {
                                // Append information
                                playerStatement.setLong(1, ((PlayerBlockLocation) blockLocation).getOwnerId());

                                playerStatement.setLong(2, chunkLocation.getWorldId());
                                playerStatement.setInt(3, chunkLocation.getChunkX());
                                playerStatement.setInt(4, chunkLocation.getChunkZ());

                                playerStatement.setByte(5, blockLocation.getRelativeX());
                                playerStatement.setShort(6, blockLocation.getY());
                                playerStatement.setByte(7, blockLocation.getRelativeZ());

                                blockLocation.setOnDatabase();
                                playerStatement.addBatch();
                                havePlayer = true;
                            }
                        }
                    }

                    // Execute and close the statements
                    if (haveAdmin) {
                        adminStatement.executeBatch();
                        adminStatement.close();
                    }
                    if (havePlayer) {
                        playerStatement.executeBatch();
                        playerStatement.close();
                    }

                    // Clear the list
                    blocksToUpdate.clear();
                }
                if (!blocksToDelete.isEmpty()) {
                    // Start statement
                    StringBuilder
                            adminQuery = new StringBuilder(
                            "DELETE FROM `minecraft`.`world_admin_blocks` WHERE (worlds_worldId, chunkX, chunkZ, blockX, blockY, blockZ) IN ("),
                            playerQuery = new StringBuilder(
                                    "DELETE FROM `minecraft`.`world_blocks` WHERE (worlds_worldId, chunkX, chunkZ, blockX, blockY, blockZ) IN (");
                    boolean firstAdmin = true, firstPlayer = true;

                    for (Map.Entry<ChunkLocation, HashSet<ProtectedBlockLocation>> entry : blocksToDelete.entrySet()) {
                        ChunkLocation chunkLocation = entry.getKey();

                        for (ProtectedBlockLocation blockLocation : entry.getValue()) {
                            StringBuilder query;

                            if (blockLocation.getType() == ProtectionType.ADMIN_PROTECTION) {
                                // Append comma
                                if (!firstAdmin) adminQuery.append(',');
                                firstAdmin = false;

                                // Set query
                                query = adminQuery;
                            } else { //if (blockLocation.getType() == ProtectionType.PLAYER_PROTECTION) {
                                // Append comma
                                if (!firstPlayer) playerQuery.append(',');
                                firstPlayer = false;

                                // Set query
                                query = playerQuery;
                            }

                            // Append information
                            query
                                    .append('(')
                                    .append(chunkLocation.getWorldId()).append(',')
                                    .append(chunkLocation.getChunkX()).append(',')

                                    .append(chunkLocation.getChunkZ()).append(',')
                                    .append(blockLocation.getRelativeX()).append(',')
                                    .append(blockLocation.getY()).append(',')
                                    .append(blockLocation.getRelativeZ())
                                    .append(')');
                            blockLocation.setOnDatabase();
                        }
                    }

                    // Close statement
                    adminQuery.append(");");
                    LobsterCraft.logger.info("Security: " + adminQuery.toString());
                    playerQuery.append(");");
                    LobsterCraft.logger.info("Security: " + playerQuery.toString());

                    if (!firstAdmin) {
                        // Prepare statement
                        PreparedStatement adminStatement = connection.prepareStatement(adminQuery.toString());

                        // Execute and close the statement
                        adminStatement.execute();
                        adminStatement.close();
                    }
                    if (!firstPlayer) {
                        // Prepare statement
                        PreparedStatement playerStatement = connection.prepareStatement(playerQuery.toString());

                        // Execute and close the statement
                        playerStatement.execute();
                        playerStatement.close();
                    }

                    // Clear the list
                    blocksToDelete.clear();
                }

                synchronized (pendingUnloading) {
                    pendingUnloading.removeAll(unloadingChunks);
                }

                // Just "debug" something useful
                if (blockSize > 0)
                    LobsterCraft.logger.info("Took us " + formatter.format((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)) +
                            "ms to change " + blockSize + " blocks on database.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO test with public x private (debug)
    private class ChunkListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onChunkUnload(ChunkUnloadEvent event) {
            if (LobsterCraft.worldService.isWorldIgnored(event.getWorld()) || event.isCancelled())
                return;

            // Add chunk to pending unloading (we don't need to check for anything else neither cancel the event)
            pendingUnloading.add(new ChunkLocation(event.getChunk()));
            LobsterCraft.logger.info("Pending Unload chunk: x=" + event.getChunk().getX() + ", z=" + event.getChunk().getZ());
        }

    }
}
