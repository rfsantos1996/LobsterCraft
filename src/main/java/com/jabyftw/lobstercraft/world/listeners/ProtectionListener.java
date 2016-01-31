package com.jabyftw.lobstercraft.world.listeners;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.AdministratorBuildMode;
import com.jabyftw.lobstercraft.player.util.ConditionController;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.Pair;
import com.jabyftw.lobstercraft.world.util.ProtectionType;
import com.jabyftw.lobstercraft.world.util.location_util.AdministratorBlockLocation;
import com.jabyftw.lobstercraft.world.util.location_util.BlockLocation;
import com.jabyftw.lobstercraft.world.util.location_util.PlayerBlockLocation;
import com.jabyftw.lobstercraft.world.util.location_util.ProtectedBlockLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
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
public class ProtectionListener implements Listener {

    private final HashMap<Integer, Pair<ProtectedBlockLocation, Long>> fallingEntities = new HashMap<>();

    public ProtectionListener() {
        BukkitScheduler.runTaskTimer(() -> {
            Iterator<Pair<ProtectedBlockLocation, Long>> iterator = fallingEntities.values().iterator();
            long currentTimeMillis = System.currentTimeMillis();

            // Iterate through all items
            while (iterator.hasNext()) {
                Pair<ProtectedBlockLocation, Long> next = iterator.next();

                // Check if has been on the map for more than a minute
                if (currentTimeMillis - next.getB() > TimeUnit.SECONDS.toMillis(60))
                    iterator.remove();
            }
        }, 20L * 60, 20L * 60);
    }

    /**
     * Check for conditions that may cause the player to not
     *
     * @param playerHandler player that will receive messages and check for build conditions
     * @param location      Bukkit's block location to search at
     * @return true if the player can build on that place
     */
    private boolean checkForNearBlocks(PlayerHandler playerHandler, Location location) {
        // Get near blocks
        Set<ProtectedBlockLocation> protectedBlocks = LobsterCraft.blockController.getProtectedBlocks(location);

        // Check if chunks aren't loaded
        if (protectedBlocks == null) {
            // Warn player
            playerHandler.getConditionController().sendMessageIfConditionReady(ConditionController.Condition.PROTECTION_BEING_LOADED, "§cProteção está sendo carregada...");
            return false;
        }

        // Ignore administrator build mode players
        if (playerHandler.getProtectionType() != ProtectionType.ADMIN_PROTECTION)
            // Iterate through all blocks
            for (ProtectedBlockLocation protectedBlock : protectedBlocks)
                // Check if player own every block near him or block is on administrator protection
                if (protectedBlock.getType() == ProtectionType.ADMIN_PROTECTION) {
                    // Warn player, return false
                    playerHandler.getConditionController().sendMessageIfConditionReady(
                            ConditionController.Condition.PROTECTION_ADMINISTRATOR_BLOCKS,
                            "§cExistem blocos protegidos por administradores por perto."
                    );
                    return false;
                } else if (protectedBlock.getType() == ProtectionType.PLAYER_PROTECTION && playerHandler.getPlayerId() != ((PlayerBlockLocation) protectedBlock).getOwnerId()) {
                    // Warn player, return false
                    playerHandler.getConditionController().sendMessageIfConditionReady(
                            ConditionController.Condition.PROTECTION_PLAYER_BLOCKS,
                            "§cExistem blocos de outro jogador por perto."
                    );
                    return false;
                }

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakHighest(BlockBreakEvent event) {
        // Check if world is ignored
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Check for near blocks
        if (!checkForNearBlocks(LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer()), event.getBlock().getLocation()))
            event.setCancelled(true);
    }

    // Note that this will ignore checks. All of this MUST (and ARE) be checked on another priority
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakMonitor(BlockBreakEvent event) {
        // Ignore if world is ignored or event is cancelled
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        ProtectedBlockLocation protectedBlockLocation = LobsterCraft.blockController.getBlock(event.getBlock().getLocation());

        // if block exists, remove owner (conditions of "ownage" were already checked and event was cancelled)
        if (protectedBlockLocation != null)
            protectedBlockLocation.setUndefinedOwner();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlaceHighest(BlockPlaceEvent event) {
        // Check if world is ignored
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Check for type of event
        if (event instanceof BlockMultiPlaceEvent)
            // Iterate through all blocks
            for (BlockState blockState : ((BlockMultiPlaceEvent) event).getReplacedBlockStates()) {
                // Check near blocks
                if (!checkForNearBlocks(LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer()), blockState.getLocation()))
                    // If ANY of the blocks goes false, everything will be cancelled
                    event.setCancelled(true);
            }
        else
            // Check for near blocks
            if (!checkForNearBlocks(LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer()), event.getBlock().getLocation()))
                event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaceMonitor(BlockPlaceEvent event) {
        // Check if world is ignored
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer());
        List<BlockState> blockStates = event instanceof BlockMultiPlaceEvent ?
                ((BlockMultiPlaceEvent) event).getReplacedBlockStates()
                : Collections.singletonList(event.getBlock().getState());

        // If player is on administrator build mode
        if (playerHandler.getProtectionType() == ProtectionType.ADMIN_PROTECTION)
            // Iterate through all blocks
            for (BlockState blockState : blockStates)
                // Insert block
                LobsterCraft.blockController
                        .addBlock(blockState.getLocation(), AdministratorBlockLocation.class)
                        .setConstructionId(((AdministratorBuildMode) playerHandler.getBuildMode()).getConstructionId());
        else
            // Iterate through all blocks
            for (BlockState blockState : blockStates)
                // Insert block
                LobsterCraft.blockController
                        .addBlock(blockState.getLocation(), PlayerBlockLocation.class)
                        .setOwnerId(playerHandler.getPlayerId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlockHighest(EntityChangeBlockEvent event) {
        if (event.getEntity().getType() == EntityType.FALLING_BLOCK && !event.isCancelled() && !LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld())
                && LobsterCraft.blockController.getProtectedBlocks(event.getBlock().getLocation()) == null)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlockMonitor(EntityChangeBlockEvent event) {
        // Used when sheeps eat grass, endermen getting blocks
        LobsterCraft.logger.info("Material: " + event.getBlock().getType().name() + " -> " + event.getTo().name() + " entityType: " + event.getEntityType().name());

        FallingBlock fallingBlock;

        // Check if world is ignored, entity is a falling block and event isn't cancelled
        if (event.getEntity().getType() == EntityType.FALLING_BLOCK && !event.isCancelled() && !LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld())
                && (fallingBlock = (FallingBlock) event.getEntity()).getMaterial().hasGravity()) {
            Pair<ProtectedBlockLocation, Long> protectedLocation;

            // Check if the block started to fall
            if (!fallingEntities.containsKey(fallingBlock.getEntityId())) {
                ProtectedBlockLocation protectedBlockLocation = LobsterCraft.blockController.getBlock(event.getBlock().getLocation());

                // Check if block is protected
                if (protectedBlockLocation != null) {
                    fallingEntities.put(
                            event.getEntity().getEntityId(),
                            new Pair<>(protectedBlockLocation, System.currentTimeMillis())
                    );
                    protectedBlockLocation.setUndefinedOwner();
                }

                // Check if falling block was protected before
            } else if ((protectedLocation = fallingEntities.remove(fallingBlock.getEntityId())) != null) {
                if (protectedLocation.getA().getType() == ProtectionType.PLAYER_PROTECTION) {
                    // Get old player block
                    PlayerBlockLocation oldProtectedBlock = (PlayerBlockLocation) protectedLocation.getA();

                    // Register player protected block
                    LobsterCraft.blockController
                            .addBlock(event.getBlock().getLocation(), PlayerBlockLocation.class)
                            .setOwnerId((oldProtectedBlock).getOwnerId());
                } else if (protectedLocation.getA().getType() == ProtectionType.ADMIN_PROTECTION) {
                    // Get old administrator block
                    AdministratorBlockLocation oldProtectedBlock = (AdministratorBlockLocation) protectedLocation.getA();

                    // Register administrator protected block
                    LobsterCraft.blockController
                            .addBlock(event.getBlock().getLocation(), AdministratorBlockLocation.class)
                            .setConstructionId(oldProtectedBlock.getConstructionId());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockInteractHighest(PlayerInteractEvent event) {
        // Conditions to trigger desired effect
        if (event.getClickedBlock() != null && event.getMaterial() == Material.STICK && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer());

            // Check if player is spamming
            if (playerHandler.getConditionController().sendMessageIfNotReady(
                    ConditionController.Condition.PROTECTION_CHECK, "§cAguarde! Não podemos conferir proteção muito constantemente."
            )) return;

            Set<ProtectedBlockLocation> protectedBlocks = LobsterCraft.blockController.getProtectedBlocks(event.getClickedBlock().getLocation());

            // Check if player protection was loaded
            if (protectedBlocks == null) {
                playerHandler.getConditionController().sendMessageIfConditionReady(ConditionController.Condition.PROTECTION_BEING_LOADED, "§cProteção está sendo carregada...");
                return;
            }

            ProtectedBlockLocation blockLocation = LobsterCraft.blockController.getBlock(event.getClickedBlock().getLocation());

            // Check player block
            if (blockLocation != null) {
                // Warn player
                playerHandler.sendMessage(getProtectedBlockMessage(blockLocation));
                event.setCancelled(true);
            } else {
                playerHandler.sendMessage("§cEste bloco não está protegido.");
            }
        }
    }

    private String getProtectedBlockMessage(ProtectedBlockLocation blockLocation) {
        StringBuilder stringBuilder = new StringBuilder("§6Protegido para ");

        // Append information
        if (blockLocation.getType() == ProtectionType.ADMIN_PROTECTION) {
            AdministratorBlockLocation administratorBlockLocation = (AdministratorBlockLocation) blockLocation;

            stringBuilder.append("administrador §c").append(LobsterCraft.constructionsService.getConstructionName(administratorBlockLocation.getConstructionId()));
        } else if (blockLocation.getType() == ProtectionType.PLAYER_PROTECTION) {
            PlayerBlockLocation playerBlockLocation = (PlayerBlockLocation) blockLocation;

            stringBuilder.append("jogador §c#").append(playerBlockLocation.getOwnerId());
        } else {
            stringBuilder.append("§7desconhecido");
        }

        return stringBuilder.toString();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeavesDecayHighest(LeavesDecayEvent event) {
        // Load the stuff
        if (!event.isCancelled() && !LobsterCraft.worldService.isWorldIgnored(event.getBlock().getLocation().getWorld())
                && LobsterCraft.blockController.getProtectedBlocks(event.getBlock().getLocation()) == null)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecayMonitor(LeavesDecayEvent event) {
        // Check if world wasn't ignored
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        ProtectedBlockLocation protectedBlockLocation = LobsterCraft.blockController.getBlock(event.getBlock().getLocation());

        // Check if block exists
        if (protectedBlockLocation != null)
            // Remove protection
            protectedBlockLocation.setUndefinedOwner();

    }

    // As tested, this isn't spammed (worse, it took some minutes to retrigger the event)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrowHighest(StructureGrowEvent event) {
        if (!event.isCancelled() && !LobsterCraft.worldService.isWorldIgnored(event.getLocation().getWorld())
                && LobsterCraft.blockController.getProtectedBlocks(event.getLocation()) == null)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrowMonitor(StructureGrowEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getLocation().getWorld()))
            return;

        ProtectedBlockLocation structureBase = LobsterCraft.blockController.getBlock(event.getLocation());

        // Return if base isn't protected
        if (structureBase == null) return;

        boolean adminStructure = structureBase.getType() == ProtectionType.ADMIN_PROTECTION;
        long currentId = adminStructure ? ((AdministratorBlockLocation) structureBase).getConstructionId() : ((PlayerBlockLocation) structureBase).getOwnerId();

        // Itreate through all blocks
        for (BlockState blockState : event.getBlocks()) {
            if (adminStructure)
                LobsterCraft.blockController.addBlock(blockState.getLocation(), AdministratorBlockLocation.class).setConstructionId(currentId);
            else
                LobsterCraft.blockController.addBlock(blockState.getLocation(), PlayerBlockLocation.class).setOwnerId(currentId);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFormHighest(BlockFormEvent event) {
        // Bring attention, since I want to see what is this
        for (int i = 0; i < 5; i++)
            LobsterCraft.logger.info("blockMaterial: " + event.getBlock().getType().name() +
                    " formedMaterial: " + event.getNewState().getType().name());
        // UNKNOWN => probably: ice spreading
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpreadHighest(BlockSpreadEvent event) {
        if (event.getSource().getType() != Material.GRASS && !event.isCancelled() && !LobsterCraft.worldService.isWorldIgnored(event.getSource().getWorld())
                && LobsterCraft.blockController.getProtectedBlocks(event.getBlock().getLocation()) == null)
            event.setCancelled(true);
        // things spreading: grass, vine
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpreadMonitor(BlockSpreadEvent event) {
        // Check if world is ignored
        if (event.getSource().getType() == Material.GRASS || event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getSource().getWorld()))
            return;

        ProtectedBlockLocation sourceProtection = LobsterCraft.blockController.getBlock(event.getSource().getLocation());

        // Return if source isn't protected
        if (sourceProtection == null) return;

        boolean adminStructure = sourceProtection.getType() == ProtectionType.ADMIN_PROTECTION;
        long currentId = adminStructure ? ((AdministratorBlockLocation) sourceProtection).getConstructionId() : ((PlayerBlockLocation) sourceProtection).getOwnerId();

        // Itreate through all blocks
        if (adminStructure)
            LobsterCraft.blockController.addBlock(event.getBlock().getLocation(), AdministratorBlockLocation.class).setConstructionId(currentId);
        else
            LobsterCraft.blockController.addBlock(event.getBlock().getLocation(), PlayerBlockLocation.class).setOwnerId(currentId);

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtendHighest(BlockPistonExtendEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Iterate through all modified blocks
        for (Block block : event.getBlocks()) {
            BlockLocation blockLocation = new BlockLocation(block.getLocation());

            // Check if worlds need to load protection
            if (LobsterCraft.blockController.getProtectedBlocks(blockLocation) == null)
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtendMonitor(BlockPistonExtendEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Iterate through all blocks
        for (Block block : event.getBlocks()) {
            Block futureBlock = block.getRelative(event.getDirection());

            ProtectedBlockLocation oldProtection = LobsterCraft.blockController.getBlock(block.getLocation());

            if (oldProtection == null) continue;

            boolean adminProtection = oldProtection.getType() == ProtectionType.ADMIN_PROTECTION;
            long currentId = adminProtection ? ((AdministratorBlockLocation) oldProtection).getConstructionId() : ((PlayerBlockLocation) oldProtection).getOwnerId();

            if (adminProtection)
                LobsterCraft.blockController.addBlock(futureBlock.getLocation(), AdministratorBlockLocation.class).setConstructionId(currentId);
            else
                LobsterCraft.blockController.addBlock(futureBlock.getLocation(), PlayerBlockLocation.class).setOwnerId(currentId);
        }

        ProtectedBlockLocation firstBlockProtection = LobsterCraft.blockController.getBlock(event.getBlock().getRelative(event.getDirection()).getLocation());

        // Remove protection from first block
        if (firstBlockProtection != null) firstBlockProtection.setUndefinedOwner();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetractHighest(BlockPistonRetractEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Iterate through all modified blocks
        for (Block block : event.getBlocks()) {
            // Check if worlds need to load protection
            if (LobsterCraft.blockController.getProtectedBlocks(block.getLocation()) == null)
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetractMonitor(BlockPistonRetractEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Iterate through all blocks
        for (Block block : event.getBlocks()) {
            Block futureBlock = block.getRelative(event.getDirection());

            ProtectedBlockLocation oldProtection = LobsterCraft.blockController.getBlock(block.getLocation());

            if (oldProtection == null) continue;

            boolean adminProtection = oldProtection.getType() == ProtectionType.ADMIN_PROTECTION;
            long currentId = adminProtection ? ((AdministratorBlockLocation) oldProtection).getConstructionId() : ((PlayerBlockLocation) oldProtection).getOwnerId();

            if (adminProtection)
                LobsterCraft.blockController.addBlock(futureBlock.getLocation(), AdministratorBlockLocation.class).setConstructionId(currentId);
            else
                LobsterCraft.blockController.addBlock(futureBlock.getLocation(), PlayerBlockLocation.class).setOwnerId(currentId);
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

    /*@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGrowHighest(BlockGrowEvent event) {
        LobsterCraft.logger.info("blockMaterial: " + event.getBlock().getType().name() +
                " growMaterial: " + event.getNewState().getType().name());
        // things growing: wheat, pumpkin (there is no way to search if pumpkin is protected), long_grass
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpreadMonitor(BlockSpreadEvent event) {
        LobsterCraft.logger.info("blockMaterial: " + event.getBlock().getType().name() +
                " spreadMaterial: " + event.getNewState().getType().name() +
                " sourceMaterial: " + event.getSource().getType().name());
        // things spreading: grass, vine
    }*/
}
