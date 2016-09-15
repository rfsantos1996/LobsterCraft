package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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
public class TeleportBuilder {

    // Both in ticks
    private final static long
            TIME_TO_TELEPORT_TICKS = LobsterCraft.configuration.getLong(ConfigurationValues.PLAYER_TIME_WAITING_BEFORE_TELEPORTING_TICKS.toString()),
            TIME_BETWEEN_ACCEPT_TRIGGERS = LobsterCraft.configuration.getLong(ConfigurationValues.PLAYER_TELEPORT_TIME_BETWEEN_ACCEPT_TRIGGERS_TICKS.toString());

    private final Teleport teleport;

    private TeleportBuilder(@NotNull OnlinePlayer teleporting) {
        this.teleport = new Teleport(teleporting);
    }

    /**
     * @param teleporting teleporting player
     * @return a TeleportBuilder instance with every value set to false
     */
    public static TeleportBuilder getBuilder(@NotNull OnlinePlayer teleporting) {
        return new TeleportBuilder(teleporting);
    }

    /**
     * Execute teleport, delayed if player doesn't have permission or if not forced
     */
    public void execute() {
        teleport.execute();
    }

    public TeleportBuilder setPlayerLocation(@NotNull OnlinePlayer targetPlayer) {
        teleport.targetPlayer = targetPlayer;
        return this;
    }

    public TeleportBuilder setLocation(@NotNull Location targetLocation) {
        teleport.targetLocation = targetLocation;
        return this;
    }

    public TeleportBuilder overrideRegisterLastLocation(boolean overridden_registerLastLocation) {
        teleport.overridden_registerLastLocation = overridden_registerLastLocation;
        return this;
    }

    public TeleportBuilder timeWaitingForTeleport(long amountOfTicks) {
        teleport.timeWaitingInTicks = amountOfTicks;
        return this;
    }

    public TeleportBuilder setInstantaneousTeleport(boolean instantaneous) {
        teleport.forceInstantaneous = instantaneous;
        return this;
    }

    /**
     * Warn player about the teleport conditions (not move, place blocks, dealing damage etc)
     *
     * @param warnTeleportingBefore if the player should be warned or not
     * @return the builder itself
     */
    public TeleportBuilder warnTeleportingPlayer(boolean warnTeleportingBefore) {
        teleport.warnTeleportingBefore = warnTeleportingBefore;
        return this;
    }

    /**
     * Warn target player (the player that will give the location) about someone teleporting to its location
     *
     * @param warnTargetBefore if the player should be warned or not
     * @return the builder itself
     */
    public TeleportBuilder warnTargetPlayer(boolean warnTargetBefore) {
        teleport.warnTargetBefore = warnTargetBefore;
        return this;
    }

    /**
     * Wait some time before listener triggers events (so the player can properly stop moving after typing the command)
     *
     * @param waitBeforeListener if the listener should be delayed
     * @return the builder itself
     */
    public TeleportBuilder waitBeforeListenerTriggers(boolean waitBeforeListener) {
        teleport.waitBeforeListener = waitBeforeListener;
        return this;
    }

    /**
     * Teleport class: it'll be used to store settings and apply on demand
     * If instantaneous teleport, it won't be stored; otherwise, it'll be stored at TeleportListener where it can be cancelled
     */
    public class Teleport implements Runnable {

        protected final OnlinePlayer teleportingPlayerHandler;
        protected final LocationProfile teleportingProfile;
        protected final Player teleportingPlayer;
        protected final boolean instantaneous;

        // Location: Bukkit's Location or a player
        protected OnlinePlayer targetPlayer = null;
        protected Location targetLocation = null;

        // Simple variables
        protected boolean overridden_registerLastLocation = false;
        protected boolean warnTeleportingBefore = false; // Warn "do not move" to player
        protected boolean warnTargetBefore = false; // Warn "player is moving to your location" to target
        protected boolean forceInstantaneous = false;
        protected boolean waitBeforeListener = false; // Wait to insert player on listener
        protected long timeWaitingInTicks = -1;

        protected BukkitTask bukkitTask;

        private Teleport(@NotNull OnlinePlayer teleportingPlayerHandler) {
            this.teleportingPlayerHandler = teleportingPlayerHandler;
            this.teleportingProfile = teleportingPlayerHandler.getProfile(LocationProfile.class);
            this.teleportingPlayer = teleportingPlayerHandler.getPlayer();
            // Check if player has permission to do a instantaneous teleport
            instantaneous = LobsterCraft.permission.has(teleportingPlayerHandler.getPlayer(), Permissions.LOCATION_TELEPORT_INSTANTANEOUSLY.toString());
        }

        @Override
        public void run() {
            // Register last location if player has permission or if explicitly asked not to
            if ((LobsterCraft.permission.has(teleportingPlayer, Permissions.LOCATION_TELEPORT_SAVE_LAST_LOCATION.toString()) || overridden_registerLastLocation)
                    && !overridden_registerLastLocation)
                teleportingProfile.setLastLocation(teleportingPlayer.getLocation());

            // Teleport player (Note: it is checked if location is set, so one of them MUST work)
            teleportingPlayer.teleport(targetLocation != null ? targetLocation : targetPlayer.getPlayer().getLocation());

            // Even isn't instantaneous, remove from waiting teleport
            if (!isInstantaneous())
                teleportingPlayerHandler.pendingTeleport = null;
        }

        protected boolean isInstantaneous() {
            return teleport.instantaneous || teleport.forceInstantaneous;
        }

        protected void execute() {
            // Check if there is a target
            if (teleport.targetLocation == null && teleport.targetPlayer == null)
                throw new IllegalArgumentException("You didn't set up the target location!");

            // Check if TeleportProfile is ready in case of usage
            if (teleport.teleportingProfile == null && teleport.overridden_registerLastLocation)
                throw new IllegalArgumentException("TeleportProfile is null with the intention to register last location!");

            // Warn teleporting player not to move if not instantaneous
            if (warnTeleportingBefore && !isInstantaneous())
                teleportingPlayerHandler.getPlayer().sendMessage("§4NÃO SE MOVA! §cO transporte irá começar...");

            // Warn target player that player is teleporting to you if teleporting player doesn't have quietly permission
            if (warnTargetBefore && targetPlayer != null && targetPlayer.getPlayer().isOnline() &&
                    !LobsterCraft.permission.has(teleportingPlayer, Permissions.LOCATION_TELEPORT_QUIETLY.toString()))
                targetPlayer.getPlayer().sendMessage(Util.appendStrings(teleportingPlayer.getDisplayName(), "§c está transportando até você."));

            // Execute teleport task
            if (teleport.isInstantaneous())
                teleport.run();
            else
                bukkitTask = Bukkit.getScheduler().runTaskLater(
                        LobsterCraft.plugin,
                        teleport,
                        (teleport.timeWaitingInTicks >= 0 ? teleport.timeWaitingInTicks : TIME_TO_TELEPORT_TICKS) + // Waiting ticks + time between listener trigger
                                // and teleport
                                (teleport.waitBeforeListener ? TIME_BETWEEN_ACCEPT_TRIGGERS : 0L)
                );

            // Add player on listener if it isn't instantaneous (this should be here because of bukkitTask variable)
            if (!isInstantaneous())
                Bukkit.getScheduler().runTaskLater(
                        LobsterCraft.plugin,
                        () -> teleportingPlayerHandler.pendingTeleport = this,
                        waitBeforeListener ? TIME_BETWEEN_ACCEPT_TRIGGERS : 0L
                );
        }

        public void cancel() {
            // Cancel if task is already scheduled
            if (bukkitTask != null) {
                bukkitTask.cancel();
                // If it is cancelled, remove from Listener checks
                teleportingPlayerHandler.pendingTeleport = null;
            }
        }
    }
}
