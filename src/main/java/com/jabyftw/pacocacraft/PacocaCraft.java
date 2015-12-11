package com.jabyftw.pacocacraft;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.jabyftw.pacocacraft.block.block_protection.BlockProtectionService;
import com.jabyftw.pacocacraft.block.xray_protection.XrayProtectionService;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.configuration.ConfigurationFile;
import com.jabyftw.pacocacraft.location.TeleportService;
import com.jabyftw.pacocacraft.login.UserLoginService;
import com.jabyftw.pacocacraft.login.ban.BanService;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.player.PlayerService;
import com.jabyftw.pacocacraft.player.chat.ChatService;
import com.jabyftw.pacocacraft.player.invisibility.InvisibilityService;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.sun.istack.internal.NotNull;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Copyright (C) 2015  Rafael Sartori for PacocaCraft Plugin
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
public class PacocaCraft extends JavaPlugin {

    // Player list
    public static ConcurrentHashMap<Player, PlayerHandler> playerMap = new ConcurrentHashMap<>(); // public as it is used on CommandExecutor and PlayerHandler

    // Services
    public static BlockProtectionService blockProtectionService;
    public static UserLoginService userLoginService;
    public static PlayerService playerService;
    public static TeleportService teleportService;
    public static InvisibilityService invisibilityService;
    public static ChatService chatService;
    public static BanService banService;
    public static XrayProtectionService xrayProtectionService;

    // Util
    public static PacocaCraft pacocaCraft;
    public static Logger logger;
    public static ConfigurationFile config;

    // Vault
    public static Chat chat;
    public static Permission permission;

    // MySQL (HikariCP is thread safe - I read on stackoverflow)
    public static HikariDataSource dataSource;

    // ProtocolLib
    public static ProtocolManager protocolManager;

    // Variables
    private BukkitTask tickTimingTask;
    private static volatile long currentTick = 1; // after some reading, this should be fine now
    private static final Object currentTickLock = new Object();

    private static volatile boolean serverClosing; // with this, I don't need to worry about scheduling tasks to save stuff every time
    private static final Object serverClosingLock = new Object();

    @Override
    public void onEnable() {
        pacocaCraft = this;
        logger = getLogger(); // Use plugin's logger as it has our prefix <3

        // Set server as not closing (affects custom BukkitScheduler)
        synchronized(serverClosingLock) {
            serverClosing = false;
        }

        // Start ticking
        tickTimingTask = BukkitScheduler.runTaskTimer(this, () -> {
            synchronized(currentTickLock) {
                currentTick++; // not atomic, need to synchronize
            }
        }, 0, 1);

        // Start configuration
        try {
            config = new ConfigurationFile(this, "config");
        } catch(IOException | InvalidConfigurationException e) {
            logger.warning("Failed to load configuration! Using default values.");
        }

        // Check up for Vault
        if(getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.severe("Failed to start Vault! Go get it before!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check up for ProtocolLib
        if(getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            logger.severe("Failed to start ProtocolLib! Go get it before!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Get Permission and Chat instances from Vault
        RegisteredServiceProvider<Permission> permissionServiceProvider = getServer().getServicesManager().getRegistration(Permission.class);
        RegisteredServiceProvider<Chat> chatServiceProvider = getServer().getServicesManager().getRegistration(Chat.class);

        if((permission = permissionServiceProvider.getProvider()) == null)
            logger.warning("Failed to start Vault's permission service!");
        if((chat = chatServiceProvider.getProvider()) == null)
            logger.warning("Failed to start Vault's chat service!");

        // Setup ProtocolLib
        protocolManager = ProtocolLibrary.getProtocolManager();

        // Setup MySQL's Data Source
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSourceClassName("org.mariadb.jdbc.MySQLDataSource");
        hikariConfig.setJdbcUrl(PacocaCraft.config.getString(ConfigValue.MYSQL_JDBC_URL.getPath()));
        hikariConfig.setUsername(PacocaCraft.config.getString(ConfigValue.MYSQL_USERNAME.getPath()));
        hikariConfig.setPassword(PacocaCraft.config.getString(ConfigValue.MYSQL_PASSWORD.getPath()));
        hikariConfig.setMaximumPoolSize(PacocaCraft.config.getInt(ConfigValue.MYSQL_POOL_SIZE.getPath())); // (Core count * 2) + spindle (?)
        hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(3)); // 3 seconds before throwing exceptions
        hikariConfig.setMaxLifetime(TimeUnit.SECONDS.toMillis(PacocaCraft.config.getInt(ConfigValue.MYSQL_CONNECTION_TIMEOUT.getPath()) - 60)); // one minute before MariaDB's idle timeout

        try {
            // Connect to database, close if it doesn't
            dataSource = new HikariDataSource(hikariConfig);
        } catch(HikariPool.PoolInitializationException e) {
            logger.severe("Failed to start MySQL: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // TODO Setup and update MySQL tables (will do this after developing - will move server to my Ubuntu partition)

        // Register and start services
        (blockProtectionService = new BlockProtectionService()).onEnable();
        (userLoginService = new UserLoginService()).onEnable();
        (playerService = new PlayerService()).onEnable();
        (teleportService = new TeleportService()).onEnable();
        (invisibilityService = new InvisibilityService()).onEnable();
        (chatService = new ChatService()).onEnable();
        (banService = new BanService()).onEnable();
        (xrayProtectionService = new XrayProtectionService()).onEnable();

        // Announce we're ready
        logger.info(getDescription().getName() + " v" + getDescription().getVersion() + " is enabled!");
    }

    @Override
    public void onDisable() {
        // Set server as closing to custom BukkitScheduler (as it'll run tasks on demand and therefore causing IllegalStateExceptions)
        synchronized(serverClosingLock) {
            serverClosing = true;
        }

        // Kick players
        for(Player player : getServer().getOnlinePlayers()) {
            player.kickPlayer("§cServidor está sendo fechado!\n§cTente entrar mais tarde...");
        }

        // Shutdown services
        if(blockProtectionService != null) blockProtectionService.onDisable();
        if(userLoginService != null) userLoginService.onDisable();
        if(playerService != null) playerService.onDisable();
        if(teleportService != null) teleportService.onDisable();
        if(invisibilityService != null) invisibilityService.onDisable();
        if(chatService != null) chatService.onDisable();
        if(banService != null) banService.onDisable();
        if(xrayProtectionService != null) xrayProtectionService.onDisable();

        // Shutdown MySQL
        if(dataSource != null && !dataSource.isClosed())
            dataSource.close();

        // Save configuration
        if(config != null)
            try {
                config.saveFile();
            } catch(IOException e) {
                e.printStackTrace();
                logger.warning("Failed to save configuration file!");
            }

        // Stop ticking
        tickTimingTask.cancel();

        // Un-register listeners
        getServer().getPluginManager().disablePlugin(this);

        logger.info(getDescription().getName() + " is disabled!");
    }

    /**
     * Get PlayerHandler instance given Bukkit's Player instance
     *
     * @param player Bukkit's player to be searched
     *
     * @return correspondent PlayerHandler or null if none found
     */
    public static PlayerHandler getPlayerHandler(@NotNull Player player) {
        return playerMap.get(player);
    }

    /**
     * @return current tick from server
     */
    public static long getCurrentTick() {
        synchronized(currentTickLock) {
            return currentTick;
        }
    }

    /**
     * @return true if server started closing
     */
    public static boolean isServerClosing() {
        synchronized(serverClosingLock) {
            return serverClosing;
        }
    }
}
