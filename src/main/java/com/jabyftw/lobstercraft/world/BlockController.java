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
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.logging.AbstractLoggingExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sun.istack.internal.NotNull;
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

    private final ConcurrentLinkedDeque<ChunkLocation>
            pendingLoading = new ConcurrentLinkedDeque<>(), // TODO more than one thread loading => I removed (maybe) unnecessary synchronized blocks
            pendingUnloading = new ConcurrentLinkedDeque<>();
    private final Set<ChunkLocation>
            loadedLocations = Collections.synchronizedSet(new HashSet<>(NumberConversions.ceil(LIMIT_CHUNK_LOAD_SIZE * 1.2D))),
            unloadedLocations = Collections.synchronizedSet(new HashSet<>());
    private final ConcurrentHashMap<ChunkLocation, HashMap<BlockLocation, ProtectedBlockLocation>> blockStorage = new ConcurrentHashMap<>();

    private Connection connection;
    private ChunkUnloader chunkUnloader;

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

        if (LobsterCraft.worldEdit != null)
            BukkitScheduler.runTaskLater(() -> {
                try {
                    LobsterCraft.worldEdit.getEventBus().register(new WorldEditLogger());
                } catch (Exception e) {
                    e.printStackTrace();
                    LobsterCraft.logger.info("Couldn't register WorldEdit logger.");
                }
            }, 0L);
        BukkitScheduler.runTaskTimerAsynchronously(new ChunkLoader(), 20L, CHUNK_LOAD_PERIOD);
        BukkitScheduler.runTaskTimerAsynchronously(chunkUnloader = new ChunkUnloader(), 20L, CHUNK_LOAD_PERIOD);

        return true;
    }

    @Override
    public void onDisable() {
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

    public ProtectedBlockLocation addBlock(@NotNull final Location location, @NotNull ProtectionType type) {
        ProtectedBlockLocation protectedBlockLocation = new ProtectedBlockLocation(location, type);
        ProtectedBlockLocation currentValue = blockStorage.get(protectedBlockLocation.getChunkLocation()).putIfAbsent(protectedBlockLocation, protectedBlockLocation);

        if (currentValue == null)
            return protectedBlockLocation;
        else if (currentValue.getType() != protectedBlockLocation.getType())
            // Reset block state for new type
            return currentValue.setProtectionType(type).setCurrentId(PlayerHandler.UNDEFINED_PLAYER);
        else
            return currentValue;
    }

    public ProtectedBlockLocation getBlock(@NotNull final Location location) {
        // TODO Maybe return null if currentId is UNDEFINED_PLAYER?
        return getBlock(new BlockLocation(location));
    }

    public ProtectedBlockLocation getBlock(@NotNull final BlockLocation blockLocation) {
        return blockStorage.get(blockLocation.getChunkLocation()).get(blockLocation);
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

    private class ChunkLoader implements Runnable {

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
                StringBuilder stringBuilder = new StringBuilder("SELECT * FROM minecraft.world_blocks WHERE (worlds_worldId, chunkX, chunkZ) IN (");

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
                    blockStorage.putIfAbsent(chunkLocation, new HashMap<>());
                    blockStorage.get(chunkLocation).put(protectedBlockLocation, protectedBlockLocation);
                    blockSize++;
                }

                synchronized (loadedLocations) {
                    Iterator<ChunkLocation> iterator = loadedLocations.iterator();

                    while (iterator.hasNext()) {
                        // Make sure chunk is set as loaded
                        blockStorage.putIfAbsent(iterator.next(), new HashMap<>());
                        // Remove from set
                        iterator.remove();
                    }
                }

                // Close everything
                resultSet.close();
                preparedStatement.close();

                if (blockSize > 0)
                    LobsterCraft.logger.info("It took " + formatter.format((double) (System.nanoTime() - startTime) / (double) TimeUnit.MILLISECONDS.toNanos(1))
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
                    Iterator<ChunkLocation> iterator = pendingUnloading.iterator();

                    while (iterator.hasNext()) {
                        ChunkLocation chunkLocation = iterator.next();

                        if (blockStorage.containsKey(chunkLocation)) {
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
                            unloadedLocations.add(chunkLocation);
                        } else {
                            // Remove chunk if it isn't supposed to be database-unloaded
                            iterator.remove();
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
                    LobsterCraft.logger.info("Security: " + stringBuilder.toString());

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
                    LobsterCraft.logger.info("Security: " + stringBuilder.toString());

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

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onChunkUnload(ChunkUnloadEvent event) {
            if (LobsterCraft.worldService.isWorldIgnored(event.getWorld()) || event.isCancelled())
                return;

            // Add chunk to pending unloading (we don't need to check for anything else neither cancel the event)
            pendingUnloading.add(new ChunkLocation(event.getChunk()));
        }

    }

    private class WorldEditLogger {

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
                            PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler(Bukkit.getPlayer(event.getActor().getName()));

                            if (playerHandler == null)
                                throw new IllegalStateException("Actor has null PlayerHandler: " + event.getActor().getName());

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
                                    .addBlock(newState.getLocation(), playerHandler.getProtectionType())
                                    .setCurrentId(playerHandler.getBuildModeId());
                        }

                    // Remove block after processing
                    iterator.remove();
                }
            }, 20L, 20L);
        }

        @Subscribe
        public void logChanges(final EditSessionEvent event) {
            LobsterCraft.logger.info("Stage: " + event.getStage().name() + " actor=" + (event.getActor() == null ? "null" : event.getActor().getName()));
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
}
