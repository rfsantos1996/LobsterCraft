package com.jabyftw.lobstercraft.world;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.Location;

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
    private final HashSet<BlockProtectionType> blockProtectionTypesCheck = new HashSet<>();
    private final HashSet<Integer>
            ignoredPlayers = new HashSet<>(),
            ignoredConstructions = new HashSet<>(),
            ignoredCityHouses = new HashSet<>(),
            ignoredCities = new HashSet<>();
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
        this.blockProtectionTypesCheck.add(BlockProtectionType.ADMINISTRATOR_BLOCKS);
        return this;
    }

    public ProtectionChecker excludeConstruction(int constructionId) {
        this.ignoredConstructions.add(constructionId);
        return this;
    }

    public ProtectionChecker checkPlayerBlocksNearThisBlock(boolean loadExtraChunks) {
        this.loadExtraChunks = loadExtraChunks;
        this.blockProtectionTypesCheck.add(BlockProtectionType.PLAYER_BLOCKS);
        return this;
    }

    public ProtectionChecker excludePlayer(int playerId) {
        this.ignoredPlayers.add(playerId);
        return this;
    }

    public ProtectionChecker checkCityHousesBlocksNearThisBlock() {
        this.blockProtectionTypesCheck.add(BlockProtectionType.CITY_HOUSES);
        return this;
    }

    public ProtectionChecker checkCityBlocksNearThisBlock() {
        this.blockProtectionTypesCheck.add(BlockProtectionType.CITY_BLOCKS);
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

    public ProtectionChecker excludeCity(int cityId) {
        this.ignoredCities.add(cityId);
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
                if (!ignoredCities.contains(cityStructure.getCityId()) &&
                        BlockProtectionType.CITY_BLOCKS.protectionDistanceSquared(blockLocation, cityStructure.getCenterLocation()) <=
                                BlockProtectionType.CITY_BLOCKS.getMinimumDistanceBetweenCities())
                    return ProtectionCheckerResponse.CITY_NEAR_BLOCK;

        // Check if we need to load near chunks
        if (!LobsterCraft.servicesManager.worldService.loadNearChunks(blockLocation.getChunkLocation(), loadExtraChunks))
            return ProtectionCheckerResponse.REGION_NEAR_BLOCK_NOT_LOADED;

        // Check current block if we're checking near blocks
        // needed? I think so: placing, interacting and breaking a block will require this
        if (!blockProtectionTypesCheck.isEmpty())
            this.checkThisBlock = true;

        if (checkThisBlock) {
            WorldService.ProtectedBlockLocation protectedBlock = LobsterCraft.servicesManager.worldService.getBlock(blockLocation);
            // Check if block exists and is protected (should we check protection type?)
            if (protectedBlock != null && protectedBlock.hasOwner() && checkIfBlockIsProtected(protectedBlock))
                return ProtectionCheckerResponse.CURRENT_BLOCK_IS_PROTECTED;
        }

        if (!blockProtectionTypesCheck.isEmpty()) {
            int loadSize = -1;
            // Calculate the safest search size
            for (BlockProtectionType searchType : blockProtectionTypesCheck)
                loadSize = Math.max(Util.getMinimumChunkRange(searchType.getProtectionDistance()), loadSize);

            final Holder holder = new Holder();
            if (loadSize > 0)
                blockLocation.getChunkLocation().getNearChunks(loadSize).parallelStream().forEach(chunkLocation -> {
                    // Iterate through all blocks
                    for (WorldService.ProtectedBlockLocation protectedBlock : LobsterCraft.servicesManager.worldService.protectedBlocks.get(chunkLocation).values()) {
                        if (holder.hasResponse()) return;

                        // If block is protected AND...
                        if (protectedBlock.hasOwner() && blockProtectionTypesCheck.contains(protectedBlock.getCurrentType()) &&
                                // ...the distance between this blocks are less or equal the protection distance AND...
                                protectedBlock.getCurrentType().protectionDistanceSquared(blockLocation, protectedBlock) <= protectedBlock.getCurrentType().getProtectionDistanceSquared() &&
                                // ... the block is protected
                                checkIfBlockIsProtected(protectedBlock)) {
                            holder.setResponse(protectedBlock.getCurrentType());
                            return;
                        }
                    }
                });
            // If there is a response
            if (holder.hasResponse())
                return holder.getResponse();
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
                blockProtectionTypesCheck.remove(BlockProtectionType.PLAYER_BLOCKS);
            else
                ignoredPlayers.add(playerId);

            // Ignore administrator blocks
            if (LobsterCraft.permission.has(onlinePlayer.getPlayer(), Permissions.WORLD_PROTECTION_IGNORE_ADMIN_BLOCKS.toString()))
                blockProtectionTypesCheck.remove(BlockProtectionType.ADMINISTRATOR_BLOCKS);

            // Get player's city
            Integer cityId = onlinePlayer.getOfflinePlayer().getCityId();
            if (cityId != null) {
                // Ignore near cities
                if (LobsterCraft.permission.has(onlinePlayer.getPlayer(), Permissions.WORLD_PROTECTION_IGNORE_CITY_STRUCTURES.toString())) {
                    this.checkNearCities = false;
                    blockProtectionTypesCheck.remove(BlockProtectionType.CITY_BLOCKS);
                } else {
                    ignoredCities.add(cityId);
                }

                // Ignore city houses
                if (LobsterCraft.permission.has(onlinePlayer.getPlayer(), Permissions.WORLD_PROTECTION_IGNORE_CITY_HOUSES_BLOCKS.toString())) {
                    blockProtectionTypesCheck.remove(BlockProtectionType.CITY_HOUSES);
                } else {
                    // Check if player has a house
                    CityStructure.CityHouse cityHouse = onlinePlayer.getOfflinePlayer().getCity().getHouseFromPlayer(playerId);
                    if (cityHouse != null)
                        ignoredCityHouses.add(cityHouse.getHouseId());
                }
            }

            // Ignore player's build mode
            BuildingMode buildingMode;
            if ((buildingMode = onlinePlayer.getBuildingMode()) != null && (blockProtectionTypesCheck.contains(buildingMode.getBlockProtectionType()) || checkThisBlock))
                switch (buildingMode.getBlockProtectionType()) {
                    case PLAYER_BLOCKS:
                        ignoredPlayers.add(buildingMode.getProtectionId());
                        break;
                    case ADMINISTRATOR_BLOCKS:
                        ignoredConstructions.add(buildingMode.getProtectionId());
                        break;
                    case CITY_BLOCKS:
                        ignoredCities.add(buildingMode.getProtectionId());
                        break;
                    case CITY_HOUSES:
                        ignoredCityHouses.add(buildingMode.getProtectionId());
                        break;
                }
        }
    }

    private boolean checkIfBlockIsProtected(@NotNull final WorldService.ProtectedBlockLocation protectedBlock) {
        // Check if the block shouldn't be ignored
        return (protectedBlock.getCurrentType() == BlockProtectionType.ADMINISTRATOR_BLOCKS && !ignoredConstructions.contains(protectedBlock.getCurrentId())) ||
                (protectedBlock.getCurrentType() == BlockProtectionType.CITY_HOUSES && !ignoredCityHouses.contains(protectedBlock.getCurrentId())) ||
                (protectedBlock.getCurrentType() == BlockProtectionType.CITY_BLOCKS && !ignoredCities.contains(protectedBlock.getCurrentId())) ||
                (protectedBlock.getCurrentType() == BlockProtectionType.PLAYER_BLOCKS && !ignoredPlayers.contains(protectedBlock.getCurrentId()));
    }

    public enum ProtectionCheckerResponse {

        BLOCK_IS_SAFE("§aBloco seguro para ser modificado!"),

        ADMINISTRATOR_BLOCK_NEAR("§cBloco protegido por administradores por perto..."),
        CITY_HOUSE_BLOCK_NEAR("§cBloco protegido pela casa por perto..."),
        CITY_BLOCK_NEAR("§cBloco protegido pela cidade por perto..."),
        PLAYER_BLOCK_NEAR("§cBloco protegido por jogadores por perto..."),

        CURRENT_BLOCK_IS_PROTECTED("§6Esse bloco está protegido!"),
        CITY_NEAR_BLOCK("§cHá outras cidades por perto..."),
        REGION_NEAR_BLOCK_NOT_LOADED("§7Carregando blocos..."),
        PLAYER_NOT_LOGGED_IN("§cVocê não entrou no servidor!");

        private final String playerMessage;

        ProtectionCheckerResponse(String playerMessage) {
            this.playerMessage = playerMessage;
        }

        public String getPlayerMessage() {
            return playerMessage;
        }
    }

    private class Holder {

        private ProtectionCheckerResponse response = null;

        protected synchronized boolean hasResponse() {
            return response != null;
        }

        protected synchronized ProtectionCheckerResponse getResponse() {
            return response;
        }

        protected synchronized void setResponse(@NotNull final BlockProtectionType protectionType) {
            if (hasResponse()) return;
            if (protectionType == BlockProtectionType.ADMINISTRATOR_BLOCKS)
                this.response = ProtectionCheckerResponse.ADMINISTRATOR_BLOCK_NEAR;
            else if (protectionType == BlockProtectionType.CITY_HOUSES)
                this.response = ProtectionCheckerResponse.CITY_HOUSE_BLOCK_NEAR;
            else if (protectionType == BlockProtectionType.CITY_BLOCKS)
                this.response = ProtectionCheckerResponse.CITY_BLOCK_NEAR;
            else if (protectionType == BlockProtectionType.PLAYER_BLOCKS)
                this.response = ProtectionCheckerResponse.PLAYER_BLOCK_NEAR;
            else
                throw new IllegalStateException(Util.appendStrings("Protection type unknown for protection check response: ", protectionType));
        }
    }
}
