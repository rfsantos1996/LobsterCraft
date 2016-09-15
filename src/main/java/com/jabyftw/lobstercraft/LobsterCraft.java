package com.jabyftw.lobstercraft;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.jabyftw.lobstercraft.services.ServicesManager;
import com.jabyftw.lobstercraft.util.ConfigurationFile;
import com.jabyftw.lobstercraft.util.Util;
import com.sk89q.worldedit.WorldEdit;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.kitteh.vanish.VanishManager;
import org.kitteh.vanish.staticaccess.VanishNoPacket;
import org.kitteh.vanish.staticaccess.VanishNotLoadedException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
public class LobsterCraft extends JavaPlugin {

    // "Global" "final" variables
    public static Logger logger;
    public static LobsterCraft plugin;
    public static ConfigurationFile configuration;
    public static TickCounter tickCounter;

    // "Global" variables
    public static volatile boolean serverClosing; // Volatile: simple write/read (although primitives are already atomic) + used on some threads

    // "Global" dependencies
    public static HikariDataSource dataSource;
    public static Chat chat;
    public static Permission permission;
    public static ProtocolManager protocolManager;
    public static VanishManager vanishManager;
    public static WorldEdit worldEdit;

    // Services
    public static ServicesManager servicesManager;

    @Override
    public void onEnable() {
        // Set static variables
        LobsterCraft.logger = getLogger();
        LobsterCraft.plugin = this;
        serverClosing = false;
        tickCounter = new TickCounter();

        // Schedule tick counting (needed for early login problem)
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> tickCounter.increment(), 1L, 1L);

        // Setup configuration file
        try {
            LobsterCraft.configuration = new ConfigurationFile(plugin, "config.yml");
        } catch (IOException | InvalidConfigurationException exception) {
            logger.severe(Util.appendStrings("Can't continue without configuration file: ", exception.getMessage()));
            exception.printStackTrace();
            Bukkit.shutdown();
            return;
        }

        // Setup MySQL
        {
            final int CONNECTION_TIMEOUT_SECONDS = 2;

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDataSourceClassName("org.mariadb.jdbc.MySQLDataSource");
            hikariConfig.setJdbcUrl(configuration.getString(ConfigurationValues.MYSQL_JDBC_URL.toString()));
            hikariConfig.setUsername(configuration.getString(ConfigurationValues.MYSQL_USERNAME.toString()));
            hikariConfig.setPassword(configuration.getString(ConfigurationValues.MYSQL_PASSWORD.toString()));
            hikariConfig.setMaximumPoolSize(configuration.getInt(ConfigurationValues.MYSQL_POOL_SIZE.toString())); // From documentation: "(Core count * 2) + spindle"
            hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT_SECONDS)); // milliseconds before throwing exceptions
            hikariConfig.setMaxLifetime(TimeUnit.SECONDS.toMillis(configuration.getInt(ConfigurationValues.MYSQL_CONNECTION_TIMEOUT_SECONDS.toString()) - 60)); // One minute
            // before database's idle timeout

            // Create instance
            dataSource = new HikariDataSource(hikariConfig);

            // Test connection
            try {
                if (!dataSource.getConnection().isValid(CONNECTION_TIMEOUT_SECONDS))
                    throw new SQLException("Connection isn't valid.");
            } catch (SQLException exception) {
                logger.severe(Util.appendStrings("Can't continue without MySQL connection: ", exception.getMessage()));
                exception.printStackTrace();
                Bukkit.shutdown();
                return;
            }
        }

        // Checking for other plugins
        try {
            // Check for Vault
            if (getServer().getPluginManager().getPlugin("Vault") == null)
                throw new IllegalStateException("Vault not found!");

            // Setup Vault
            {
                RegisteredServiceProvider<Permission> permissionServiceProvider = getServer().getServicesManager().getRegistration(Permission.class);
                RegisteredServiceProvider<Chat> chatServiceProvider = getServer().getServicesManager().getRegistration(Chat.class);

                // Check if was successful
                if ((chat = chatServiceProvider.getProvider()) == null || (permission = permissionServiceProvider.getProvider()) == null)
                    throw new IllegalStateException("Vault not started or failed to start!");
            }

            // Check for ProtocolLib
            if (getServer().getPluginManager().getPlugin("ProtocolLib") == null)
                throw new IllegalStateException("ProtocolLib not found!");

            // Setup ProtocolLib
            protocolManager = ProtocolLibrary.getProtocolManager();
            if (protocolManager == null)
                throw new IllegalStateException("ProtocolLib not started!");

            // Check for VanishNoPacket
            if (getServer().getPluginManager().getPlugin("VanishNoPacket") == null)
                throw new IllegalStateException("VanishNoPacket not found!");

            // Setup VanishNoPacket
            try {
                //noinspection deprecation
                vanishManager = VanishNoPacket.getManager();
            } catch (VanishNotLoadedException exception) {
                exception.printStackTrace();
                throw new IllegalStateException("VanishNoPacket not started!");
            }

            // Check for WorldEdit
            if (getServer().getPluginManager().getPlugin("WorldEdit") == null)
                throw new IllegalStateException("WorldEdit not found!");

            // Setup WorldEdit
            worldEdit = WorldEdit.getInstance();
            if (worldEdit == null)
                throw new IllegalStateException("WorldEdit not started!");
        } catch (IllegalStateException exception) {
            logger.severe(Util.appendStrings("Can't continue without dependency: ", exception.getMessage()));
            exception.printStackTrace();
            Bukkit.shutdown();
            return;
        }

        // Start services
        try {
            servicesManager = new ServicesManager();
        } catch (Exception exception) {
            logger.severe(Util.appendStrings("Couldn't register services: ", exception.getMessage()));
            exception.printStackTrace();
            Bukkit.shutdown();
            return;
        }

        // Announce success :)
        logger.info(Util.appendStrings(getDescription().getName(), " v", getDescription().getVersion(), " is enabled!"));
        super.onEnable();
    }

    @Override
    public void onDisable() {
        // Closing server, we should run tasks synchronously now
        serverClosing = true;

        // Close services
        if (servicesManager != null)
            servicesManager.onDisable();
        servicesManager = null;

        // Erase dependencies variables
        chat = null;
        permission = null;
        protocolManager = null;
        vanishManager = null;
        worldEdit = null;

        // Close MySQL connection and erase variable
        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
        dataSource = null;

        // Close configuration file
        if (configuration != null)
            try {
                configuration.saveFile();
            } catch (IOException exception) {
                logger.warning(Util.appendStrings("Failed to save configuration file ", configuration.getFileName(), ": ", exception.getMessage()));
                exception.printStackTrace();
            }
        // Erase variable
        configuration = null;

        // Announce exit and erase variable
        logger.info(Util.appendStrings(getDescription().getName(), " v", getDescription().getVersion(), " is disabled!"));
        logger = null;
        super.onDisable();
    }

    @SuppressWarnings("WeakerAccess")
    public class TickCounter {

        private long tick = 0;

        synchronized void increment() {
            tick += 1;
        }

        public synchronized long getTick() {
            return tick;
        }
    }
}
