package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.player.Profile.ProfileType;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.BuildingMode;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

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
public class OnlinePlayer {

    // Both are now in ticks
    private final static long
            LOGIN_MESSAGE_PERIOD_BETWEEN_MESSAGES = LobsterCraft.configuration.getLong(ConfigurationValues.LOGIN_MESSAGE_PERIOD_BETWEEN_MESSAGES_TICKS.toString()),
            LOGIN_MESSAGE_TIME_TO_LOGIN = LobsterCraft.configuration.getLong(ConfigurationValues.LOGIN_MESSAGE_TIME_TO_LOGIN_SECONDS.toString()) * 20L;

    // Essential constants
    private final Player player;
    private final OfflinePlayer offlinePlayer;
    protected final ConcurrentHashMap<ProfileType, Profile> profiles = new ConcurrentHashMap<>();

    /*
     * Variables set on PlayerHandlerService#onPlayerJoinLowest on the constructor and used on login
     */
    protected volatile OnlineState onlineState;
    private BukkitTask loginMessageTask;
    private PlayerState preLoginState;
    private long preLoginTick;

    /*
     * Variables set during runtime
     */
    private long loginTime = -1;
    private PlayerState survivalGameModeState = null;
    private boolean godMode = false;
    protected TeleportBuilder.Teleport pendingTeleport = null;
    private TriggerController triggerController;
    protected BuildingMode buildingMode = null;
    protected final ArrayDeque<XrayListener.OreBlockLocation> oreLocationHistory = new ArrayDeque<>();

    /**
     * Construct a online player
     *
     * @param offlinePlayer offline player for player's name
     * @param player        Bukkit's player instance
     * @see OnlinePlayer#loginPlayer() this will modify player's attributes setted on the constructor
     */
    OnlinePlayer(@NotNull OfflinePlayer offlinePlayer, @NotNull Player player) {
        // Check if player name matches
        if (!player.getName().equalsIgnoreCase(offlinePlayer.getPlayerName()))
            throw new IllegalArgumentException(Util.appendStrings("Player isn't owner of this PlayerHandler (", offlinePlayer.getPlayerName(), ")"));

        this.offlinePlayer = offlinePlayer;
        this.player = player;
        this.onlineState = offlinePlayer.isRegistered() ? OnlineState.PRE_LOGIN : OnlineState.PRE_REGISTER;
        this.triggerController = new TriggerController(player);

        // Update display name
        player.setDisplayName(ChatColor.translateAlternateColorCodes(
                '&', Util.appendStrings(
                        // Add prefix
                        LobsterCraft.chat.getPlayerPrefix(player),
                        // Add name
                        player.getName(),
                        // Add suffix
                        LobsterCraft.chat.getPlayerSuffix(player),
                        // Add needed reset sign
                        ChatColor.RESET
                )));

        /*
         * Set player on pre-login state
         */

        // Make player invisible before logging in
        setInvisible(true);

        // Set player game mode to SURVIVAL
        setGameMode(GameMode.SURVIVAL);
        // Store player state and reset player
        preLoginState = capturePlayerState().resetPlayer();

        // Set pre-login variables => they will be restored through preLoginState
        player.setCanPickupItems(false);
        player.setAllowFlight(true);
        player.setFlying(true);

        preLoginTick = LobsterCraft.tickCounter.getTick();
        // Set login message task, will be removed on PlayerHandlerService#loginPlayer with other attributes
        loginMessageTask = Bukkit.getScheduler().runTaskTimer(LobsterCraft.plugin, () -> {
            // Check if player is online
            if (!player.isOnline()) {
                loginMessageTask.cancel();
                return;
            }

            String commandUsage = Util.appendStrings("§6Use o comando §c", offlinePlayer.isRegistered() ? "/login (senha)" : "/register (senha) (senha)");

            if (LobsterCraft.tickCounter.getTick() - preLoginTick > LOGIN_MESSAGE_TIME_TO_LOGIN)
                // Kick the player because he took to long
                player.kickPlayer(Util.appendStrings("§4Você não conectou ao servidor.\n", commandUsage));
            else
                // Send message if time isn't exceeded
                player.sendMessage(commandUsage);
        }, 5L, LOGIN_MESSAGE_PERIOD_BETWEEN_MESSAGES);
    }

    /**
     * Attempt login player
     *
     * @param encryptedPassword player's encrypted password
     * @return a LoginResponse to send the CommandSender ("LOGIN_WENT_ASYNCHRONOUS_SUCCESSFULLY" is a success)
     * @see OnlinePlayer#loginPlayer() to force login
     * @see Util#encryptString(String) for password encrypting
     */
    public LoginResponse attemptLoginPlayer(@NotNull final String encryptedPassword) {
        // Check if passwords match
        if (!offlinePlayer.getEncryptedPassword().equals(encryptedPassword))
            return LoginResponse.PASSWORD_DO_NOT_MATCH;

        return loginPlayer();
    }

    /**
     * Register player
     *
     * @param encryptedPassword player's encrypted password
     * @return a LoginResponse to send the CommandSender ("LOGIN_WENT_ASYNCHRONOUS_SUCCESSFULLY" is a success)
     * @see OnlinePlayer#loginPlayer() to force login
     * @see Util#encryptString(String) for password encrypting
     */
    public LoginResponse attemptRegisterPlayer(@NotNull final String encryptedPassword) {
        return LobsterCraft.servicesManager.playerHandlerService.registerPlayer(this, encryptedPassword);
    }

    /**
     * Attempt login player, will go asynchronous when fetching profiles and back to synchronous when applying
     *
     * @return a LoginResponse to send the CommandSender ("LOGIN_WENT_ASYNCHRONOUS_SUCCESSFULLY" is a success)
     */
    LoginResponse loginPlayer() {
        // Check if player was registered
        if (!offlinePlayer.isRegistered())
            return LoginResponse.NOT_REGISTERED;

        // Check if player is already logged in
        if (onlineState == OnlinePlayer.OnlineState.LOGGED_IN)
            return LoginResponse.ALREADY_LOGGED_IN;

        Bukkit.getScheduler().runTaskAsynchronously(
                LobsterCraft.plugin,
                () -> {
                    // Cancel if player isn't online anymore
                    if (!player.isOnline())
                        return;

                    // Fetch all profiles asynchronously
                    FutureTask<Set<Profile>> retrieveProfiles = LobsterCraft.servicesManager.playerHandlerService.retrieveProfiles(this);
                    retrieveProfiles.run();

                    // Remove login message task
                    if (loginMessageTask != null) {
                        loginMessageTask.cancel();
                        loginMessageTask = null;
                    }

                    // Go back synchronous
                    Bukkit.getScheduler().runTask(
                            LobsterCraft.plugin,
                            () -> {
                                // Cancel if player isn't online anymore
                                if (!player.isOnline())
                                    return;

                                try {
                                    // Restore player pre-login state before applying profiles (they might want to check stuff)
                                    if (preLoginState != null) {
                                        preLoginState.restorePlayerState();
                                        preLoginState = null;
                                    }

                                    // Apply all profiles (they can now use synchronous methods and can change player's state)
                                    for (Profile profile : retrieveProfiles.get())
                                        // Will cause NullPointerException if there is a error on profile retrieving which will kick the player as needed
                                        profile.applyProfile(this);

                                    // Update login time and player's last IP
                                    offlinePlayer.lastIp = player.getAddress().getAddress().getHostAddress();
                                    offlinePlayer.databaseState = DatabaseState.UPDATE_DATABASE;
                                    loginTime = System.currentTimeMillis();

                                    // Set as logged in after profiles are applied
                                    onlineState = OnlinePlayer.OnlineState.LOGGED_IN;

                                    // Set player's visibility condition
                                    // Check if player have the permission to join quietly/invisible
                                    if (LobsterCraft.permission.has(player, Permissions.JOIN_VANISHED.toString())) {
                                        setInvisible(true);
                                    } else {
                                        // Broadcast player login because he isn't vanished
                                        setInvisible(false);
                                        Bukkit.broadcastMessage(Util.appendStrings("§b+ §3", player.getName()));
                                    }
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                    player.kickPlayer("§4Ocorreu um erro durante login.");
                                }
                            });
                });

        return LoginResponse.LOGIN_WENT_ASYNCHRONOUS_SUCCESSFULLY;
    }

    /**
     * This method is used on player quit, preparing to save everything
     *
     * @see PlayerHandlerService#onPlayerQuitMonitor(PlayerQuitEvent)
     */
    protected void logOff() {
        // Restore player moment if it exists
        if (preLoginState != null) {
            preLoginState.restorePlayerState();
            preLoginState = null;
        }

        // Remove login message task if it exists
        if (loginMessageTask != null) {
            loginMessageTask.cancel();
            loginMessageTask = null;
        }

        // Note: teleport task is cancelled on TeleportListener

        if (isLoggedIn()) {
            // Restore player game mode
            setGameMode(GameMode.SURVIVAL);

            // Save offline player's attributes
            offlinePlayer.lastTimeOnline = System.currentTimeMillis();
            offlinePlayer.timePlayed += Math.abs(System.currentTimeMillis() - loginTime);
            offlinePlayer.databaseState = DatabaseState.UPDATE_DATABASE;

            // Set as not logged in
            this.onlineState = OnlineState.PRE_LOGIN;
        }
    }

    /**
     * Change player's name checking password (used in commands)
     *
     * @param newPlayerName     player's new name
     * @param encryptedPassword player's current password
     * @return true if successfully changed player name
     */
    public PlayerHandlerService.ChangeNameResponse changePlayerName(@NotNull final String newPlayerName, @NotNull final String encryptedPassword) {
        return offlinePlayer.getEncryptedPassword().equals(encryptedPassword) ?
                LobsterCraft.servicesManager.playerHandlerService.changePlayerName(offlinePlayer, newPlayerName)
                : PlayerHandlerService.ChangeNameResponse.INCORRECT_PASSWORD;
    }

    /**
     * Kick or ban player. This won't announce to the server and will keep a record on MySQL. This method <b>SHOULD</b> run asynchronously.
     *
     * @param banType        kick type
     * @param reason         reason to be at record, from 4 to 120 characters
     * @param moderatorId    moderator to be stored, can be null
     * @param bannedDuration ban duration, can be null
     * @return a ban response to the CommandSender
     */
    public PlayerHandlerService.BanResponse kickPlayer(@NotNull final PlayerHandlerService.BanType banType, @NotNull final String reason, @Nullable Integer moderatorId,
                                                       @Nullable final Long bannedDuration) {
        return LobsterCraft.servicesManager.playerHandlerService.kickPlayer(offlinePlayer, banType, reason, moderatorId, bannedDuration);
    }

    /**
     * Capture player variables to be restored later. Can be used, for example, when joining and leaving an mini-game.
     *
     * @return a PlayerState instance, capable of restoring the player
     */
    public PlayerState capturePlayerState() {
        return new PlayerState(this);
    }

    /**
     * Change player's game mode. It'll store the SURVIVAL player state and restore on log off
     *
     * @param gameMode player's new game mode
     * @return true if game mode was changed
     */
    public boolean setGameMode(@NotNull final GameMode gameMode) {
        // Ignore if game mode is the same
        if (player.getGameMode() == gameMode) return false;

        // If is changing TO survival game mode, restore
        if (survivalGameModeState != null && gameMode == GameMode.SURVIVAL) {
            player.getInventory().clear();
            survivalGameModeState.restorePlayerState();
            survivalGameModeState = null;
        }

        // Store current player state if LEAVING survival game mode
        if (player.getGameMode() == GameMode.SURVIVAL)
            survivalGameModeState = capturePlayerState().resetPlayer();

        // Apply Bukkit's setGameMode(GameMode)
        player.setGameMode(gameMode);
        return true;
    }

    /**
     * Safe player is a player that can't kill or be killed on PVP, drop items or exp in any way. This is built for events and for moderators or any player in creative
     * mode.
     *
     * @return true if this player is a "Safe Player"
     */
    public boolean isSafePlayer() {
        return LobsterCraft.permission.has(player, Permissions.PLAYER_SAFE_PLAYER.toString()) || player.getGameMode() != GameMode.SURVIVAL;
    }

    public boolean isInvisible() {
        return LobsterCraft.vanishManager.isVanished(player);
    }

    /**
     * Change player's visual state
     *
     * @param invisible true if player needs to BECOME invisible
     */
    public void setInvisible(boolean invisible) {
        if ((isInvisible() && !invisible) || (!isInvisible() && invisible))
            LobsterCraft.vanishManager.toggleVanishQuiet(player, false);
    }

    /*
     * Getters
     */

    /**
     * @return player's Bukkit player instance
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return player's OfflinePlayer instance
     */
    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    /**
     * @return current player's login state
     */
    public OnlineState getOnlineState() {
        return onlineState;
    }

    public boolean isLoggedIn() {
        return onlineState == OnlineState.LOGGED_IN;
    }

    public boolean isDamageable() {
        return isLoggedIn() && !isInvisible() && !isGodMode(); //&& !isSafePlayer(); // We should allow PVP but not dropping items
    }

    public TriggerController getTriggerController() {
        return triggerController;
    }

    public BuildingMode getBuildingMode() {
        return buildingMode;
    }

    public boolean isGodMode() {
        return godMode;
    }

    /**
     * Update player's god mode
     *
     * @param godMode true if player should be invincible
     * @return godMode
     */
    public boolean setGodMode(boolean godMode) {
        // Credits: VanishNoPacket
        // If is going to be on god mode, remove entities' agro
        if (godMode)
            // Iterate through all entities
            for (Entity entity : player.getNearbyEntities(48 / 2, 48 / 2, 48 / 2)) {
                LivingEntity target;
                // If entity have a target and it is the same player, remove target
                if (entity instanceof Creature && (target = ((Creature) entity).getTarget()) instanceof Player && target.equals(player))
                    ((Creature) entity).setTarget(null);
            }
        this.godMode = godMode;
        return godMode;
    }

    /**
     * Retrieve player's profile
     *
     * @param profileClass profile class to search for
     * @param <T>          the class should extend Profile
     * @return null if no profile with that class was found
     */
    @SuppressWarnings("unchecked")
    public <T extends Profile> T getProfile(@NotNull Class<T> profileClass) {
        ProfileType profileType = ProfileType.getProfileType(profileClass);
        // Check if no profile type was found
        if (profileType == null) return null;
        synchronized (profiles) {
            return (T) profiles.get(profileType);
        }
    }

    /**
     * This will check if the "obj" has/is the same OfflinePlayer
     *
     * @param obj object to be compared
     * @return true if object is a equal OfflinePlayer or is a OnlinePlayer with equal OfflinePlayer variable
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return offlinePlayer.equals(obj);
    }

    @Override
    public int hashCode() {
        return offlinePlayer.hashCode();
    }

    /*
     * Some classes
     */

    public class PlayerState {

        private final OnlinePlayer onlinePlayer;
        private final Player player;

        // Player variables
        private final Location playerLocation;
        private final Collection<PotionEffect> activePotionEffects;
        private final ItemStack[] armorContents, contents;
        private final boolean allowFlight, flying, canPickupItems, healthScaled;
        private final int totalExperience, foodLevel, remainingAir, maximumAir, level, fireTicks, noDamageTicks;
        private final float exhaustion, saturation, fallDistance, exp;
        private final double health, healthScale, maxHealth;

        private PlayerState(@NotNull OnlinePlayer onlinePlayer) {
            this.onlinePlayer = onlinePlayer;
            this.player = onlinePlayer.getPlayer();

            // Store stuff
            // Location
            playerLocation = player.getLocation().clone();
            // Potion effects
            activePotionEffects = player.getActivePotionEffects();
            // Inventory
            armorContents = player.getInventory().getArmorContents();
            contents = player.getInventory().getContents();
            // booleans
            allowFlight = player.getAllowFlight();
            flying = player.isFlying();
            canPickupItems = player.getCanPickupItems();
            healthScaled = player.isHealthScaled();
            // integers
            totalExperience = player.getTotalExperience();
            foodLevel = player.getFoodLevel();
            remainingAir = player.getRemainingAir();
            maximumAir = player.getMaximumAir();
            level = player.getLevel();
            fireTicks = player.getFireTicks();
            noDamageTicks = player.getNoDamageTicks();
            // floats
            exhaustion = player.getExhaustion();
            saturation = player.getSaturation();
            fallDistance = player.getFallDistance();
            exp = player.getExp();
            // double
            health = player.getHealth();
            healthScale = player.getHealthScale();
            maxHealth = player.getMaxHealth();
        }

        /**
         * Restore captured player state
         */
        public void restorePlayerState() {
            // Teleport player
            TeleportBuilder.getBuilder(onlinePlayer)
                    .overrideRegisterLastLocation(false)
                    .setLocation(playerLocation)
                    .setInstantaneousTeleport(true)
                    .execute();
            // Restore potion effects
            player.addPotionEffects(activePotionEffects);
            // Restore inventory
            player.getInventory().setArmorContents(armorContents);
            player.getInventory().setContents(contents);
            // Restore booleans
            player.setAllowFlight(allowFlight);
            player.setFlying(flying);
            player.setCanPickupItems(canPickupItems);
            player.setHealthScaled(healthScaled);
            // Restore integers
            player.setTotalExperience(totalExperience);
            player.setFoodLevel(foodLevel);
            player.setRemainingAir(remainingAir);
            player.setMaximumAir(maximumAir);
            player.setLevel(level);
            player.setFireTicks(fireTicks);
            player.setNoDamageTicks(noDamageTicks);
            // Restore floats
            player.setExhaustion(exhaustion);
            player.setSaturation(saturation);
            player.setFallDistance(fallDistance);
            player.setExp(exp);
            // Restore doubles
            player.setHealth(health);
            player.setHealthScale(healthScale);
            player.setMaxHealth(maxHealth);
        }

        /**
         * Clear player
         *
         * @return this PlayerState instance
         */
        public PlayerState resetPlayer() {
            // Restore some variables
            player.setFoodLevel(20);
            player.setSaturation(0);
            player.setExhaustion(0);
            player.setTotalExperience(0);
            player.setLevel(0);
            player.setExp(0);
            player.setFallDistance(0);
            // Restore health and air
            player.setHealthScaled(false);
            player.setMaxHealth(20);
            player.setHealth(player.getMaxHealth());
            player.setMaximumAir(10);
            player.setRemainingAir(player.getMaximumAir());
            // Remove potion effects
            for (PotionEffect potionEffect : player.getActivePotionEffects())
                player.removePotionEffect(potionEffect.getType());
            // Clear inventory
            player.getInventory().clear();

            return this;
        }
    }

    public enum LoginResponse {
        LOGIN_WENT_ASYNCHRONOUS_SUCCESSFULLY,
        PASSWORD_DO_NOT_MATCH,
        NOT_REGISTERED,
        ALREADY_REGISTERED,
        ALREADY_LOGGED_IN
    }

    @SuppressWarnings("WeakerAccess")
    public enum OnlineState {
        LOGGED_IN,
        PRE_LOGIN,
        PRE_REGISTER
    }
}
