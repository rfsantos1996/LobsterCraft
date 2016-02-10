package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.ConditionController;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Service;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.util.ProtectionType;
import com.jabyftw.lobstercraft.world.util.location_util.BlockLocation;
import com.jabyftw.lobstercraft.world.util.location_util.ChunkLocation;
import com.jabyftw.lobstercraft.world.util.location_util.ProtectedBlockLocation;
import com.jabyftw.lobstercraft.world.util.location_util.TemporaryProtectedBlockLocation;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.logging.AbstractLoggingExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.util.NumberConversions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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

    private final int NUMBER_OF_CHUNK_LOADER_TASKS = LobsterCraft.config.getInt(ConfigValue.WORLD_CHUNK_LOAD_NUMBER_OF_TASKS.toString());

    // Although they're concurrent, some chunk loading that takes too long are interfering with each other
    private final ConcurrentLinkedDeque<ChunkLocation>
            pendingLoading = new ConcurrentLinkedDeque<>(), // TODO more than one thread loading
            pendingUnloading = new ConcurrentLinkedDeque<>();
    private final Set<ChunkLocation>
            loadedLocations = Collections.synchronizedSet(new HashSet<>(NumberConversions.ceil(LIMIT_CHUNK_LOAD_SIZE * 1.2D))),
            unloadedLocations = Collections.synchronizedSet(new HashSet<>());
    private final ConcurrentHashMap<ChunkLocation, ConcurrentHashMap<BlockLocation, ProtectedBlockLocation>> blockStorage = new ConcurrentHashMap<>();

    public TemporaryBlockProtection temporaryBlockProtection;
    private ChunkUnloader chunkUnloader;
    private Connection connection;

    @Override
    public boolean onEnable() {
        LobsterCraft.logger.info("minimumLoadSize=" + MINIMUM_LOAD_SIZE + ", " +
                "defaultLoadSize=" + DEFAULT_LOAD_SIZE + ", " +
                "defaultChunksLoaded=" + Util.getNumberOfChunksLoaded(DEFAULT_LOAD_SIZE) + ", " +
                "numberChunkLoadTasks=" + NUMBER_OF_CHUNK_LOADER_TASKS
        );

        Bukkit.getServer().getPluginManager().registerEvents(new ChunkListener(), LobsterCraft.lobsterCraft);

        try {
            checkConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (LobsterCraft.worldEdit != null)
            BukkitScheduler.runTaskLater(() -> {
                try {
                    LobsterCraft.worldEdit.getEventBus().register(new WorldEditLogger());
                } catch (Exception e) {
                    e.printStackTrace();
                    LobsterCraft.logger.info("Couldn't register WorldEdit logger.");
                }
            }, 0L);

        for (int i = 0; i < NUMBER_OF_CHUNK_LOADER_TASKS; i++)
            BukkitScheduler.runTaskTimerAsynchronously(new ChunkLoader(), 10L, CHUNK_LOAD_PERIOD);
        BukkitScheduler.runTaskTimerAsynchronously(chunkUnloader = new ChunkUnloader(), 10L, CHUNK_LOAD_PERIOD);
        temporaryBlockProtection = new TemporaryBlockProtection();

        return true;
    }

    @Override
    public void onDisable() {
        // End temporary blocks task
        temporaryBlockProtection.destroy();

        // Fill it up with all loaded chunks
        for (World world : Bukkit.getWorlds())
            if (!LobsterCraft.worldService.isWorldIgnored(world))
                for (Chunk chunk : world.getLoadedChunks())
                    pendingUnloading.add(new ChunkLocation(chunk));

        while (!pendingUnloading.isEmpty())
            chunkUnloader.run();

        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * Block placed
     * -> If it is new block, player protected => if theres no next block from the same player -> temporary protection, else -> default protection, get near temporary blocks to send to protection
     */

    public void addBlock(@NotNull final Location location, @NotNull ProtectionType type, long currentId) {
        ProtectedBlockLocation protectedBlockLocation = new ProtectedBlockLocation(location, type);
        ProtectedBlockLocation currentValue = blockStorage.get(protectedBlockLocation.getChunkLocation()).putIfAbsent(protectedBlockLocation, protectedBlockLocation);
        ProtectedBlockLocation returningLocation;

        if (currentValue == null)
            returningLocation = protectedBlockLocation;
        else if (currentValue.getType() != protectedBlockLocation.getType())
            // Reset block state for new type
            returningLocation = currentValue.setProtectionType(type).setCurrentId(PlayerHandler.UNDEFINED_PLAYER);
        else
            returningLocation = currentValue;
        // TODO temporary blocks for protection type

        returningLocation.getDatabaseState().isOnDatabase();
        returningLocation.setCurrentId(currentId);

        if (type == ProtectionType.PLAYER_PROTECTION) {

        }

    }

    public ProtectedBlockLocation getBlock(@NotNull final Location location) {
        // TODO Maybe return null if currentId is UNDEFINED_PLAYER?
        return getBlock(new BlockLocation(location));
    }

    public ProtectedBlockLocation getBlock(@NotNull final BlockLocation blockLocation) {
        return blockStorage.get(blockLocation.getChunkLocation()).get(blockLocation);
    }

    public Collection<ProtectedBlockLocation> getBlocksForChunk(@NotNull final ChunkLocation chunkLocation) {
        return blockStorage.get(chunkLocation).values();
    }

    public boolean checkForOtherPlayersAndAdministratorBlocks(@NotNull final Location location, @Nullable Long playerId) {
        return new BlockLocation(location).getChunkLocation().getNearChunks(MINIMUM_LOAD_SIZE).parallelStream().anyMatch(chunkLocation -> {
            for (ProtectedBlockLocation protectedBlock : blockStorage.get(chunkLocation).values())
                if (!protectedBlock.isUndefined() && ((protectedBlock.getType() == ProtectionType.PLAYER_PROTECTION && (playerId == null || playerId != protectedBlock.getCurrentId()))
                        || (protectedBlock.getType() == ProtectionType.ADMIN_PROTECTION)))
                    return true;
            return false;
        });
    }

    public boolean checkForOtherPlayersBlocks(@NotNull final Location location, long playerId) {
        return checkForOtherPlayersBlocks(new BlockLocation(location), playerId);
    }

    public boolean checkForOtherPlayersBlocks(@NotNull final BlockLocation blockLocation, long playerId) {
        /*
         * Actual average: 0.38ms (success) - http://timings.aikar.co/?url=14884599
         * The old method was taking:
         * ~180ms without changes (old commit, for each iterating with all blocks) - http://timings.aikar.co/?url=14884327
         * ~130ms with parallel stream for chunks and stream for blocks - http://timings.aikar.co/?url=14884433
         * ~70ms with one parallel for chunks and for-each for blocks - http://timings.aikar.co/?url=14884464
         *
         */
        return blockLocation.getChunkLocation().getNearChunks(MINIMUM_LOAD_SIZE).parallelStream().anyMatch(chunkLocation -> {
            for (ProtectedBlockLocation protectedBlock : blockStorage.get(chunkLocation).values())
                if (protectedBlock.getType() == ProtectionType.PLAYER_PROTECTION && !protectedBlock.isUndefined() && playerId != protectedBlock.getCurrentId())
                    return true;
            return false;
        });
    }

    public boolean checkForAdministratorBlocks(@NotNull final Location location) {
        return checkForAdministratorBlocks(new BlockLocation(location));
    }

    public boolean checkForAdministratorBlocks(@NotNull final BlockLocation blockLocation) {
        return blockLocation.getChunkLocation().getNearChunks(MINIMUM_LOAD_SIZE).parallelStream().anyMatch(chunkLocation -> {
            for (ProtectedBlockLocation protectedBlock : blockStorage.get(chunkLocation).values())
                if (protectedBlock.getType() == ProtectionType.ADMIN_PROTECTION && !protectedBlock.isUndefined())
                    return true;
            return false;
        });
    }

    public boolean loadNearChunks(@NotNull final Location location) {
        return loadNearChunks(new ChunkLocation(location.getChunk()));
    }

    public boolean loadNearChunks(@NotNull final ChunkLocation chunkLocation) {
        // Check for not loaded ones
        for (ChunkLocation location : chunkLocation.getNearChunks(DEFAULT_LOAD_SIZE))
            // Insert to set if it isn't loaded
            if (!pendingLoading.contains(location) && !loadedLocations.contains(location) && !blockStorage.containsKey(location))
                pendingLoading.add(location);

        // Check if the minimum are required, return false if it is
        for (ChunkLocation location : chunkLocation.getNearChunks(MINIMUM_LOAD_SIZE))
            if (pendingLoading.contains(location) ||
                    pendingUnloading.contains(location) ||
                    loadedLocations.contains(location) ||
                    unloadedLocations.contains(location) ||
                    !blockStorage.containsKey(location))
                return false;

        return true;
    }

    private void checkConnection() throws SQLException {
        if (connection == null || !connection.isValid(1)) {
            if (connection != null) connection.close();
            connection = LobsterCraft.dataSource.getConnection();
        }
    }

    public void removeConstruction(@NotNull final Connection connection, long constructionId) throws SQLException {
        // Prepare statement
        PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `minecraft`.`world_blocks` WHERE `ownerType` = ? AND `ownerId` = ?;");

        // Set variables
        preparedStatement.setByte(1, ProtectionType.ADMIN_PROTECTION.getId());
        preparedStatement.setLong(2, constructionId);

        // Execute and close statement
        preparedStatement.execute();
        preparedStatement.close();

        // Iterate through all chunks
        for (ConcurrentHashMap<BlockLocation, ProtectedBlockLocation> blockLocationHashMap : blockStorage.values()) {
            Iterator<ProtectedBlockLocation> iterator = blockLocationHashMap.values().iterator();

            // Iterate through all blocks
            while (iterator.hasNext()) {
                ProtectedBlockLocation next = iterator.next();

                // Delete block if it is of the same type
                if (next.getType() == ProtectionType.ADMIN_PROTECTION && next.getCurrentId() == constructionId)
                    iterator.remove();
            }
        }
    }

    private class ChunkLoader implements Runnable {

        private Connection connection = null;

        private void checkConnection() throws SQLException {
            if (connection == null || !connection.isValid(1)) {
                if (connection != null) connection.close();
                connection = LobsterCraft.dataSource.getConnection();
            }
        }

        @Override
        public void run() {
            // Create queries
            StringBuilder stringBuilder = new StringBuilder("SELECT * FROM minecraft.world_blocks WHERE (worlds_worldId, chunkX, chunkZ) IN (");

            synchronized (pendingLoading) {
                // Ignore empty loading queue (must be synchronized to the iterator we're using)
                if (pendingLoading.isEmpty()) return;

                int indexBounds = Math.min(LIMIT_CHUNK_LOAD_SIZE, pendingLoading.size() - 1);
                int index = 0;

                synchronized (loadedLocations) {
                    // Iterate through all items
                    Iterator<ChunkLocation> iterator = pendingLoading.iterator();

                    while (iterator.hasNext() && index <= indexBounds) {
                        ChunkLocation chunkLocation = iterator.next();

                        // Append information
                        stringBuilder
                                .append('(')
                                .append(chunkLocation.getWorldId()).append(", ")
                                .append(chunkLocation.getChunkX()).append(", ")
                                .append(chunkLocation.getChunkZ())
                                .append(')');

                        // Check boundaries
                        if (index < indexBounds) stringBuilder.append(", ");
                        index++;

                        // Add to list of loaded
                        loadedLocations.add(chunkLocation);
                        iterator.remove();
                    }
                }

                // Close query
                stringBuilder.append(");");
            }

            try {
                // Prepare connection
                checkConnection();

                // Prepare statement
                PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());

                // Execute statement
                ResultSet resultSet = preparedStatement.executeQuery();

                int blockSize = 0;
                long startTime = System.nanoTime();

                while (resultSet.next()) {
                    // Create chunk
                    ChunkLocation chunkLocation = new ChunkLocation(
                            resultSet.getLong("worlds_worldId"),
                            resultSet.getInt("chunkX"),
                            resultSet.getInt("chunkZ")
                    );

                    // Create block
                    ProtectedBlockLocation protectedBlockLocation = new ProtectedBlockLocation(
                            chunkLocation,
                            ProtectionType.getFromId(resultSet.getByte("ownerType")),
                            resultSet.getByte("blockX"),
                            resultSet.getShort("blockY"),
                            resultSet.getByte("blockZ"),
                            resultSet.getLong("ownerId")
                    );

                    // Add protected block
                    blockStorage.putIfAbsent(chunkLocation, new ConcurrentHashMap<>());
                    blockStorage.get(chunkLocation).put(protectedBlockLocation, protectedBlockLocation);
                    blockSize++;
                }

                synchronized (loadedLocations) {
                    Iterator<ChunkLocation> iterator = loadedLocations.iterator();

                    while (iterator.hasNext()) {
                        // Make sure chunk is set as loaded
                        blockStorage.putIfAbsent(iterator.next(), new ConcurrentHashMap<>());
                        // Remove from set
                        iterator.remove();
                    }
                }

                // Close everything
                resultSet.close();
                preparedStatement.close();

                double timeTakenMilliseconds = (double) (System.nanoTime() - startTime) / (double) TimeUnit.MILLISECONDS.toNanos(1);

                if (blockSize > 0 && timeTakenMilliseconds > 5.0d)
                    LobsterCraft.logger.info("It took " + formatter.format(timeTakenMilliseconds) + "ms to search for " + blockSize + " blocks.");
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

        @Override
        public void run() {

            // Ignore empty unloading queue
            if (pendingUnloading.isEmpty()) return;

            try {
                // Retrieve connection
                checkConnection();

                int blockSize = 0;
                long start = System.nanoTime();

                // Iterate through all needed chunks
                synchronized (unloadedLocations) {
                    synchronized (pendingUnloading) {
                        Iterator<ChunkLocation> iterator = pendingUnloading.iterator();

                        while (iterator.hasNext()) {
                            ChunkLocation chunkLocation = iterator.next();

                            if (blockStorage.containsKey(chunkLocation)) {
                                // Itreate through all blocks => It'll be removed, so theres no reason to synchronize
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
                                unloadedLocations.add(chunkLocation);
                            } else {
                                // Remove chunk if it isn't supposed to be database-unloaded
                                iterator.remove();
                            }
                        }
                    }
                }

                if (!blocksToInsert.isEmpty()) {
                    // Start statement
                    StringBuilder stringBuilder = new StringBuilder(
                            "INSERT INTO `minecraft`.`world_blocks` (`worlds_worldId`, `chunkX`, `chunkZ`, `blockX`, `blockY`, `blockZ`, `ownerType`, `ownerId`) VALUES "
                    );
                    boolean first = true;

                    // Iterate through all chunks
                    for (Map.Entry<ChunkLocation, HashSet<ProtectedBlockLocation>> entry : blocksToInsert.entrySet()) {
                        ChunkLocation chunkLocation = entry.getKey();

                        // Iterate through all blocks
                        for (ProtectedBlockLocation blockLocation : entry.getValue()) {
                            if (!first) stringBuilder.append(',');
                            first = false;

                            // Append all information in order
                            stringBuilder
                                    .append('(')
                                    .append(chunkLocation.getWorldId()).append(',')
                                    .append(chunkLocation.getChunkX()).append(',')
                                    .append(chunkLocation.getChunkZ()).append(',')
                                    .append(blockLocation.getRelativeX()).append(',')
                                    .append(blockLocation.getY()).append(',')
                                    .append(blockLocation.getRelativeZ()).append(',')
                                    .append(blockLocation.getType().getId()).append(',')
                                    .append(blockLocation.getCurrentId())
                                    .append(')');
                            // Update block instance
                            blockLocation.setOnDatabase();
                        }
                    }

                    // Close statement
                    stringBuilder.append(';');
                    //LobsterCraft.logger.info("Security: " + stringBuilder.toString());

                    // Prepare, execute and close statement
                    PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
                    preparedStatement.execute();
                    preparedStatement.close();

                    // Clear the list
                    blocksToInsert.clear();
                }

                if (!blocksToUpdate.isEmpty()) {
                    // Start statement
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "UPDATE `minecraft`.`world_blocks` SET  `ownerType` = ?, `ownerId` = ? WHERE `worlds_worldId` = ? AND `chunkX` = ? AND `chunkZ` = ? AND `blockX` = ? AND `blockY` = ? AND `blockZ` = ?;"
                    );

                    // Itereate through chunks
                    for (Map.Entry<ChunkLocation, HashSet<ProtectedBlockLocation>> entry : blocksToUpdate.entrySet()) {
                        ChunkLocation chunkLocation = entry.getKey();

                        // Iterate through blocks
                        for (ProtectedBlockLocation blockLocation : entry.getValue()) {
                            // Append information
                            preparedStatement.setByte(1, blockLocation.getType().getId());
                            preparedStatement.setLong(2, blockLocation.getCurrentId());

                            // Append where-clause
                            preparedStatement.setLong(3, chunkLocation.getWorldId());
                            preparedStatement.setInt(4, chunkLocation.getChunkX());
                            preparedStatement.setInt(5, chunkLocation.getChunkZ());

                            preparedStatement.setByte(6, blockLocation.getRelativeX());
                            preparedStatement.setShort(7, blockLocation.getY());
                            preparedStatement.setByte(8, blockLocation.getRelativeZ());

                            // Add batch
                            blockLocation.setOnDatabase();
                            preparedStatement.addBatch();
                        }
                    }

                    // Execute and close statement
                    preparedStatement.execute();
                    preparedStatement.close();

                    // Clear the list
                    blocksToUpdate.clear();
                }

                if (!blocksToDelete.isEmpty()) {
                    // Start statement
                    StringBuilder stringBuilder = new StringBuilder(
                            "DELETE FROM `minecraft`.`world_blocks` WHERE (`worlds_worldId`, `chunkX`, `chunkZ`, `blockX`, `blockY`, `blockZ`) IN ("
                    );
                    boolean first = true;

                    // Iterate through all chunks
                    for (Map.Entry<ChunkLocation, HashSet<ProtectedBlockLocation>> entry : blocksToDelete.entrySet()) {
                        ChunkLocation chunkLocation = entry.getKey();

                        // Iterate through all blocks
                        for (ProtectedBlockLocation blockLocation : entry.getValue()) {
                            if (!first) stringBuilder.append(',');
                            first = false;

                            // Append all information in order
                            stringBuilder
                                    .append('(')
                                    .append(chunkLocation.getWorldId()).append(',')
                                    .append(chunkLocation.getChunkX()).append(',')
                                    .append(chunkLocation.getChunkZ()).append(',')
                                    .append(blockLocation.getRelativeX()).append(',')
                                    .append(blockLocation.getY()).append(',')
                                    .append(blockLocation.getRelativeZ())
                                    .append(')');
                            // Update block instance
                            blockLocation.setOnDatabase();
                        }
                    }

                    // Close statement
                    stringBuilder.append(");");
                    //LobsterCraft.logger.info("Security: " + stringBuilder.toString());

                    // Prepare, execute and close statement
                    PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
                    preparedStatement.execute();
                    preparedStatement.close();

                    // Clear the list
                    blocksToDelete.clear();
                }

                synchronized (unloadedLocations) {
                    Iterator<ChunkLocation> iterator = unloadedLocations.iterator();

                    while (iterator.hasNext()) {
                        // Remove chunk's blocks from map
                        blockStorage.remove(iterator.next());
                        // Remove chunk from unloaded location set
                        iterator.remove();
                    }
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

    private class ChunkListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onChunkUnloadHighest(ChunkUnloadEvent event) {
            if (LobsterCraft.worldService.isWorldIgnored(event.getWorld()) || event.isCancelled())
                return;

            // Cancel those chunks who are on temporary protection (blocks may change during this period)
            if (!temporaryBlockProtection.canChunkBeUnloaded(new ChunkLocation(event.getChunk())))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onChunkUnloadMonitor(ChunkUnloadEvent event) {
            if (LobsterCraft.worldService.isWorldIgnored(event.getWorld()) || event.isCancelled())
                return;

            // Add chunk to pending unloading (we don't need to check for anything else neither cancel the event)
            pendingUnloading.add(new ChunkLocation(event.getChunk()));
        }

    }

    private class WorldEditLogger {

        private long lastActorWarning;

        private final ConcurrentHashMap<BlockLocation, LogEntry> pendingBlocks = new ConcurrentHashMap<>();

        WorldEditLogger() {
            BukkitScheduler.runTaskTimerAsynchronously(() -> {
                if (pendingBlocks.isEmpty())
                    return;

                Iterator<Map.Entry<BlockLocation, LogEntry>> iterator = pendingBlocks.entrySet().iterator();

                // Iterate through all blocks
                while (iterator.hasNext()) {
                    Map.Entry<BlockLocation, LogEntry> entry = iterator.next();

                    if (!LobsterCraft.blockController.loadNearChunks(entry.getKey().getChunkLocation())) {
                        //LobsterCraft.logger.info("Couldn't load " + entry.getKey().toString() + ", continuing");
                        continue;
                    }

                    // Retrieve variables
                    EditSessionEvent event = entry.getValue().event;
                    BlockState newState = entry.getValue().blockState;

                    // If block is being set to nothing, unprotect it
                    if (newState.getType() == Material.AIR) {
                        ProtectedBlockLocation blockLocation = LobsterCraft.blockController.getBlock(newState.getLocation());

                        // If block exists, remove it
                        if (blockLocation != null && !blockLocation.isUndefined())
                            blockLocation.setUndefinedOwner();
                    } else //noinspection ConstantConditions => it is already filtered before being added to pendingBlocks
                        if (event.getActor().isPlayer()) {
                            PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandlerNoRestrictions(Bukkit.getPlayer(event.getActor().getName()));

                            if (playerHandler == null) {
                                // Warn console (but not for every block)
                                if (System.currentTimeMillis() - lastActorWarning > TimeUnit.SECONDS.toSeconds(30)) {
                                    LobsterCraft.logger.warning("Actor is now offline (" + event.getActor().getName() + "), his blocks aren't going to be protected");
                                    lastActorWarning = System.currentTimeMillis();
                                }
                                iterator.remove();
                                return;
                            }

                            if (playerHandler.getProtectionType() != ProtectionType.ADMIN_PROTECTION) {
                                playerHandler.getConditionController().sendMessageIfConditionReady(
                                        ConditionController.Condition.WORLD_EDIT_PLAYER_PROTECTION_WARNING,
                                        "§cUse §4//undo§c, blocos de WorldEdit não serão protegidos fora do modo de construção para administradores."
                                );
                                iterator.remove();
                                return;
                            }

                            // Protect block for player
                            LobsterCraft.blockController
                                    .addBlock(newState.getLocation(), playerHandler.getProtectionType(), playerHandler.getBuildModeId());
                        }

                    // Remove block after processing
                    iterator.remove();
                }
            }, 20L, 20L);
        }

        @Subscribe
        public void logChanges(final EditSessionEvent event) {
            if (event.getActor() == null || event.getStage() != EditSession.Stage.BEFORE_CHANGE)
                return;

            event.setExtent(new LoggingExtent(event));
        }

        private class LoggingExtent extends AbstractLoggingExtent {

            private final EditSessionEvent event;

            protected LoggingExtent(EditSessionEvent event) {
                super(event.getExtent());
                this.event = event;
            }

            @SuppressWarnings("deprecation")
            @Override
            protected void onBlockChange(Vector position, BaseBlock newBlock) {
                if (!(event.getWorld() instanceof BukkitWorld))
                    return;

                World bukkitWorld = ((BukkitWorld) event.getWorld()).getWorld();

                if (LobsterCraft.worldService.isWorldIgnored(bukkitWorld))
                    return;

                BlockState newState = bukkitWorld.getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ()).getState();

                // Update new state of the block
                newState.setTypeId(newBlock.getType());
                newState.setRawData((byte) newBlock.getData());

                // Add block to pending processing map
                pendingBlocks.put(new BlockLocation(newState.getLocation()), new LogEntry(newState, event));
            }
        }

        private class LogEntry {

            protected final BlockState blockState;
            protected final EditSessionEvent event;

            LogEntry(@NotNull final BlockState blockState, @NotNull final EditSessionEvent event) {
                this.blockState = blockState;
                this.event = event;
            }
        }
    }

    public class TemporaryBlockProtection {

        private final double
                SEARCH_DISTANCE = LobsterCraft.config.getDouble(ConfigValue.WORLD_TEMPORARY_PROTECTION_PROTECTED_DISTANCE.getPath()),
                SEARCH_DISTANCE_SQUARED = NumberConversions.square(SEARCH_DISTANCE);
        private final int CHUNK_SEARCH_DISTANCE = Util.getMinimumChunkSize(SEARCH_DISTANCE);

        private final ConcurrentHashMap<ChunkLocation, ConcurrentHashMap<BlockLocation, TemporaryProtectedBlockLocation>> temporaryBlocksStorage = new ConcurrentHashMap<>();

        public TemporaryBlockProtection() {
        }

        public void destroy() {
        }

        // TODO after temporary blocks are processed, return to storage or delete blocks => delete chunk so it can be unloaded from world if necessary

        public void addBlock(@NotNull final ProtectedBlockLocation protectedBlockLocation) {
            // Make sure chunk is here
            temporaryBlocksStorage.putIfAbsent(protectedBlockLocation.getChunkLocation(), new ConcurrentHashMap<>());

            // Insert block
            TemporaryProtectedBlockLocation temporaryLocation = new TemporaryProtectedBlockLocation(protectedBlockLocation);
            TemporaryProtectedBlockLocation currentValue = temporaryBlocksStorage.get(protectedBlockLocation.getChunkLocation()).putIfAbsent(protectedBlockLocation, temporaryLocation);

            final TemporaryProtectedBlockLocation actualBlock = currentValue == null ? temporaryLocation : currentValue;

            // Check for near blocks
            BukkitScheduler.runTaskAsynchronously(new BlockProcess(actualBlock));
        }

        public boolean canChunkBeUnloaded(@NotNull final ChunkLocation chunkLocation) {
            return !temporaryBlocksStorage.containsKey(chunkLocation);
        }

        private class BlockProcess implements Runnable {

            private final TemporaryProtectedBlockLocation actualBlock;
            private final ConcurrentHashMap<ChunkLocation, ConcurrentHashMap<BlockLocation, TemporaryProtectedBlockLocation>> blocksNear = new ConcurrentHashMap<>();

            private BlockProcess(@NotNull final TemporaryProtectedBlockLocation actualBlock) {
                this.actualBlock = actualBlock;
            }

            @Override
            public void run() {
                long start = System.nanoTime();

                actualBlock.getChunkLocation().getNearChunks(CHUNK_SEARCH_DISTANCE)
                        .parallelStream()
                        // Iterate asynchronously through all chunks
                        .forEach(
                                chunkLocation -> {
                                    // Insert chunk
                                    blocksNear.putIfAbsent(chunkLocation, new ConcurrentHashMap<>());
                                    ConcurrentHashMap<BlockLocation, TemporaryProtectedBlockLocation> blocksMap = blocksNear.get(chunkLocation);

                                    // Iterate through all temporary blocks from given chunk
                                    temporaryBlocksStorage.get(chunkLocation).values()
                                            .stream()
                                            // Filter to player's new blocks
                                            .filter((blockLocation) ->
                                                    blockLocation.getCurrentId() != PlayerHandler.UNDEFINED_PLAYER &&
                                                            blockLocation.getType() == actualBlock.getType() &&
                                                            blockLocation.getCurrentId() == actualBlock.getCurrentId() &&
                                                            blockLocation.distanceSquared(actualBlock) < SEARCH_DISTANCE_SQUARED
                                            )
                                            .forEach(temporaryProtectedBlockLocation -> blocksMap.put(temporaryProtectedBlockLocation, temporaryProtectedBlockLocation));
                                }
                        );

                LobsterCraft.logger.info("It took " + formatter.format((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)) + "ms to process block");
            }
        }
    }
}
