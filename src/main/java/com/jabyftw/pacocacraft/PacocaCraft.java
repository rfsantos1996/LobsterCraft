package com.jabyftw.pacocacraft;

import com.jabyftw.pacocacraft.block_protection.BlockProtectionService;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.configuration.ConfigurationFile;
import com.jabyftw.pacocacraft.login.UserLoginService;
import com.jabyftw.pacocacraft.login.ban.BanService;
import com.jabyftw.pacocacraft.player.UserProfile;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
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
    public static ConcurrentHashMap<Player, UserProfile> playerMap = new ConcurrentHashMap<>();

    // Services
    public static BlockProtectionService blockProtectionService;
    public static UserLoginService userLoginService;
    public static BanService banService;

    // Util
    public static PacocaCraft pacocaCraft;
    public static Server server;
    public static Logger logger;
    public static ConfigurationFile config;

    // Vault
    public static Chat chat;
    public static Permission permission;

    // MySQL
    public volatile static HikariDataSource dataSource;

    // Timing
    private BukkitTask tickTimingTask;
    public static volatile long currentTick = 1; // read on async pre-join event -- no need to AtomicLong
    public static final Object tickLock = new Object();

    @Override
    public void onEnable() {
        pacocaCraft = this;
        server = getServer();
        logger = getLogger();

        // Start ticking
        tickTimingTask = server.getScheduler().runTaskTimer(this, () -> {
            synchronized(tickLock) {
                currentTick++; // not instantaneous event, need to synchronize
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

        // Get Permission and Chat instances from Vault
        RegisteredServiceProvider<Permission> permissionServiceProvider = getServer().getServicesManager().getRegistration(Permission.class);
        RegisteredServiceProvider<Chat> chatServiceProvider = getServer().getServicesManager().getRegistration(Chat.class);

        if((permission = permissionServiceProvider.getProvider()) == null)
            logger.warning("Failed to start Vault's permission service!");
        if((chat = chatServiceProvider.getProvider()) == null)
            logger.warning("Failed to start Vault's chat service!");

        // Setup MySQL's Data Source
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSourceClassName("org.mariadb.jdbc.MySQLDataSource");
        //hikariConfig.setDriverClassName("MariaDB"); // May not be needed
        hikariConfig.setJdbcUrl(ConfigValue.MYSQL_JDBC_URL.<String>getValue());
        hikariConfig.setUsername(ConfigValue.MYSQL_USERNAME.<String>getValue());
        hikariConfig.setPassword(ConfigValue.MYSQL_PASSWORD.<String>getValue());
        hikariConfig.setMaximumPoolSize(ConfigValue.MYSQL_POOL_SIZE.<Integer>getValue()); // (Core count * 2) + spindle (?)
        hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(3)); // 3 seconds before throwing exceptions
        hikariConfig.setMaxLifetime(TimeUnit.SECONDS.toMillis(ConfigValue.MYSQL_CONNECTION_TIMEOUT.<Integer>getValue() - 60)); // one minute before MariaDB's idle timeout
        try {
            // Connect to database, close if it doesn't
            dataSource = new HikariDataSource(hikariConfig);
        } catch(HikariPool.PoolInitializationException e) {
            logger.severe("Failed to start MySQL: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Setup and update MySQL tables


        // Register and start services
        (blockProtectionService = new BlockProtectionService()).onEnable();
        (userLoginService = new UserLoginService()).onEnable();
        (banService = new BanService()).onEnable();

        // Announce we're ready
        logger.info(getDescription().getName() + " v" + getDescription().getVersion() + " is enabled!");
    }

    @Override
    public void onDisable() {
        // Kick players


        // Shutdown services
        if(blockProtectionService != null) blockProtectionService.onDisable();
        if(userLoginService != null) userLoginService.onDisable();
        if(banService != null) banService.onDisable();

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
}
