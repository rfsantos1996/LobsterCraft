package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.TriggerController;
import com.jabyftw.lobstercraft.player.custom_events.EntityDamageEntityEvent;
import com.jabyftw.lobstercraft.player.custom_events.PlayerDamageEntityEvent;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.services.Service;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.util.NumberConversions;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
public class WorldService extends Service {

    private final static double
            NEAR_BLOCKS_SEARCH_RADIUS_SQUARED = NumberConversions.square(LobsterCraft.configuration.getDouble(ConfigurationValues.WORLD_PROTECTION_PLAYER_NEAR_BLOCKS_SEARCH_RADIUS.toString()));
    private final static int
            MAXIMUM_BLOCKS_RESTORED = LobsterCraft.configuration.getInt(ConfigurationValues.WORLD_PROTECTION_PLAYER_UNLOADER_BLOCKS_RESTORED.toString()),
            NEAR_BLOCKS_REQUIRED = LobsterCraft.configuration.getInt(ConfigurationValues.WORLD_PROTECTION_PLAYER_REQUIRED_NEAR_BLOCKS_AMOUNT.toString()),
            CHUNK_UNLOADER_CHUNKS_PER_RUN = LobsterCraft.configuration.getInt(ConfigurationValues.WORLD_PROTECTION_PLAYER_UNLOADER_CHUNKS_PER_RUN.toString()),
            CHUNK_LOADER_CHUNKS_PER_RUN = LobsterCraft.configuration.getInt(ConfigurationValues.WORLD_PROTECTION_PLAYER_LOADER_CHUNKS_PER_RUN.toString()),
            MINIMUM_LOAD_CHUNK_RANGE = Util.getMinimumChunkRange(ProtectionType.PLAYER_PROTECTION.getSearchDistance()),
            DEFAULT_LOAD_CHUNK_RANGE = NumberConversions.ceil(MINIMUM_LOAD_CHUNK_RANGE * LobsterCraft.configuration.getDouble(ConfigurationValues.WORLD_PROTECTION_PLAYER_LOADER_RANGE_MULTIPLIER.toString()));
    private final static long CHUNK_UNLOADER_PERIOD = LobsterCraft.configuration.getLong(ConfigurationValues.WORLD_PROTECTION_PLAYER_UNLOADER_PERIOD_TICKS.toString());

    // Search types used for protection
    private final static ProtectionType[] DEFAULT_SEARCH_TYPES = new ProtectionType[]{
            // easiest to search
            ProtectionType.CITY_HOUSES_PROTECTION,
            // lowest block amount
            ProtectionType.ADMIN_PROTECTION,
            // high block amount
            ProtectionType.PLAYER_PROTECTION
    };

    // Run-time MySQL connection
    private Connection connection;

    /*
     * World manipulation
     */
    private final boolean worldListIsHandled = LobsterCraft.configuration.getBoolean(ConfigurationValues.WORLD_HANDLING.toString());
    private final HashSet<String> configurationWorldList = new HashSet<>();
    private final ConcurrentHashMap<Byte, World> worlds_id = new ConcurrentHashMap<>(Bukkit.getWorlds().size());

    /*
     * City and administrator managing
     */
    private final ConcurrentHashMap<Integer, String> administrator_constructions_id = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> administrator_constructions_name = new ConcurrentHashMap<>();

    /*
     * Chunk manipulation for player blocks
     */
    private final ChunkUnloader chunkUnloader;
    private final ChunkLoader[] chunkLoaders;
    private final Object chunkLoadingLock = new Object(), chunkUnloadingLock = new Object();
    private final ConcurrentSkipListSet<ChunkLocation> chunksLoading = new ConcurrentSkipListSet<>(), chunksUnloading = new ConcurrentSkipListSet<>(); // Fast checks
    private final ConcurrentLinkedDeque<ChunkLocation> // Used for actually (un)loading
            chunksUnloadingQueue = new ConcurrentLinkedDeque<>(), chunksLoadingQueue_low = new ConcurrentLinkedDeque<>(), chunksLoadingQueue_high = new ConcurrentLinkedDeque<>();

    /*
     * Block manipulation
     */
    protected final ConcurrentHashMap<ChunkLocation, ConcurrentHashMap<BlockLocation, ProtectedBlockLocation>>
            playerProtectedBlocks = new ConcurrentHashMap<>(),
            cityHousesProtectedBlocks = new ConcurrentHashMap<>(),
            adminProtectedBlocks = new ConcurrentHashMap<>();

    public WorldService() throws SQLException {
        super();

        // Add all ignored worlds with lower-cased names
        for (String worldName : LobsterCraft.configuration.getStringList(ConfigurationValues.WORLD_LIST.toString()))
            configurationWorldList.add(worldName.toLowerCase());

        // Initialize some variables
        checkConnection();

        // Update world cache
        updateWorldCache(connection);
        loadAdministratorBlocks(connection);
        // This will load city houses' blocks
        // The City service will link houseId <-> City <-> House Structure: city and its houses will be loaded/created/updated there, we will check any changes to the
        // house and pass it to our blocks (such as deleted houses => delete blocks of same houseId)
        loadCityHousesBlocks(connection);
        // Since every block from deleted houses was removed:
        LobsterCraft.servicesManager.cityService.deletedHouses.clear();

        // Set ChunkUnloader
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                LobsterCraft.plugin,
                (chunkUnloader = new ChunkUnloader()),
                CHUNK_UNLOADER_PERIOD, CHUNK_UNLOADER_PERIOD
        );

        // Set ChunkLoaders
        {
            int threads = LobsterCraft.configuration.getInt(ConfigurationValues.WORLD_PROTECTION_PLAYER_LOADER_THREADS.toString());
            long period = LobsterCraft.configuration.getLong(ConfigurationValues.WORLD_PROTECTION_PLAYER_LOADER_PERIOD_TICKS.toString());
            Random random = new Random();

            chunkLoaders = new ChunkLoader[threads];
            for (int i = 0; i < threads; i++)
                Bukkit.getScheduler().runTaskTimerAsynchronously(
                        LobsterCraft.plugin,
                        (chunkLoaders[i] = new ChunkLoader()),
                        random.nextInt((int) period), period
                );
        }
    }

    @Override
    public void onDisable() {
        try {
            checkConnection();
            // Fill it up with all loaded chunks
            for (World world : getHandledWorlds())
                for (Chunk chunk : world.getLoadedChunks())
                    unloadChunk(chunk);

            // Unload all chunks
            synchronized (chunkUnloadingLock) {
                while (!chunksUnloadingQueue.isEmpty())
                    chunkUnloader.run();
            }

            // Save everything
            saveCityHousesCache(connection);
            saveAdministratorCache(connection);
            updateWorldCache(connection);
            connection.close();
            connection = null;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void checkConnection() throws SQLException {
        boolean connectionInvalid = false;
        // Create connection if it is dead or something
        if (connection == null || (connectionInvalid = !connection.isValid(1))) {
            if (connectionInvalid)
                connection.close();
            connection = LobsterCraft.dataSource.getConnection();
        }
    }

    /*
     * World manipulation
     */

    /**
     * Retrieve Bukkit's World by name
     *
     * @param worldName world's name (not sensitive case)
     * @return null if no world was found
     */
    public World getWorld(@NotNull final String worldName) {
        // Search for the world
        for (World world : Bukkit.getWorlds())
            if (world.getName().equalsIgnoreCase(worldName)) return world;
        return null;
    }

    /**
     * Get Bukkit's World by id
     *
     * @param worldId world's id
     * @return null if no world was found
     */
    public World getWorld(byte worldId) {
        return worlds_id.get(worldId);
    }

    /**
     * Get world's id
     *
     * @param world world to check for id
     * @return null if none found
     */
    public Byte getWorldId(@NotNull final World world) {
        for (Map.Entry<Byte, World> entry : worlds_id.entrySet())
            if (entry.getValue().getName().equalsIgnoreCase(world.getName())) return entry.getKey();
        return null;
    }

    /**
     * @return a collection with all handled worlds
     */
    public Collection<World> getHandledWorlds() {
        return worlds_id.values();
    }

    /**
     * Check if world is ignored by our plugin
     *
     * @param world world to be checked
     * @return true if world is ignored and should not be stored on database
     */
    public boolean isWorldIgnored(@Nullable final World world) {
        // World is ignored if null
        return world == null ||
                // World is ignored if is on the list and shouldn't be handled
                (configurationWorldList.contains(world.getName().toLowerCase()) && !worldListIsHandled) ||
                // World is ignored if isn't on the list and the list should be handled
                (!configurationWorldList.contains(world.getName().toLowerCase()) && worldListIsHandled);
    }

    /**
     * Retrieve construction name from id
     *
     * @param constructionId construction's id
     * @return null if none found
     */
    public String getConstructionName(@NotNull final int constructionId) {
        return administrator_constructions_id.get(constructionId);
    }

    /**
     * Retrieve construction name from exact name
     *
     * @param constructionName construction's exact name
     * @return null if none found
     */
    public Integer getConstructionId(@NotNull final String constructionName) {
        return administrator_constructions_name.get(constructionName.toLowerCase());
    }

    /**
     * Match construction from name
     *
     * @param string construction's name - will return null if length is less than 3 or greater than 45
     * @return null if no construction was found
     */
    public Integer matchConstruction(@NotNull final String string) {
        if (string.length() < 3 || string.length() > 45)
            return null;

        Integer mostEqual = getConstructionId(string);
        int equalSize = 3;

        // Check if name is exact
        if (mostEqual != null) return mostEqual;

        for (Map.Entry<String, Integer> entry : administrator_constructions_name.entrySet()) {
            int thisSize = Util.getEqualityOfNames(string.toCharArray(), entry.getKey().toCharArray());

            if (thisSize >= equalSize) {
                mostEqual = entry.getValue();
                equalSize = thisSize;
            }
        }
        return mostEqual;
    }

    /**
     * Create a administrator construction. Blocks in this constructions will be protected as administrator's (high protection range, including security from entity
     * damage).
     *
     * @param constructionName construction name (length: 4 to 45 characters)
     * @return a response for the CommandSender
     * @throws SQLException in case something goes wrong on MySQL
     */
    public ConstructionCreationResponse createConstruction(@NotNull final String constructionName) throws SQLException {
        String constructionNameLowered = constructionName.toLowerCase();

        // Check if name is valid
        if (!Util.checkStringCharactersAndLength(constructionNameLowered, 4, 45))
            return ConstructionCreationResponse.NAME_INVALID;

        // Check if name is available
        if (matchConstruction(constructionName) != null)
            return ConstructionCreationResponse.NAME_MATCHED_ANOTHER;

        // Insert construction
        {
            // Get connection (let's not use our shared connection)
            Connection connection = LobsterCraft.dataSource.getConnection();
            // Create statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO `minecraft`.`world_constructions` (`constructionName`) VALUES (?);",
                    Statement.RETURN_GENERATED_KEYS
            );
            preparedStatement.setString(1, constructionNameLowered);

            // Execute statement, retrieve generated key
            preparedStatement.execute();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            // Check if there is a generated key
            if (!generatedKeys.next()) throw new SQLException("Construction id not given!");

            // Insert construction to database
            int constructionId = generatedKeys.getInt("constructionId");
            administrator_constructions_id.put(constructionId, constructionNameLowered.toLowerCase());
            administrator_constructions_name.put(constructionNameLowered.toLowerCase(), constructionId);

            // Close everything
            generatedKeys.close();
            preparedStatement.close();
            connection.close();
        }
        return ConstructionCreationResponse.SUCCESSFULLY_CREATED;
    }

    /**
     * Delete a administrator construction. Blocks in this constructions will be marked as unprotected, it's database entry will be removed right away.
     *
     * @param constructionId construction's id, for security and primary key
     * @throws SQLException in case something goes wrong on MySQL
     */
    public boolean removeConstruction(@NotNull final int constructionId) throws SQLException {
        // Check if construction exists
        String constructionName;
        if ((constructionName = getConstructionName(constructionId)) == null)
            return false;

        // Delete construction from database
        {
            // Get connection (let's not use our shared connection)
            Connection connection = LobsterCraft.dataSource.getConnection();
            // Create statement
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `minecraft`.`world_constructions` WHERE `constructionId` = ?;");
            preparedStatement.setInt(1, constructionId);

            // Execute statement
            preparedStatement.execute();

            // Close everything
            preparedStatement.close();
            connection.close();
        }

        // Check if successfully removed
        if (!administrator_constructions_id.remove(constructionId, constructionName) || !administrator_constructions_name.remove(constructionName, constructionId))
            return false;

        // Remove blocks since everything was all right
        {
            for (ConcurrentHashMap<BlockLocation, ProtectedBlockLocation> map : adminProtectedBlocks.values())
                for (ProtectedBlockLocation blockLocation : map.values())
                    // Check if block was protected in this construction
                    if (blockLocation.hasOwner() && blockLocation.getCurrentType() == ProtectionType.ADMIN_PROTECTION && blockLocation.getCurrentId() == constructionId)
                        blockLocation.setOwner(null, null);
        }
        return true;
    }

    /*
     * Listen to world events
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onWorldCreate(WorldLoadEvent event) {
        // Update world cache if world isn't ignored
        if (!isWorldIgnored(event.getWorld()))
            try {
                checkConnection();
                updateWorldCache(connection);
                LobsterCraft.logger.info(Util.appendStrings("Loaded post-initialization world ", event.getWorld().getName()));
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onPortalCreationByEntity(EntityCreatePortalEvent event) {
        // Accept creation by allowed players
        if (event.getEntity() instanceof Player && LobsterCraft.permission.has((Player) event.getEntity(), Permissions.WORLD_CREATE_PORTALS.toString()))
            return;

        // Cancel every other possibility
        event.setCancelled(true);
    }

    /*
     * Block manipulation
     */

    /**
     * Search in every database for a possible protection for this block
     *
     * @param blockLocation block's location
     * @return null if block isn't protected
     * @throws NullPointerException if chunk isn't loaded
     */
    protected ProtectedBlockLocation getBlock(@NotNull final BlockLocation blockLocation) throws NullPointerException {
        ChunkLocation chunkLocation = blockLocation.getChunkLocation();

        // Check admin protection
        if (adminProtectedBlocks.containsKey(chunkLocation)) {
            ProtectedBlockLocation adminProtection = adminProtectedBlocks.get(chunkLocation).get(blockLocation);
            if (adminProtection != null) return adminProtection;
        }

        // Check city houses protection
        if (cityHousesProtectedBlocks.containsKey(chunkLocation)) {
            ProtectedBlockLocation cityHousesProtection = cityHousesProtectedBlocks.get(chunkLocation).get(blockLocation);
            if (cityHousesProtection != null) return cityHousesProtection;
        }

        // Check player protection
        ProtectedBlockLocation playerProtection = playerProtectedBlocks.get(chunkLocation).get(blockLocation);
        if (playerProtection != null) return playerProtection;

        return null;
    }

    private ProtectedBlockLocation getBlock(@NotNull final Location location) {
        return getBlock(new BlockLocation(location));
    }

    /*
     * Listen to block events
     */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreakHighest(BlockBreakEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getWorld())) return;

        OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), OnlinePlayer.OnlineState.LOGGED_IN);
        // Cancel not logged in players
        if (onlinePlayer == null) {
            event.setCancelled(true);
            return;
        }

        // Check near blocks
        if (ProtectionChecker.init(event.getBlock().getLocation())
                // Check this block
                .checkThisBlock(true, true)
                // Check for city houses' blocks near location
                .checkCityHousesBlocksNearThisBlock()
                // Check near cities
                .checkCitiesNearThisBlock(true)
                // Check for administrator blocks near location
                .checkAdministratorBlocksNearThisBlock()
                // Check for near player's blocks
                .checkPlayerBlocksNearThisBlock(true)
                .automatizePlayer(onlinePlayer)
                .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
            event.setCancelled(true);
    }

    // Note that this will ignore checks. All of this MUST (and ARE) be checked on another priority
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockBreakMonitor(BlockBreakEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getWorld())) return;

        ProtectedBlockLocation protectedBlockLocation = getBlock(event.getBlock().getLocation());
        // if block exists, remove owner (conditions were already checked and event was cancelled on Highest priority)
        if (protectedBlockLocation != null) protectedBlockLocation.setOwner(null, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockPlaceHighest(BlockPlaceEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getWorld())) return;

        OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), OnlinePlayer.OnlineState.LOGGED_IN);
        // Cancel not logged in players
        if (onlinePlayer == null) {
            event.setCancelled(true);
            return;
        }

        List<BlockState> blockStates = event instanceof BlockMultiPlaceEvent ?
                ((BlockMultiPlaceEvent) event).getReplacedBlockStates()
                : Collections.singletonList(event.getBlock().getState());

        // Iterate through all blocks
        for (BlockState blockState : blockStates) {
            // Check near blocks
            if (ProtectionChecker.init(blockState.getLocation())
                    // Check this block
                    .checkThisBlock(true, true)
                    // Check for city houses' blocks near location
                    .checkCityHousesBlocksNearThisBlock()
                    // Check near cities
                    .checkCitiesNearThisBlock(true)
                    // Check for administrator blocks near location
                    .checkAdministratorBlocksNearThisBlock()
                    // Check for near player's blocks
                    .checkPlayerBlocksNearThisBlock(true)
                    .automatizePlayer(onlinePlayer)
                    .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockPlaceMonitor(BlockPlaceEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getWorld())) return;

        OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), OnlinePlayer.OnlineState.LOGGED_IN);
        // Cancel not logged in players
        if (onlinePlayer == null) {
            event.setCancelled(true);
            return;
        }

        List<BlockState> blockStates = event instanceof BlockMultiPlaceEvent ?
                ((BlockMultiPlaceEvent) event).getReplacedBlockStates()
                : Collections.singletonList(event.getBlock().getState());

        // TODO: automatize player build mode
        // Iterate through all blocks
        //for (BlockState blockState : blockStates)
        // Insert block
        //LobsterCraft.blockController.addBlock(blockState.getLocation(), onlinePlayer.getProtectionType(), onlinePlayer.getBuildModeId());
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPistonExtendHighest(BlockPistonExtendEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Iterate through all modified blocks
        for (Block block : event.getBlocks()) {
            // Check if worlds need to load protection
            if (!LobsterCraft.blockController.loadNearChunks(block.getLocation()))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPistonExtendMonitor(BlockPistonExtendEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Iterate through all blocks
        for (Block block : event.getBlocks()) {
            Block futureBlock = block.getRelative(event.getDirection());

            ProtectedBlockLocation oldProtection = LobsterCraft.blockController.getBlock(block.getLocation());

            if (oldProtection == null) continue;

            if (!oldProtection.isUndefined())
                LobsterCraft.blockController.addBlock(futureBlock.getLocation(), oldProtection.getType(), oldProtection.getCurrentId());
        }

        ProtectedBlockLocation firstBlockProtection = LobsterCraft.blockController.getBlock(event.getBlock().getRelative(event.getDirection()).getLocation());

        // Remove protection from first block
        if (firstBlockProtection != null) firstBlockProtection.setUndefinedOwner();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPistonRetractHighest(BlockPistonRetractEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Iterate through all modified blocks
        for (Block block : event.getBlocks()) {
            // Check if worlds need to load protection
            if (!LobsterCraft.blockController.loadNearChunks(block.getLocation()))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPistonRetractMonitor(BlockPistonRetractEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Iterate through all blocks
        for (Block block : event.getBlocks()) {
            Block futureBlock = block.getRelative(event.getDirection());

            ProtectedBlockLocation oldProtection = LobsterCraft.blockController.getBlock(block.getLocation());

            if (oldProtection == null) continue;

            if (!oldProtection.isUndefined())
                LobsterCraft.blockController.addBlock(futureBlock.getLocation(), oldProtection.getType(), oldProtection.getCurrentId());
        }

        // Remove protection of the last block on the list
        for (Block block : event.getBlocks()) {
            Block nextBlock = block.getRelative(event.getDirection());

            // It is the last block
            if (!event.getBlocks().contains(nextBlock)) {
                ProtectedBlockLocation firstBlockProtection = LobsterCraft.blockController.getBlock(block.getLocation());

                // Remove protection from the last block
                if (firstBlockProtection != null) firstBlockProtection.setUndefinedOwner();
            }
        }
    }

    /**
     * Listen for fallen blocks: let's remove their protection and ignore them, it's just easier...
     *
     * @param event Bukkit given event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityChangeBlockMonitor(EntityChangeBlockEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getWorld())) return;

        // Check for falling block
        if (event.getEntity().getType() == EntityType.FALLING_BLOCK && ((FallingBlock) event.getEntity()).getMaterial().hasGravity()) {
            // Check if player protection is loaded
            if (!loadNearChunks(new ChunkLocation(event.getBlock().getChunk()), false)) {
                event.setCancelled(true);
                return;
            }
            ProtectedBlockLocation protectedBlockLocation = getBlock(event.getBlock().getLocation());

            // Remove old block protection, if any
            if (protectedBlockLocation != null && protectedBlockLocation.hasOwner())
                protectedBlockLocation.setOwner(null, null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onInteractWithBlockHigh(PlayerInteractEvent event) {
        // Ignore ignored worlds and events that doesn't have blocks
        if (!event.hasBlock() || isWorldIgnored(event.getClickedBlock().getWorld())) return;

        OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), OnlinePlayer.OnlineState.LOGGED_IN);
        // Check if player is online and event interacts with a block
        if (onlinePlayer != null &&
                ProtectionChecker.init(event.getClickedBlock().getLocation())
                        // Check this block
                        .checkThisBlock(true, true)
                        // Check for administrator blocks near location (we do not want player near administration blocks running around interacting with everything)
                        .checkAdministratorBlocksNearThisBlock()
                        .automatizePlayer(onlinePlayer)
                        .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
            event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onToolInteractWithBlockNormal(PlayerInteractEvent event) {
        // Conditions to trigger desired effect (clicked a block, material in hand is specific, world isn't ignored)
        if (event.hasBlock() && event.getMaterial() == TOOL_HAND_MATERIAL && !isWorldIgnored(event.getClickedBlock().getWorld())) {
            Location bukkitBlockLocation;

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                bukkitBlockLocation = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                bukkitBlockLocation = event.getClickedBlock().getLocation();
            } else {
                return;
            }

            PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer());

            // Check if player is spamming
            if (playerHandler.getTriggerController().sendMessageIfNotReady(
                    TriggerController.Condition.PROTECTION_CHECK, "§cAguarde! Não podemos conferir proteção muito constantemente."
            )) return;

            // Check if player protection was loaded
            if (!LobsterCraft.blockController.loadNearChunks(bukkitBlockLocation)) {
                playerHandler.getTriggerController().sendMessageIfConditionReady(TriggerController.Condition.PROTECTION_BEING_LOADED, "§cProteção está sendo carregada...");
                return;
            }

            ProtectedBlockLocation blockLocation = LobsterCraft.blockController.getBlock(bukkitBlockLocation);

            // Check player block, warn player about its state
            if (blockLocation != null)
                playerHandler.sendMessage(getProtectedBlockMessage(blockLocation));
            else
                playerHandler.sendMessage("§cEste bloco não está protegido.");

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onArmorStandManipulateHigh(PlayerArmorStandManipulateEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getRightClicked().getWorld())) return;

        OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), OnlinePlayer.OnlineState.LOGGED_IN);
        // If player isn't online, PreLoginListener will deny the action
        if (onlinePlayer != null &&
                // We can't search for THIS block (it isn't a block)
                // City houses WILL have shared armor stands (city houses are not checked)
                ProtectionChecker.init(event.getRightClicked().getLocation())
                        // Check for administrator blocks near location (we do not want player near administration blocks running around interacting with everything)
                        .checkAdministratorBlocksNearThisBlock()
                        // Check for cities near location (we do not want a stranger messing around our city)
                        .checkCitiesNearThisBlock(true)
                        // Check if block is near a private player house
                        .checkPlayerBlocksNearThisBlock(true)
                        .automatizePlayer(onlinePlayer)
                        .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onArmorStandDamageLow(EntityDamageEntityEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getDamaged().getWorld())) return;

        if (event.getDamaged().getType() == EntityType.ARMOR_STAND) {
            OnlinePlayer onlinePlayer = event.getDamager() instanceof Player ?
                    LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) event.getDamager(), OnlinePlayer.OnlineState.LOGGED_IN) : null;

            // We can't search for THIS block (it isn't a block)
            // City houses WILL have shared armor stands (city houses are not checked)
            if (ProtectionChecker.init(event.getDamaged().getLocation())
                    // Check for administrator blocks near location (we do not want player near administration blocks running around interacting with everything)
                    .checkAdministratorBlocksNearThisBlock()
                    // Check for cities near location (we do not want a stranger messing around our city)
                    .checkCitiesNearThisBlock(true)
                    // Check if block is near a private player house
                    .checkPlayerBlocksNearThisBlock(onlinePlayer != null)
                    // Accepts null player
                    .automatizePlayer(onlinePlayer)
                    .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onCreatureSpawnHighest(CreatureSpawnEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getLocation().getWorld())) return;

        // Allow safe mobs
        if (!(event.getEntity() instanceof Monster))
            return;

        // Check for administrator blocks near location
        if (ProtectionChecker.init(event.getLocation())
                .checkAdministratorBlocksNearThisBlock()
                .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityPrimeExplosionHighest(ExplosionPrimeEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getEntity().getLocation().getWorld())) return;

        // Do not even prime when protection isn't loaded
        if (!loadNearChunks(new ChunkLocation(event.getEntity().getLocation().getChunk()), false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityExplodeHighest(EntityExplodeEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getLocation().getWorld())) return;

        // We won't protect city's buildings
        if (ProtectionChecker.init(event.getLocation())
                // Check for administrator blocks near location
                .checkAdministratorBlocksNearThisBlock()
                .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE) {
            event.setCancelled(true);
            return;
        }

        // Load player chunks for next protection step
        if (!loadNearChunks(new ChunkLocation(event.getLocation().getChunk()), false)) {
            event.setCancelled(true);
            return;
        }

        Iterator<Block> iterator = event.blockList().iterator();
        // Iterate through all blocks
        while (iterator.hasNext()) {
            Block next = iterator.next();

            if (ProtectionChecker.init(next.getLocation())
                    .checkThisBlock(true, false)
                    .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
                iterator.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onDoorBreakHighest(EntityBreakDoorEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getWorld())) return;

        // We won't protect city's door
        if (ProtectionChecker.init(event.getBlock().getLocation())
                .checkThisBlock(true, false)
                // Check for administrator blocks near location
                .checkAdministratorBlocksNearThisBlock()
                // Check for city blocks only (do not break player houses)
                .checkCityHousesBlocksNearThisBlock()
                // Check if block is near a private player house
                .checkPlayerBlocksNearThisBlock(false)
                .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockIgnite(BlockIgniteEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getWorld())) return;

        OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), OnlinePlayer.OnlineState.LOGGED_IN);
        // We won't protect city's buildings but this block and administrator blocks
        if (ProtectionChecker.init(event.getBlock().getLocation())
                .checkThisBlock(true, false)
                // Check for administrator blocks near location
                .checkAdministratorBlocksNearThisBlock()
                // Check for near cities
                .checkCitiesNearThisBlock(true)
                // Check for city houses
                .checkCityHousesBlocksNearThisBlock()
                // Check for other players' blocks
                .checkPlayerBlocksNearThisBlock(true)
                .automatizePlayer(onlinePlayer)
                .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBurnHighest(BlockBurnEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getLocation().getWorld())) return;

        // We won't protect city's buildings but this block and administrator blocks
        if (ProtectionChecker.init(event.getBlock().getLocation())
                .checkThisBlock(true, false)
                // Check for administrator blocks near location
                .checkAdministratorBlocksNearThisBlock()
                .getResponse() != ProtectionChecker.ProtectionCheckerResponse.BLOCK_IS_SAFE)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onLeavesDecayHighest(LeavesDecayEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getWorld())) return;

        // Load player protection
        if (!loadNearChunks(new ChunkLocation(event.getBlock().getChunk()), false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onLeavesDecayMonitor(LeavesDecayEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getBlock().getWorld())) return;

        ProtectedBlockLocation blockLocation = getBlock(event.getBlock().getLocation());
        if (blockLocation != null)
            // Remove protection even from administrator blocks
            blockLocation.setOwner(null, null);
    }

    // As tested, this isn't spammed (worse, it took some minutes to re-trigger the event)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onStructureGrowHighest(StructureGrowEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getLocation().getWorld())) return;

        // Load player protection
        if (!loadNearChunks(new ChunkLocation(event.getLocation().getChunk()), false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onStructureGrowMonitor(StructureGrowEvent event) {
        // Ignore ignored worlds
        if (isWorldIgnored(event.getLocation().getWorld())) return;

        ProtectedBlockLocation baseBlock = getBlock(event.getLocation());
        // Return if base isn't protected
        if (baseBlock == null || !baseBlock.hasOwner()) return;

        // Iterate through all blocks
        for (BlockState blockState : event.getBlocks())
            // Protect block
            new ProtectedBlockLocation(blockState.getLocation()).setOwner(baseBlock.getCurrentType(), baseBlock.getCurrentId());
    }

    // DEBUGGING
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onFormMonitor(BlockFormEvent event) {
        // Bring attention, since I want to see what is this
        for (int i = 0; i < 5; i++)
            // UNKNOWN => probably: ice spreading
            LobsterCraft.logger.info(Util.appendStrings("blockMaterial: ", event.getBlock().getType().name(), " formedMaterial: ", event.getNewState().getType().name(),
                    Util.locationToString(event.getBlock().getLocation())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockSpreadHighest(BlockSpreadEvent event) {
        // Check if world or types are ignored
        if (event.getSource().getType() == Material.GRASS || event.getSource().getType() == Material.FIRE || isWorldIgnored(event.getSource().getWorld()))
            return;

        // Cancel if player blocks are not loaded
        if (loadNearChunks(event.getBlock().getLocation(), false))
            event.setCancelled(true);
        // things spreading: grass, vine, fire, mushrooms
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockSpreadMonitor(BlockSpreadEvent event) {
        // Check if world or types are ignored
        if (event.getSource().getType() == Material.GRASS || event.getSource().getType() == Material.FIRE || isWorldIgnored(event.getSource().getWorld()))
            return;

        ProtectedBlockLocation baseBlock = getBlock(event.getSource().getLocation());
        // Return if source isn't protected
        if (baseBlock == null || !baseBlock.hasOwner()) return;

        // Protect new block
        new ProtectedBlockLocation(event.getBlock().getLocation()).setOwner(baseBlock.getCurrentType(), baseBlock.getCurrentId());
    }

    /*
     * Chunk manipulation
     */

    /**
     * Load PLAYER protection chunks.
     *
     * @param chunkLocation chunk location to center the load
     * @param loadExtra     should be true for player interactions (this is used only for )
     * @return true if all chunks are loaded
     */
    protected boolean loadNearChunks(@NotNull final ChunkLocation chunkLocation, boolean loadExtra) {
        Set<ChunkLocation>
                minimumSizeNearChunks = chunkLocation.getNearChunks(MINIMUM_LOAD_CHUNK_RANGE),
                defaultSizeNearChunks = loadExtra ? chunkLocation.getNearChunks(DEFAULT_LOAD_CHUNK_RANGE) : minimumSizeNearChunks;

        // Check for not loaded ones
        for (ChunkLocation location : defaultSizeNearChunks)
            // Insert to set if it isn't loaded
            if (!chunksUnloading.contains(location) && !playerProtectedBlocks.containsKey(location)) {
                synchronized (chunkLoadingLock) {
                    if (!chunksLoading.contains(location)) {
                        if (minimumSizeNearChunks.contains(location))
                            chunksLoadingQueue_high.add(location);
                        else
                            chunksLoadingQueue_low.add(location);
                        chunksLoading.add(location);
                    } else
                        // Since the block is already loading, lets check if it is on low priority queue. If it is, move it to high priority because it is a
                        // "minimumSize" chunk location
                        if (minimumSizeNearChunks.contains(location) && chunksLoadingQueue_low.contains(location)) {
                            chunksLoadingQueue_low.remove(location);
                            chunksLoadingQueue_high.add(location);
                        }
                }
            }

        // Check if the minimum are required, return false if it is
        for (ChunkLocation location : minimumSizeNearChunks)
            if (chunksLoading.contains(location) || chunksUnloading.contains(location) || !playerProtectedBlocks.containsKey(location))
                return false;
        return true;
    }

    private boolean loadNearChunks(@NotNull final Location location, boolean loadExtra) {
        return loadNearChunks(new ChunkLocation(location.getChunk()), loadExtra);
    }

    /**
     * @param chunkLocation chunk to be unloaded safely since we need to check first
     * @return true if chunk will be unloaded
     */
    private boolean unloadChunk(@NotNull ChunkLocation chunkLocation) {
        synchronized (chunkUnloadingLock) {
            // Insert to queue if isn't already unloading and if chunk is loaded
            if (!chunksUnloading.contains(chunkLocation) && playerProtectedBlocks.containsKey(chunkLocation)) {
                this.chunksUnloadingQueue.add(chunkLocation);
                this.chunksUnloading.add(chunkLocation);
                return true;
            }
        }
        return false;
    }

    private boolean unloadChunk(@NotNull Chunk chunk) {
        return unloadChunk(new ChunkLocation(chunk));
    }

    /*
     * Database handling
     */

    private void updateWorldCache(@NotNull Connection connection) throws SQLException {
        HashSet<World> pendingInsertion = new HashSet<>();
        HashSet<Byte> pendingDeletion = new HashSet<>();

        // Fill pendingInsertion with worlds that aren't ignored
        for (World world : Bukkit.getWorlds())
            if (!isWorldIgnored(world)) pendingInsertion.add(world);

        // Check current database
        {
            // Prepare statement and execute query
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.worlds;");
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through all results
            while (resultSet.next()) {
                String worldName = resultSet.getString("worldName").toLowerCase();
                byte worldId = resultSet.getByte("worldId");
                World foundWorld = getWorld(worldName);

                // Exists a world on database that doesn't exists on Bukkit or was supposed to be ignored => delete
                if (foundWorld == null || isWorldIgnored(foundWorld)) {
                    pendingDeletion.add(worldId);
                } else {
                    // Check if world is loaded
                    if (!worlds_id.containsKey(worldId))
                        worlds_id.put(worldId, foundWorld);

                    // Remove from pending insertions because it is already loaded
                    pendingInsertion.remove(foundWorld);
                }
            }

            // Close statement
            resultSet.close();
            preparedStatement.close();
        }

        // Insert pending insertions
        if (!pendingInsertion.isEmpty()) {
            // Create query
            StringBuilder stringBuilder = new StringBuilder("INSERT INTO `minecraft`.`worlds` (`worldName`) VALUES ");
            Iterator<World> iterator = pendingInsertion.iterator();

            // Iterate through all items
            while (iterator.hasNext()) {
                World world = iterator.next();
                // Append world
                stringBuilder.append("('").append(world.getName().toLowerCase()).append("')");
                if (iterator.hasNext()) stringBuilder.append(", ");
            }

            // Close query
            stringBuilder.append(";");

            // Prepare, execute and close statement
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
            preparedStatement.execute();
            preparedStatement.close();
        }

        // Retrieve pending insertions's world id
        if (!pendingInsertion.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder("SELECT * FROM `minecraft`.`worlds` WHERE `worldName` IN (");

            Iterator<World> iterator = pendingInsertion.iterator();
            // Iterate through all items (again)
            while (iterator.hasNext()) {
                World world = iterator.next();

                // Append their name
                stringBuilder.append('\'').append(world.getName().toLowerCase()).append('\'');
                if (iterator.hasNext()) stringBuilder.append(", ");

                // Doesn't need them anymore, remove it
                iterator.remove();
            }

            // Close query
            stringBuilder.append(");");

            // Prepare statement and execute query
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
            ResultSet resultSet = preparedStatement.executeQuery();

            World world;
            // Iterate through inserted worlds
            while (resultSet.next())
                // Insert them to world map
                if ((world = getWorld(resultSet.getString("worldName"))) != null)
                    worlds_id.put(resultSet.getByte("worldId"), world);

            // Close query and statement
            resultSet.close();
            preparedStatement.close();
        }

        // Delete pending deletions
        if (!pendingDeletion.isEmpty()) {
            // Create query
            StringBuilder stringBuilder = new StringBuilder("DELETE FROM `minecraft`.`worlds` WHERE `worldId` IN (");

            Iterator<Byte> iterator = pendingDeletion.iterator();
            // Iterate through all items
            while (iterator.hasNext()) {
                stringBuilder.append(iterator.next());
                if (iterator.hasNext()) stringBuilder.append(", ");

                // Doesn't need it anymore, remove
                iterator.remove();
            }

            // Close query
            stringBuilder.append(");");

            // Prepare, execute and close statement
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
            preparedStatement.execute();
            preparedStatement.close();
        }
    }

    private void loadAdministratorBlocks(@NotNull final Connection connection) throws SQLException {
        long start = System.nanoTime();
        int blockSize = 0;

        // Load constructions
        {
            // Prepare and execute statement
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.world_constructions;");
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through constructions
            while (resultSet.next()) {
                int constructionId = resultSet.getInt("constructionId");
                String constructionName = resultSet.getString("constructionName").toLowerCase();

                // Add constructions
                administrator_constructions_id.put(constructionId, constructionName);
                administrator_constructions_name.put(constructionName, constructionId);
            }

            // Close everything
            resultSet.close();
            preparedStatement.close();
        }

        // Load blocks
        {
            // Prepare statement and execute query => we're looking just for ADMINISTRATOR blocks
            PreparedStatement preparedStatement = connection.prepareStatement(Util.appendStrings("SELECT * FROM minecraft.world_blocks WHERE ownerType = ",
                    ProtectionType.ADMIN_PROTECTION.getTypeId(), ';'));
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through all blocks
            while (resultSet.next()) {
                // Create chunk
                ChunkLocation chunkLocation = new ChunkLocation(
                        resultSet.getByte("worlds_worldId"),
                        resultSet.getInt("chunkX"),
                        resultSet.getInt("chunkZ")
                );

                // Delete ignored worlds' blocks
                if (chunkLocation.worldIsIgnored()) {
                    resultSet.deleteRow();
                    continue;
                }

                // Insert chunk if not there
                if (!adminProtectedBlocks.containsKey(chunkLocation))
                    adminProtectedBlocks.put(chunkLocation, new ConcurrentHashMap<>());

                int constructionId = resultSet.getInt("ownerId");
                ProtectedBlockLocation protectedBlockLocation = new ProtectedBlockLocation(
                        ProtectionType.ADMIN_PROTECTION,
                        constructionId,
                        chunkLocation,
                        resultSet.getByte("blockX"),
                        resultSet.getShort("blockY"),
                        resultSet.getByte("blockZ")
                )
                        // This will insert the block on the block map
                        .setOwner(ProtectionType.ADMIN_PROTECTION, constructionId);
                // If construction was deleted, remove block
                // We can't check if the world is ignored here because we don't even listen to events from other world (for example, on unloading chunks that will save
                // and update the blocks on database)
                if (administrator_constructions_id.get(constructionId) == null)
                    protectedBlockLocation.setOwner(null, null);
                blockSize++;
            }

            // Close everything
            resultSet.close();
            preparedStatement.close();
        }

        if (blockSize > 0)
            LobsterCraft.logger.info(Util.appendStrings("It took ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                    "ms to search for ", blockSize, " administrator blocks."));
    }

    private void loadCityHousesBlocks(@NotNull final Connection connection) throws SQLException {
        long start = System.nanoTime();
        int blockSize = 0;

        // Load blocks
        {
            // Prepare statement and execute query => we're looking just for ADMINISTRATOR blocks
            PreparedStatement preparedStatement = connection.prepareStatement(Util.appendStrings("SELECT * FROM minecraft.world_blocks WHERE ownerType = ",
                    ProtectionType.CITY_HOUSES_PROTECTION.getTypeId(), ';'));
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through all blocks
            while (resultSet.next()) {
                // Create chunk
                ChunkLocation chunkLocation = new ChunkLocation(
                        resultSet.getByte("worlds_worldId"),
                        resultSet.getInt("chunkX"),
                        resultSet.getInt("chunkZ")
                );

                // Delete ignored worlds' blocks
                if (chunkLocation.worldIsIgnored()) {
                    resultSet.deleteRow();
                    continue;
                }

                // Insert chunk if not there
                if (!cityHousesProtectedBlocks.containsKey(chunkLocation))
                    cityHousesProtectedBlocks.put(chunkLocation, new ConcurrentHashMap<>());

                int houseId = resultSet.getInt("ownerId");
                ProtectedBlockLocation protectedBlockLocation = new ProtectedBlockLocation(
                        ProtectionType.CITY_HOUSES_PROTECTION,
                        houseId,
                        chunkLocation,
                        resultSet.getByte("blockX"),
                        resultSet.getShort("blockY"),
                        resultSet.getByte("blockZ")
                )
                        // This will insert the block on the block map
                        .setOwner(ProtectionType.CITY_HOUSES_PROTECTION, houseId);

                // If house (city) was deleted, remove block
                if (LobsterCraft.servicesManager.cityService.deletedHouses.contains(houseId))
                    protectedBlockLocation.setOwner(null, null);
                blockSize++;
            }

            // Close everything
            resultSet.close();
            preparedStatement.close();

            if (blockSize > 0)
                LobsterCraft.logger.info(Util.appendStrings("It took ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                        "ms to search for ", blockSize, " administrator blocks."));
        }
    }

    private void saveAdministratorCache(@NotNull final Connection connection) throws SQLException {
        int blockSize = 0;
        long start = System.nanoTime();
        final HashSet<ProtectedBlockLocation>
                blocksToInsert = new HashSet<>(),
                blocksToUpdate = new HashSet<>(),
                blocksToDelete = new HashSet<>();

        // Iterate through all blocks
        for (ConcurrentHashMap<BlockLocation, ProtectedBlockLocation> map : adminProtectedBlocks.values()) {
            for (ProtectedBlockLocation blockLocation : map.values()) {
                // Skip non-changed blocks
                if (!blockLocation.getDatabaseState().shouldSave()) continue;

                // Detect action
                switch (blockLocation.getDatabaseState()) {
                    default:
                        blockSize++;
                    case INSERT_TO_DATABASE:
                        blocksToInsert.add(blockLocation);
                        break;
                    case UPDATE_DATABASE:
                        blocksToUpdate.add(blockLocation);
                        break;
                    case DELETE_FROM_DATABASE:
                        blocksToDelete.add(blockLocation);
                        break;
                }
            }
        }

        // Update database
        try {
            insertBlocks(connection, blocksToInsert);
            updateBlocks(connection, blocksToUpdate);
            deleteBlocks(connection, blocksToDelete);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        if (blockSize > 0)
            LobsterCraft.logger.info(Util.appendStrings("It took ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                    "ms to update ", blockSize, " changed administrator blocks."));
    }

    private void saveCityHousesCache(@NotNull final Connection connection) throws SQLException {
        int blockSize = 0;
        long start = System.nanoTime();
        final HashSet<ProtectedBlockLocation>
                blocksToInsert = new HashSet<>(),
                blocksToUpdate = new HashSet<>(),
                blocksToDelete = new HashSet<>();

        boolean hasDeletedHouses = LobsterCraft.servicesManager.cityService.deletedHouses.isEmpty();
        // Iterate through all blocks
        for (ConcurrentHashMap<BlockLocation, ProtectedBlockLocation> map : cityHousesProtectedBlocks.values()) {
            for (ProtectedBlockLocation blockLocation : map.values()) {
                // Check if house was deleted during run time
                if (hasDeletedHouses && blockLocation.getCurrentId() != null &&
                        LobsterCraft.servicesManager.cityService.deletedHouses.contains(blockLocation.getCurrentId()))
                    // Unprotect house => it'll be delete from database
                    blockLocation.setOwner(null, null);

                // Skip non-changed blocks
                if (!blockLocation.getDatabaseState().shouldSave()) continue;

                // Detect action
                switch (blockLocation.getDatabaseState()) {
                    default:
                        blockSize++;
                    case INSERT_TO_DATABASE:
                        blocksToInsert.add(blockLocation);
                        break;
                    case UPDATE_DATABASE:
                        blocksToUpdate.add(blockLocation);
                        break;
                    case DELETE_FROM_DATABASE:
                        blocksToDelete.add(blockLocation);
                        break;
                }
            }
        }

        // Update database
        try {
            insertBlocks(connection, blocksToInsert);
            updateBlocks(connection, blocksToUpdate);
            deleteBlocks(connection, blocksToDelete);

            // Delete houses
            if (!LobsterCraft.servicesManager.cityService.deletedHouses.isEmpty()) {
                // Prepare statement
                PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `minecraft`.`city_house_locations` WHERE `houseId` = ?;");
                // Iterate through all deleted houses
                for (Integer houseId : LobsterCraft.servicesManager.cityService.deletedHouses) {
                    // Set variable
                    preparedStatement.setInt(1, houseId);
                    // Add batch
                    preparedStatement.addBatch();
                }
                // Execute and close statement
                preparedStatement.execute();
                preparedStatement.close();

                // Since all blocks and house entries were deleted, clear Set
                LobsterCraft.servicesManager.cityService.deletedHouses.clear();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }


        if (blockSize > 0)
            LobsterCraft.logger.info(Util.appendStrings("It took ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                    "ms to update ", blockSize, " changed city houses' blocks."));
    }

    private static void insertBlocks(@NotNull final Connection connection, @NotNull final Set<ProtectedBlockLocation> blocksToInsert) throws SQLException {
        if (!blocksToInsert.isEmpty()) {
            // Start statement
            StringBuilder stringBuilder = new StringBuilder(
                    "INSERT INTO `minecraft`.`world_blocks` (`worlds_worldId`, `chunkX`, `chunkZ`, `blockX`, `blockY`, `blockZ`, `ownerType`, `ownerId`) VALUES "
            );

            boolean first = true;
            // Iterate through all blocks
            for (ProtectedBlockLocation blockLocation : blocksToInsert) {
                ChunkLocation chunkLocation = blockLocation.getChunkLocation();

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
                        .append(blockLocation.getCurrentType().getTypeId()).append(',')
                        .append(blockLocation.getCurrentId())
                        .append(')');
                // Update block instance
                blockLocation.originalType = blockLocation.currentType;
                blockLocation.originalId = blockLocation.currentId;
            }

            // Close statement
            stringBuilder.append(';');
            String sqlStatement = stringBuilder.toString();
            LobsterCraft.logger.config(Util.appendStrings("Security: ", sqlStatement));

            // Prepare, execute and close statement
            PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
            preparedStatement.execute();
            preparedStatement.close();

            // Clear the list
            blocksToInsert.clear();
        }
    }

    private static void updateBlocks(@NotNull final Connection connection, @NotNull final Set<ProtectedBlockLocation> blocksToUpdate) throws SQLException {
        if (!blocksToUpdate.isEmpty()) {
            // Start statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "UPDATE `minecraft`.`world_blocks` SET  `ownerType` = ?, `ownerId` = ? " +
                            "WHERE `worlds_worldId` = ? AND `chunkX` = ? AND `chunkZ` = ? AND `blockX` = ? AND `blockY` = ? AND `blockZ` = ?;"
            );

            // Iterate through blocks
            for (ProtectedBlockLocation blockLocation : blocksToUpdate) {
                ChunkLocation chunkLocation = blockLocation.getChunkLocation();

                // Append information
                preparedStatement.setByte(1, blockLocation.getCurrentType().getTypeId());
                preparedStatement.setLong(2, blockLocation.getCurrentId());

                // Append where-clause
                preparedStatement.setLong(3, chunkLocation.getWorldId());
                preparedStatement.setInt(4, chunkLocation.getChunkX());
                preparedStatement.setInt(5, chunkLocation.getChunkZ());

                preparedStatement.setByte(6, blockLocation.getRelativeX());
                preparedStatement.setShort(7, blockLocation.getY());
                preparedStatement.setByte(8, blockLocation.getRelativeZ());

                // Update block instance
                blockLocation.originalType = blockLocation.currentType;
                blockLocation.originalId = blockLocation.currentId;
                // Add batch
                preparedStatement.addBatch();
            }

            // Execute and close statement
            preparedStatement.execute();
            preparedStatement.close();

            // Clear the list
            blocksToUpdate.clear();
        }
    }

    private static void deleteBlocks(@NotNull final Connection connection, @NotNull final Set<ProtectedBlockLocation> blocksToDelete) throws SQLException {
        if (!blocksToDelete.isEmpty()) {
            // Start statement
            StringBuilder stringBuilder = new StringBuilder(
                    "DELETE FROM `minecraft`.`world_blocks` WHERE (`worlds_worldId`, `chunkX`, `chunkZ`, `blockX`, `blockY`, `blockZ`) IN ("
            );

            boolean first = true;
            // Iterate through all chunks
            for (ProtectedBlockLocation blockLocation : blocksToDelete) {
                ChunkLocation chunkLocation = blockLocation.getChunkLocation();

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
                blockLocation.originalType = blockLocation.currentType;
                blockLocation.originalId = blockLocation.currentId;
            }

            // Close statement
            stringBuilder.append(");");
            String sqlStatement = stringBuilder.toString();
            LobsterCraft.logger.info(Util.appendStrings("Security: ", sqlStatement));

            // Prepare, execute and close statement
            PreparedStatement preparedStatement = connection.prepareStatement(sqlStatement);
            preparedStatement.execute();
            preparedStatement.close();

            // Clear the list
            blocksToDelete.clear();
        }
    }

    /*
     * Some classes
     */

    public enum ConstructionCreationResponse {
        SUCCESSFULLY_CREATED,
        NAME_MATCHED_ANOTHER,
        NAME_INVALID
    }

    private class ChunkLoader implements Runnable {

        private final HashSet<ChunkLocation> chunksProcessing = new HashSet<>();

        @Override
        public void run() {
            // Create queries => we're here just to load PLAYER blocks!
            StringBuilder stringBuilder = new StringBuilder("SELECT * FROM minecraft.world_blocks WHERE ownerType = ").append(ProtectionType.PLAYER_PROTECTION.getTypeId())
                    .append(" AND (worlds_worldId, chunkX, chunkZ) IN (");

            synchronized (chunkLoadingLock) {
                // Ignore empty loading queue (must be synchronized to the iterator we're using)
                if (chunksLoading.isEmpty()) return;

                // Lets ask for high priority first:
                int index = 0;

                while (index < CHUNK_LOADER_CHUNKS_PER_RUN) {
                    // Get high priority chunk
                    ChunkLocation chunkLocation = chunksLoadingQueue_high.pollFirst();
                    // Then low priority
                    if (chunkLocation == null) chunksLoadingQueue_low.pollFirst();
                    // We don't have anything to do
                    if (chunkLocation == null) return;

                    // Check if this chunk isn't the first one
                    if (index > 0) stringBuilder.append(", ");

                    // Append chunk information
                    stringBuilder
                            .append('(')
                            .append(chunkLocation.getWorldId()).append(", ")
                            .append(chunkLocation.getChunkX()).append(", ")
                            .append(chunkLocation.getChunkZ())
                            .append(')');

                    chunksProcessing.add(chunkLocation);
                    index++;
                }

                // Close query
                stringBuilder.append(");");
            }

            try {
                // Prepare connection
                checkConnection();

                // Prepare statement and execute query
                PreparedStatement preparedStatement = connection.prepareStatement(stringBuilder.toString());
                ResultSet resultSet = preparedStatement.executeQuery();

                int blockSize = 0;
                long startTime = System.nanoTime();

                // Get results
                while (resultSet.next()) {
                    // Create chunk
                    ChunkLocation chunkLocation = new ChunkLocation(
                            resultSet.getByte("worlds_worldId"),
                            resultSet.getInt("chunkX"),
                            resultSet.getInt("chunkZ")
                    );

                    // Delete ignored worlds' blocks
                    if (chunkLocation.worldIsIgnored()) {
                        resultSet.deleteRow();
                        continue;
                    }

                    // Insert chunk location so we don't need to putIfAbsent on every block through setOwner
                    if (!playerProtectedBlocks.containsKey(chunkLocation))
                        playerProtectedBlocks.put(chunkLocation, new ConcurrentHashMap<>());

                    int ownerId = resultSet.getInt("ownerId");
                    // Create block and set owner => it'll insert him on block map
                    // Note: protection type is only player protection
                    new ProtectedBlockLocation(
                            ProtectionType.PLAYER_PROTECTION,
                            ownerId,
                            chunkLocation,
                            resultSet.getByte("blockX"),
                            resultSet.getShort("blockY"),
                            resultSet.getByte("blockZ")
                    ).setOwner(ProtectionType.PLAYER_PROTECTION, ownerId);

                    // Add protected block
                    blockSize++;
                }

                // Make sure chunk is loaded
                synchronized (chunkLoadingLock) {
                    chunksLoading.removeAll(chunksProcessing);
                    // Set as loaded
                    for (ChunkLocation processingChunk : chunksProcessing)
                        playerProtectedBlocks.put(processingChunk, new ConcurrentHashMap<>());
                    // Clear list
                    chunksProcessing.clear();
                }

                // Close everything
                resultSet.close();
                preparedStatement.close();

                double timeTakenMilliseconds = (double) (System.nanoTime() - startTime) / (double) TimeUnit.MILLISECONDS.toNanos(1);
                if (blockSize > 0 && timeTakenMilliseconds > 4.0d)
                    LobsterCraft.logger.info(Util.appendStrings("It took ", Util.formatDecimal(timeTakenMilliseconds), "ms to search for ", blockSize, " blocks."));
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    private class ChunkUnloader implements Runnable {

        private final BlockRestorer blockRestorer = new BlockRestorer();
        private final ConcurrentLinkedDeque<ProtectedBlockLocation> blocksToRestore = new ConcurrentLinkedDeque<>(); // limit by MAXIMUM_BLOCKS_RESTORED
        private final HashSet<ChunkLocation> chunksToProcess = new HashSet<>(), chunksProcessing = new HashSet<>();
        private final HashSet<ProtectedBlockLocation>
                blocksToInsert = new HashSet<>(),
                blocksToUpdate = new HashSet<>(),
                blocksToDelete = new HashSet<>();

        @Override
        public void run() {
            int blocksChanged = 0, temporaryBlocksProcessed = 0, temporaryBlocksRemoved = 0;
            long start = System.nanoTime();

            // Ignore empty unloading queue
            // Note: this may return empty even if it isn't
            synchronized (chunksUnloadingQueue) {
                int currentIndex = 0;
                while (currentIndex < CHUNK_UNLOADER_CHUNKS_PER_RUN) {
                    ChunkLocation chunkLocation = chunksUnloadingQueue.poll();
                    // Poll until limit of chunks
                    if (chunkLocation != null) {
                        if (playerProtectedBlocks.get(chunkLocation) != null) {
                            chunksToProcess.add(chunkLocation);
                            currentIndex++;
                        }
                    } else {
                        // There aren't more chunks to unload, let's start working
                        break;
                    }
                }
            }

            // Check if we have job to do
            if (chunksToProcess.isEmpty()) return;

            Iterator<ChunkLocation> iterator = chunksToProcess.iterator();
            while (iterator.hasNext()) {
                ChunkLocation processingChunk = iterator.next();

                // Check if near chunks are loaded
                boolean chunksNotReady = !loadNearChunks(processingChunk, false);

                // Check if we manually need to load chunks
                if (LobsterCraft.serverClosing && chunksNotReady) {
                    LobsterCraft.logger.config("Trying to manually load chunk...");
                    while (!loadNearChunks(processingChunk, false))
                        // We just need one working
                        chunkLoaders[0].run();
                } else if (chunksNotReady) {
                    // Skip this chunk since we don't need to load it
                    continue;
                } else {
                    chunksProcessing.add(processingChunk);
                    iterator.remove();
                }

                ConcurrentHashMap<BlockLocation, ProtectedBlockLocation> protectedBlocks = playerProtectedBlocks.get(processingChunk);
                // Process player blocks
                if (protectedBlocks != null)
                    for (ProtectedBlockLocation blockLocation : protectedBlocks.values()) {
                        // Skip neutral blocks
                        if (!blockLocation.getDatabaseState().shouldSave()) continue;
                        blocksChanged++;

                        // Check database state so we can separate on database update
                        if (blockLocation.getDatabaseState() == DatabaseState.INSERT_TO_DATABASE) {
                            // Check if block is temporary
                            if (blockLocation.isTemporaryBlock()) {
                                AtomicInteger numberOfProtectedBlocks = new AtomicInteger(0);

                                blockLocation.getChunkLocation().getNearChunks(MINIMUM_LOAD_CHUNK_RANGE).parallelStream().forEach(chunkLocation -> {
                                    for (ProtectedBlockLocation otherBlock : playerProtectedBlocks.get(chunkLocation).values()) {
                                        // Skip if found enough
                                        if (numberOfProtectedBlocks.get() > NEAR_BLOCKS_REQUIRED) break;

                                        // Skip blocks not protected, this same block and far away blocks
                                        if (!otherBlock.hasOwner() || otherBlock.equals(blockLocation) ||
                                                otherBlock.distanceSquared(blockLocation) > NEAR_BLOCKS_SEARCH_RADIUS_SQUARED)
                                            continue;

                                        // This is a valid block, increment and check if enough
                                        if (numberOfProtectedBlocks.incrementAndGet() > NEAR_BLOCKS_REQUIRED)
                                            break;
                                    }
                                });

                                if (numberOfProtectedBlocks.get() < NEAR_BLOCKS_REQUIRED) {
                                    // Remove block protection
                                    blockLocation.setOwner(null, null);
                                    // Note: a block ISN'T restored when there is enough blocks near it. Doesn't depend on their state (temporary or permanent)
                                    blocksToRestore.add(blockLocation);
                                    temporaryBlocksRemoved++;
                                    // Do not insert block on "blocksToInsert" or set it as permanent, continue to the next one
                                    continue;
                                }
                            }
                            // If block isn't temporary, add for insertion
                            blocksToInsert.add(blockLocation);
                            // Set block permanent
                            blockLocation.setAsPermanentBlock();
                            temporaryBlocksProcessed++;
                        } else if (blockLocation.getDatabaseState() == DatabaseState.UPDATE_DATABASE) {
                            blocksToUpdate.add(blockLocation);
                        } else if (blockLocation.getDatabaseState() == DatabaseState.DELETE_FROM_DATABASE) {
                            blocksToDelete.add(blockLocation);
                        }
                    }
            }

            try {
                // Make sure connection is alive
                checkConnection();

                // Update database
                insertBlocks(connection, blocksToInsert);
                updateBlocks(connection, blocksToUpdate);
                deleteBlocks(connection, blocksToDelete);
            } catch (SQLException exception) {
                exception.printStackTrace();
            }

            // Unload chunks
            synchronized (chunkUnloadingLock) {
                // First, remove blocks
                chunksProcessing.forEach(playerProtectedBlocks::remove);
                // Then remove from "chunksUnloading"
                chunksUnloading.removeAll(chunksProcessing);
            }

            // Schedule a block restorer run
            if (LobsterCraft.serverClosing)
                // Run until we have restored everything
                while (!blocksToRestore.isEmpty()) blockRestorer.run();
            else
                Bukkit.getScheduler().runTask(LobsterCraft.plugin, blockRestorer);

            // Just "debug" something useful
            if (blocksChanged > 0)
                LobsterCraft.logger.info(Util.appendStrings("Took us ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                        "ms to change ", blocksChanged, " blocks storage: ", temporaryBlocksProcessed, " temporary blocks processed and ", temporaryBlocksRemoved,
                        " temporary blocks removed."));
        }


        protected class BlockRestorer implements Runnable {

            @Override
            public void run() {
                long start = System.nanoTime();
                int successfulRestores = 0, failedRestores = 0;

                int index = 0;
                // Iterate until limit
                while (index < MAXIMUM_BLOCKS_RESTORED) {
                    ProtectedBlockLocation protectedBlockLocation = blocksToRestore.pollFirst();
                    // Check if there is a block to restore
                    if (protectedBlockLocation != null)
                        if (protectedBlockLocation.restoreBlockState())
                            successfulRestores++;
                        else
                            failedRestores++;
                    else
                        // Cancel, let's wait until there is
                        break;
                }

                if (successfulRestores > 0 || failedRestores > 0)
                    LobsterCraft.logger.info(Util.appendStrings("Took us ", Util.formatDecimal((double) (System.nanoTime() - start) / (double) TimeUnit.MILLISECONDS.toNanos(1)),
                            "ms to restore ", successfulRestores + failedRestores, " blocks (failed on ", failedRestores, ", succeeded on ", successfulRestores, ")."));
            }
        }
    }

    protected class ProtectedBlockLocation extends BlockLocation {

        private ProtectionType currentType, originalType;
        private Integer originalId, currentId;

        private BlockState blockState = null;

//        // This is used just for FallingBlocks, can we discard blockState?
//        private ProtectedBlockLocation(@NotNull final ProtectedBlockLocation copy) {
//            super(copy.getChunkLocation(), copy.getRelativeX(), copy.getY(), copy.getRelativeZ());
//            this.currentType = copy.currentType;
//            this.originalType = copy.originalType;
//            this.originalId = copy.originalId;
//            this.currentId = copy.currentId;
//            this.blockState = copy.blockState;
//        }

        /*
         * Used for database.
         * This WON'T insert the block on storage map
         */
        public ProtectedBlockLocation(@NotNull ProtectionType databaseProtectionType, @NotNull Integer databaseCurrentId, @NotNull ChunkLocation chunkLocation, byte x, short y, byte z) {
            super(chunkLocation, x, y, z);
            this.originalType = databaseProtectionType;
            this.originalId = databaseCurrentId;
        }

        /*
         * Used for new blocks
         */
        public ProtectedBlockLocation(@NotNull Location location) {
            super(location);
            this.originalType = null;
            this.originalId = null;

            this.currentType = null;
            this.currentId = null;
        }

        public ProtectedBlockLocation setOwner(@Nullable ProtectionType protectionType, @Nullable Integer id) {
            if ((protectionType != null && id == null) || (protectionType == null && id != null))
                throw new IllegalArgumentException("Can't have a combination null/not-null.");
            this.currentId = id;
            // Exchange block map if current type isn't null and future type isn't null too
            if (id != null && protectionType != this.currentType) {
                // If block was protected before, it was on storage and should be removed:
                // Since the new not null protection type will replace the old one, remove the old
                if (this.currentType != null)
                    switch (this.currentType) {
                        case ADMIN_PROTECTION:
                            adminProtectedBlocks.get(getChunkLocation()).remove(this);
                            break;
                        case CITY_HOUSES_PROTECTION:
                            cityHousesProtectedBlocks.get(getChunkLocation()).remove(this);
                            break;
                        case PLAYER_PROTECTION:
                            playerProtectedBlocks.get(getChunkLocation()).remove(this);
                            break;
                    }
                // Insert on storage (check if containsKey before, we don't want to create a new ConcurrentHashMap every time
                switch (protectionType) {
                    case ADMIN_PROTECTION:
                        if (!adminProtectedBlocks.containsKey(getChunkLocation()))
                            adminProtectedBlocks.put(getChunkLocation(), new ConcurrentHashMap<>());
                        adminProtectedBlocks.get(getChunkLocation()).put(this, this);
                        break;
                    case CITY_HOUSES_PROTECTION:
                        if (!cityHousesProtectedBlocks.containsKey(getChunkLocation()))
                            cityHousesProtectedBlocks.put(getChunkLocation(), new ConcurrentHashMap<>());
                        cityHousesProtectedBlocks.get(getChunkLocation()).put(this, this);
                        break;
                    case PLAYER_PROTECTION:
                        if (!playerProtectedBlocks.containsKey(getChunkLocation()))
                            playerProtectedBlocks.put(getChunkLocation(), new ConcurrentHashMap<>());
                        playerProtectedBlocks.get(getChunkLocation()).put(this, this);
                        break;
                }
            }
            this.currentType = id == null ? null : protectionType;
            return this;
        }

        /**
         * This method should be called on any kind of "BlockPlaceEvent" when a block is placed with a low amount of protected blocks around it
         *
         * @return this block instance
         */
        public ProtectedBlockLocation captureBlockState() {
            blockState = toBukkitLocation().getBlock().getState();
            return this;
        }

        /**
         * This method should be called if the block has enough protected blocks around it.
         *
         * @return this block instance
         */
        public ProtectedBlockLocation setAsPermanentBlock() {
            blockState = null;
            return this;
        }

        /**
         * @return true if block was placed alone or without enough protected blocks around
         */
        public boolean isTemporaryBlock() {
            return blockState != null;
        }

        /**
         * This method should be called before the chunk is unloaded if the block doesn't have enough protected blocks around it.
         *
         * @return true if block was successfully restored
         */
        public boolean restoreBlockState() {
            return blockState != null && blockState.update(true, true);
        }

        public DatabaseState getDatabaseState() {
            // If nothing changed and it isn't null => it is still on database
            if (currentId != null && Objects.equals(this.originalId, currentId) && originalType == currentType)
                return DatabaseState.ON_DATABASE;

            // If wasn't on database on the first place
            if (originalId == null)
                if (currentId == null)
                    // and still doesn't have an owner => do not insert
                    return DatabaseState.NOT_ON_DATABASE;
                else
                    // wasn't on database and now has an owner => insert
                    return DatabaseState.INSERT_TO_DATABASE;

            // Since original wasn't null, checks if current is null (delete) or not null (should update)
            return currentId == null ? DatabaseState.DELETE_FROM_DATABASE : DatabaseState.UPDATE_DATABASE;
        }

        public Integer getCurrentId() {
            return currentId;
        }

        public ProtectionType getCurrentType() {
            return currentType;
        }

        public boolean hasOwner() {
            return currentId != null;
        }

        // This method will be replaced by direct writes since those variables are on the same scope
//        public final void setOnDatabase() {
//            this.originalType = currentType;
//            this.originalId = currentId;
//        }
    }

}
