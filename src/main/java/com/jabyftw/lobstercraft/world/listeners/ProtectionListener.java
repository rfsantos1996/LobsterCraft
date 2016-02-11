package com.jabyftw.lobstercraft.world.listeners;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.custom_events.EntityDamageEntityEvent;
import com.jabyftw.lobstercraft.player.custom_events.PlayerDamageEntityEvent;
import com.jabyftw.lobstercraft.player.util.ConditionController;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.Pair;
import com.jabyftw.lobstercraft.world.util.ProtectionType;
import com.jabyftw.lobstercraft.world.util.location_util.BlockLocation;
import com.jabyftw.lobstercraft.world.util.location_util.ProtectedBlockLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
public class ProtectionListener implements Listener {

    private final HashMap<Integer, Pair<ProtectedBlockLocation, Long>> fallingEntities = new HashMap<>();
    private final Material TOOL_HAND_MATERIAL = com.jabyftw.easiercommands.Util.parseToMaterial(LobsterCraft.config.getString(ConfigValue.WORLD_PROTECTION_TOOL_HAND_MATERIAL.toString()));

    public ProtectionListener() {
        if (TOOL_HAND_MATERIAL == null)
            throw new IllegalStateException("Tool hand material from configuration is null!");

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
        // Check if chunks aren't loaded
        if (!LobsterCraft.blockController.loadNearChunks(location)) {
            // Warn player
            playerHandler.getConditionController().sendMessageIfConditionReady(ConditionController.Condition.PROTECTION_BEING_LOADED, "§cProteção está sendo carregada...");
            return false;
        }

        // Ignore administrator build mode players
        if (playerHandler.getProtectionType() != ProtectionType.ADMIN_PROTECTION)
            if (LobsterCraft.blockController.checkForOtherPlayersAndAdministratorBlocks(location, playerHandler.getPlayerId())) {
                // Warn player, return false
                playerHandler.getConditionController().sendMessageIfConditionReady(
                        ConditionController.Condition.PROTECTION_NEAR_BLOCKS,
                        "§cExistem blocos protegidos por outra pessoa por perto."
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

        // Iterate through all blocks
        for (BlockState blockState : blockStates)
            // Insert block
            LobsterCraft.blockController
                    .addBlock(blockState.getLocation(), playerHandler.getProtectionType(), playerHandler.getBuildModeId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getLocation().getWorld()))
            return;

        if (!(event.getEntity() instanceof Monster))
            return;

        if (!LobsterCraft.blockController.loadNearChunks(event.getLocation())) {
            event.setCancelled(true);
            return;
        }

        if (LobsterCraft.blockController.checkForAdministratorBlocks(event.getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getLocation().getWorld()))
            return;

        if (!LobsterCraft.blockController.loadNearChunks(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }

        ProtectedBlockLocation blockLocation = LobsterCraft.blockController.getBlock(event.getBlock().getLocation());

        // Don't burn protected blocks );
        if (blockLocation != null && !blockLocation.isUndefined())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityPrimeExplosion(ExplosionPrimeEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getEntity().getLocation().getWorld()))
            return;

        // Do not even prime when protection isn't loaded
        if (!LobsterCraft.blockController.loadNearChunks(event.getEntity().getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getLocation().getWorld()))
            return;

        // Do not explode blocks if protection isn't loaded
        if (!LobsterCraft.blockController.loadNearChunks(event.getLocation())) {
            event.setCancelled(true);
            return;
        }

        // Check for near administrators blocks
        if (LobsterCraft.blockController.checkForAdministratorBlocks(event.getLocation())) {
            event.setCancelled(true);
            return;
        }

        Iterator<Block> iterator = event.blockList().iterator();

        // Iterate through all blocks
        while (iterator.hasNext()) {
            Block next = iterator.next();

            BlockLocation blockLocation = new BlockLocation(next.getLocation());

            // It isn't Suspicious since BlockLocation has the same hashCode() as ProtectedBlockLocation
            //noinspection SuspiciousMethodCalls
            if (LobsterCraft.blockController.getBlocksForChunk(blockLocation.getChunkLocation()).contains(blockLocation))
                // List is mutable, remove block from damage
                iterator.remove();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDoorBreak(EntityBreakDoorEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        if (!LobsterCraft.blockController.loadNearChunks(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }

        ProtectedBlockLocation blockLocation = LobsterCraft.blockController.getBlock(event.getBlock().getLocation());

        // Do not allow any door to be broken
        if (blockLocation != null && !blockLocation.isUndefined())
            event.setCancelled(true);
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlockHighest(EntityChangeBlockEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // If world isn't loaded, return
        if (!LobsterCraft.blockController.loadNearChunks(event.getBlock().getLocation())) {
            // Cancel if we handle these entities when block is protected
            if (event.getEntityType() == EntityType.SHEEP || event.getEntityType() == EntityType.ENDERMAN || event.getEntityType() == EntityType.FALLING_BLOCK)
                event.setCancelled(true);
            return;
        }

        // Check for sheep or enderman
        if (event.getEntity().getType() == EntityType.SHEEP || event.getEntity().getType() == EntityType.ENDERMAN) {
            ProtectedBlockLocation blockLocation = LobsterCraft.blockController.getBlock(event.getBlock().getLocation());

            // Cancel any protected block from being broken/stolen
            if (blockLocation != null && !blockLocation.isUndefined())
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlockMonitor(EntityChangeBlockEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Check for falling block
        FallingBlock fallingBlock;
        if (event.getEntity().getType() == EntityType.FALLING_BLOCK && (fallingBlock = (FallingBlock) event.getEntity()).getMaterial().hasGravity()) {
            Pair<ProtectedBlockLocation, Long> protectedLocation;

            // Check if the block started to fall
            if (!fallingEntities.containsKey(fallingBlock.getEntityId())) {
                ProtectedBlockLocation protectedBlockLocation = LobsterCraft.blockController.getBlock(event.getBlock().getLocation());

                // Check if block is protected
                if (protectedBlockLocation != null && !protectedBlockLocation.isUndefined()) {
                    fallingEntities.put(
                            event.getEntity().getEntityId(),
                            new Pair<>(protectedBlockLocation, System.currentTimeMillis())
                    );
                    protectedBlockLocation.setUndefinedOwner();
                }

                // Check if falling block was protected before
            } else if ((protectedLocation = fallingEntities.remove(fallingBlock.getEntityId())) != null) {
                ProtectedBlockLocation oldBlock = protectedLocation.getA();

                // Do not have to check for undefined owner as it is filtered on insertion
                LobsterCraft.blockController
                        .addBlock(event.getBlock().getLocation(), oldBlock.getType(), oldBlock.getCurrentId());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onArmorStandDamageByEntity(EntityDamageEntityEvent event) {
        if (event.getDamaged().getType() == EntityType.ARMOR_STAND) {
            // Check if world is loaded
            if (!LobsterCraft.blockController.loadNearChunks(event.getDamaged().getLocation())) {
                event.setCancelled(true);
                return;
            }

            // Ignore players
            if (event.getDamager() instanceof Player) return;

            // Check for near protection
            if (LobsterCraft.blockController.checkForOtherPlayersAndAdministratorBlocks(event.getDamaged().getLocation(), null))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onArmorStandDamageByPlayer(PlayerDamageEntityEvent event) {
        if (event.getDamaged().getType() == EntityType.ARMOR_STAND) {
            // Check if world is loaded
            if (!LobsterCraft.blockController.loadNearChunks(event.getDamaged().getLocation())) {
                event.setCancelled(true);
                return;
            }

            // Check for near protection
            if (!checkForNearBlocks(LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayerDamager()), event.getDamaged().getLocation()))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer());

        // Check if player protection was loaded
        if (!checkForNearBlocks(playerHandler, event.getRightClicked().getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteractHighest(PlayerInteractEvent event) {
        PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler(event.getPlayer());

        // Needed, we do not ignore cancelled events
        if (playerHandler != null) {
            // Check if player protection was loaded
            if (!checkForNearBlocks(playerHandler, event.getPlayer().getLocation())) {
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
                event.setCancelled(true);
                return;
            }

            if (event.getClickedBlock() != null) {
                ProtectedBlockLocation protectedBlockLocation = LobsterCraft.blockController.getBlock(event.getClickedBlock().getLocation());

                // Check it even if checkForNearBlocks return true? This could be dangerous, so yep
                // If block is protected and the build mode isn't equal the player mode => cancel
                if (protectedBlockLocation != null && (protectedBlockLocation.getCurrentId() != playerHandler.getBuildModeId() || protectedBlockLocation.getType() != playerHandler.getProtectionType())) {
                    event.setUseInteractedBlock(Event.Result.DENY);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToolInteractHighest(PlayerInteractEvent event) {
        // Conditions to trigger desired effect (clicked a block, material in hand is specific)
        if (event.getClickedBlock() != null && event.getMaterial() == TOOL_HAND_MATERIAL) {
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
            if (playerHandler.getConditionController().sendMessageIfNotReady(
                    ConditionController.Condition.PROTECTION_CHECK, "§cAguarde! Não podemos conferir proteção muito constantemente."
            )) return;

            // Check if player protection was loaded
            if (!LobsterCraft.blockController.loadNearChunks(bukkitBlockLocation)) {
                playerHandler.getConditionController().sendMessageIfConditionReady(ConditionController.Condition.PROTECTION_BEING_LOADED, "§cProteção está sendo carregada...");
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

    private String getProtectedBlockMessage(ProtectedBlockLocation blockLocation) {
        StringBuilder stringBuilder = new StringBuilder("§6Protegido ");
        boolean blockDefined = !blockLocation.isUndefined();

        // Append information
        if (blockDefined) {
            // "protegido temporariamente para" ou "protegido para"
            if (blockLocation.isTemporaryBlock()) stringBuilder.append("temporariamente ");
            stringBuilder.append("para ");

            if (blockLocation.getType() == ProtectionType.ADMIN_PROTECTION)
                stringBuilder.append("administrador §c").append(LobsterCraft.constructionsService.getConstructionName(blockLocation.getCurrentId()));
            else if (blockLocation.getType() == ProtectionType.PLAYER_PROTECTION)
                stringBuilder.append("jogador §c#").append(blockLocation.getCurrentId());

        } else {
            stringBuilder.append("para §7(desconhecido)");
        }

        return stringBuilder.toString();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeavesDecayHighest(LeavesDecayEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getLocation().getWorld()))
            return;

        if (!LobsterCraft.blockController.loadNearChunks(event.getBlock().getLocation()))
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
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getLocation().getWorld()))
            return;

        if (!LobsterCraft.blockController.loadNearChunks(event.getLocation()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrowMonitor(StructureGrowEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getLocation().getWorld()))
            return;

        ProtectedBlockLocation structureBase = LobsterCraft.blockController.getBlock(event.getLocation());

        // Return if base isn't protected
        if (structureBase == null) return;

        if (!structureBase.isUndefined())
            // Itreate through all blocks
            for (BlockState blockState : event.getBlocks())
                LobsterCraft.blockController.addBlock(blockState.getLocation(), structureBase.getType(), structureBase.getCurrentId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFormHighest(BlockFormEvent event) {
        // Bring attention, since I want to see what is this
        for (int i = 0; i < 5; i++)
            LobsterCraft.logger.info("blockMaterial: " + event.getBlock().getType().name() +
                    " formedMaterial: " + event.getNewState().getType().name());
        // UNKNOWN => probably: ice spreading
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getBlock().getWorld()))
            return;

        // Cancel if it isn't loaded
        if (!LobsterCraft.blockController.loadNearChunks(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }

        // Cancel if protected blocks are close
        if (LobsterCraft.blockController.checkForOtherPlayersAndAdministratorBlocks(event.getBlock().getLocation(), null)) {
            event.setCancelled(true);
            return;
        }

        ProtectedBlockLocation blockLocation = LobsterCraft.blockController.getBlock(event.getBlock().getLocation());

        // If block is protected, cancel event
        if (blockLocation != null && !blockLocation.isUndefined())
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpreadHighest(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.GRASS || event.getSource().getType() == Material.FIRE || event.isCancelled())
            return;

        if (!LobsterCraft.worldService.isWorldIgnored(event.getSource().getWorld()) && !LobsterCraft.blockController.loadNearChunks(event.getBlock().getLocation()))
            event.setCancelled(true);
        // things spreading: grass, vine, fire, mushrooms
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpreadMonitor(BlockSpreadEvent event) {
        // Check if world is ignored
        if (event.getSource().getType() == Material.GRASS || event.getSource().getType() == Material.FIRE || event.isCancelled() || LobsterCraft.worldService.isWorldIgnored(event.getSource().getWorld()))
            return;

        ProtectedBlockLocation sourceProtection = LobsterCraft.blockController.getBlock(event.getSource().getLocation());

        // Return if source isn't protected
        if (sourceProtection == null) return;

        // Iterate through all blocks
        if (!sourceProtection.isUndefined())
            LobsterCraft.blockController.addBlock(event.getBlock().getLocation(), sourceProtection.getType(), sourceProtection.getCurrentId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtendHighest(BlockPistonExtendEvent event) {
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
    public void onPistonExtendMonitor(BlockPistonExtendEvent event) {
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
    public void onPistonRetractHighest(BlockPistonRetractEvent event) {
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
    public void onPistonRetractMonitor(BlockPistonRetractEvent event) {
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
