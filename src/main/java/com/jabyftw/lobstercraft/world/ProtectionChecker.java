package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.Location;

import java.util.Collection;
import java.util.HashSet;

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
class ProtectionChecker {

    private final BlockLocation blockLocation;

    // Checker variables
    private final HashSet<ProtectionType> blockProtectionTypesCheck = new HashSet<>();
    private final HashSet<Short> ignoredNearCities = new HashSet<>();
    private final HashSet<Integer>
            ignoredPlayers = new HashSet<>(),
            ignoredConstructions = new HashSet<>(),
            ignoredCityHouses = new HashSet<>();
    private OnlinePlayer onlinePlayer;
    private boolean
            loadExtraChunks = false,
            checkNearCities = false,
            checkThisBlock = true;

    private ProtectionChecker(@NotNull final BlockLocation blockLocation) {
        this.blockLocation = blockLocation;
    }

    public static ProtectionChecker init(@NotNull final BlockLocation blockLocation) {
        return new ProtectionChecker(blockLocation);
    }

    public static ProtectionChecker init(@NotNull final Location bukkitLocation) {
        return new ProtectionChecker(new BlockLocation(bukkitLocation));
    }

    /*
     * Class options
     */

    // This will retrieve all information from the player
    public ProtectionChecker automatizePlayer(@Nullable final OnlinePlayer onlinePlayer) {
        this.onlinePlayer = onlinePlayer;
        return this;
    }

    public ProtectionChecker checkThisBlock(boolean checkThisBlock, boolean loadExtraChunks) {
        this.checkThisBlock = checkThisBlock;
        this.loadExtraChunks = loadExtraChunks;
        return this;
    }

    public ProtectionChecker checkAdministratorBlocksNearThisBlock() {
        this.blockProtectionTypesCheck.add(ProtectionType.ADMIN_PROTECTION);
        return this;
    }

    public ProtectionChecker excludeConstruction(int constructionId) {
        this.ignoredConstructions.add(constructionId);
        return this;
    }

    public ProtectionChecker checkPlayerBlocksNearThisBlock(boolean loadExtraChunks) {
        this.loadExtraChunks = loadExtraChunks;
        this.blockProtectionTypesCheck.add(ProtectionType.PLAYER_PROTECTION);
        return this;
    }

    public ProtectionChecker excludePlayer(int playerId) {
        this.ignoredPlayers.add(playerId);
        return this;
    }

    public ProtectionChecker checkCityHousesBlocksNearThisBlock() {
        this.blockProtectionTypesCheck.add(ProtectionType.CITY_HOUSES_PROTECTION);
        return this;
    }

    public ProtectionChecker excludeHouse(int houseId) {
        this.ignoredCityHouses.add(houseId);
        return this;
    }

    public ProtectionChecker checkCitiesNearThisBlock(boolean checkNearCities) {
        this.checkNearCities = checkNearCities;
        return this;
    }

    public ProtectionChecker excludeCity(short cityId) {
        this.ignoredNearCities.add(cityId);
        return this;
    }

    /*
     * Executor
     */

    public ProtectionCheckerResponse getResponse() {
        if (onlinePlayer != null) {
            // Check if player is logged in
            if (!onlinePlayer.isLoggedIn()) return ProtectionCheckerResponse.PLAYER_NOT_LOGGED_IN;

            // Re-check player
            reallyAutomatizePlayer();
        }

        if (checkNearCities)
            for (CityStructure cityStructure : LobsterCraft.servicesManager.cityService.getCityStructures())
                // Check if the block is near a stranger city
                if (!ignoredNearCities.contains(cityStructure.getCityId()) &&
                        cityStructure.getCenterLocation().distanceSquared(blockLocation) <= ProtectionType.CITY_HOUSES_PROTECTION.getSearchDistanceSquared())
                    return ProtectionCheckerResponse.CITY_NEAR_BLOCK;

        // Check if we need to load near chunks
        if ((blockProtectionTypesCheck.contains(ProtectionType.PLAYER_PROTECTION) || checkThisBlock) &&
                !LobsterCraft.servicesManager.worldService.loadNearChunks(blockLocation.getChunkLocation(), loadExtraChunks))
            return ProtectionCheckerResponse.REGION_NEAR_BLOCK_NOT_LOADED;

        // Check current block if we're checking near blocks
        // needed? I think so: placing, interacting and breaking a block will require this
        if (!blockProtectionTypesCheck.isEmpty())
            this.checkThisBlock = true;

        if (checkThisBlock) {
            WorldService.ProtectedBlockLocation protectedBlock = LobsterCraft.servicesManager.worldService.getBlock(blockLocation);
            // Check if block exists and is protected
            if (protectedBlock != null && protectedBlock.hasOwner() && checkIfBlockIsProtected(protectedBlock))
                return ProtectionCheckerResponse.CURRENT_BLOCK_IS_PROTECTED;
        }

        if (!blockProtectionTypesCheck.isEmpty()) {
            int loadSize = -1;
            // Calculate the safest search size
            for (ProtectionType searchType : blockProtectionTypesCheck)
                loadSize = Math.max(Util.getMinimumChunkRange(searchType.getSearchDistance()), loadSize);

            if (loadSize > 0 && blockLocation.getChunkLocation().getNearChunks(loadSize).parallelStream().anyMatch(chunkLocation -> {
                // Iterate using the priority order (administrator > city houses > player blocks) - it'll iterate using the number of blocks
                for (ProtectionType searchType : ProtectionType.getPriorityOrder()) {
                    // Check if search type is not required
                    if (!blockProtectionTypesCheck.contains(searchType)) continue;
                    Collection<WorldService.ProtectedBlockLocation> protectedBlockLocations = null;

                    // Check if we should skip this chunk for this searchType
                    if (!blockLocation.getChunkLocation().shouldBeIgnored(chunkLocation, searchType.getSearchDistance()))
                        switch (searchType) {
                            case PLAYER_PROTECTION:
                                // Won't throw NullPointerException because we ignore far away chunks that doesn't need to be loaded
                                protectedBlockLocations = LobsterCraft.servicesManager.worldService.playerProtectedBlocks.get(chunkLocation).values();
                                break;
                            case ADMIN_PROTECTION:
                                protectedBlockLocations = LobsterCraft.servicesManager.worldService.adminProtectedBlocks.get(chunkLocation).values();
                                break;
                            case CITY_HOUSES_PROTECTION:
                                protectedBlockLocations = LobsterCraft.servicesManager.worldService.cityHousesProtectedBlocks.get(chunkLocation).values();
                                break;
                        }

                    // If we didn't skip, lets check all the blocks until we find one
                    if (protectedBlockLocations != null)
                        for (WorldService.ProtectedBlockLocation protectedBlock : protectedBlockLocations)
                            // If block is protected AND...
                            if (protectedBlock.hasOwner() &&
                                    // ...the distance between this blocks are less or equal the protection distance AND...
                                    protectedBlock.distanceSquared(blockLocation) <= searchType.getSearchDistanceSquared() &&
                                    // ... the block is protected
                                    checkIfBlockIsProtected(protectedBlock))
                                return true;
                }
                // Return will close this stream
                return true;
            }))
                return ProtectionCheckerResponse.PROTECTED_REGION_NEAR_BLOCK;
        }

        // No protection close, block is safe to be broken
        return ProtectionCheckerResponse.BLOCK_IS_SAFE;
    }

    /*
     * Internal methods
     */

    private void reallyAutomatizePlayer() {
        // Get player id
        Integer playerId = onlinePlayer.getOfflinePlayer().getPlayerId();
        if (playerId != null) {
            // Ignore player blocks
            if (LobsterCraft.permission.has(onlinePlayer.getPlayer(), Permissions.WORLD_PROTECTION_IGNORE_PLAYER_BLOCKS.toString()))
                blockProtectionTypesCheck.remove(ProtectionType.PLAYER_PROTECTION);
            else
                ignoredPlayers.add(playerId);

            // Ignore administrator blocks
            if (LobsterCraft.permission.has(onlinePlayer.getPlayer(), Permissions.WORLD_PROTECTION_IGNORE_ADMIN_BLOCKS.toString()))
                blockProtectionTypesCheck.remove(ProtectionType.ADMIN_PROTECTION);

            // Get player's city
            Short cityId = onlinePlayer.getOfflinePlayer().getCityId();
            if (cityId != null) {
                // Ignore near cities
                if (LobsterCraft.permission.has(onlinePlayer.getPlayer(), Permissions.WORLD_PROTECTION_IGNORE_CITY_STRUCTURES.toString()))
                    this.checkNearCities = false;
                else
                    ignoredNearCities.add(cityId);

                // Ignore city houses
                if (LobsterCraft.permission.has(onlinePlayer.getPlayer(), Permissions.WORLD_PROTECTION_IGNORE_CITY_HOUSES_BLOCKS.toString())) {
                    blockProtectionTypesCheck.remove(ProtectionType.CITY_HOUSES_PROTECTION);
                } else {
                    // Check if player has a house
                    CityStructure.CityHouse cityHouse = LobsterCraft.servicesManager.cityService.getCity(cityId).getHouseFromPlayer(playerId);
                    if (cityHouse != null)
                        ignoredCityHouses.add(cityHouse.getHouseId());
                }
            }
        }
    }

    private boolean checkIfBlockIsProtected(@NotNull final WorldService.ProtectedBlockLocation protectedBlock) {
        // Check if the block shouldn't be ignored
        return blockProtectionTypesCheck.contains(protectedBlock.getCurrentType()) &&
                (protectedBlock.getCurrentType() == ProtectionType.ADMIN_PROTECTION && !ignoredConstructions.contains(protectedBlock.getCurrentId())) ||
                (protectedBlock.getCurrentType() == ProtectionType.CITY_HOUSES_PROTECTION && !ignoredCityHouses.contains(protectedBlock.getCurrentId())) ||
                (protectedBlock.getCurrentType() == ProtectionType.PLAYER_PROTECTION && !ignoredPlayers.contains(protectedBlock.getCurrentId()));
    }

    public enum ProtectionCheckerResponse {
        BLOCK_IS_SAFE,
        CURRENT_BLOCK_IS_PROTECTED,
        CITY_NEAR_BLOCK,
        PROTECTED_REGION_NEAR_BLOCK,
        REGION_NEAR_BLOCK_NOT_LOADED,
        PLAYER_NOT_LOGGED_IN
    }
}
