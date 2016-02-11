package com.jabyftw.lobstercraft;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.jabyftw.lobstercraft.commands.CommandService;
import com.jabyftw.lobstercraft.player.PlayerHandlerService;
import com.jabyftw.lobstercraft.player.PlayerService;
import com.jabyftw.lobstercraft.player.ProfileService;
import com.jabyftw.lobstercraft.player.chat.ChatService;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.ConfigurationFile;
import com.jabyftw.lobstercraft.util.Service;
import com.jabyftw.lobstercraft.world.BlockController;
import com.jabyftw.lobstercraft.world.CityService;
import com.jabyftw.lobstercraft.world.ConstructionsService;
import com.jabyftw.lobstercraft.world.WorldService;
import com.jabyftw.lobstercraft.world.xray_protection.XrayProtectionService;
import com.sk89q.worldedit.WorldEdit;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.kitteh.vanish.VanishManager;
import org.kitteh.vanish.staticaccess.VanishNoPacket;

import java.io.IOException;
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

    // Plugin specific variables
    public static LobsterCraft lobsterCraft;
    public static Logger logger;
    public static ConfigurationFile config;

    // MySQL (thread safe)
    public static HikariDataSource dataSource;

    // ProtocolLib
    public static ProtocolManager protocolManager;

    // VanishNoPacket
    public static VanishManager vanishManager;

    // WorldEdit
    public static WorldEdit worldEdit;

    // Vault
    public static Chat chat;
    public static Permission permission;

    // Public services
    public static ProfileService profileService;
    public static PlayerHandlerService playerHandlerService;
    public static PlayerService playerService;
    public static WorldService worldService;
    public static XrayProtectionService xrayProtectionService;
    public static BlockController blockController;
    public static CityService cityService;
    public static ConstructionsService constructionsService;
    public static ChatService chatService;

    // State specific variables
    private final static Object timerLock = new Object();
    public static volatile boolean serverClosing;
    private static long ticksPassed = 0;

    // Services
    private Service[] serverServices;

    public static long getTicksPassed() {
        synchronized (timerLock) {
            return ticksPassed;
        }
    }

    @Override
    public void onEnable() {
        // Set instance
        LobsterCraft.lobsterCraft = this;
        LobsterCraft.logger = getLogger();

        // Set state
        serverClosing = false;

        // Catch any exception on initialization => drop plugin
        try {
            // Initialize configuration
            config = new ConfigurationFile(this, "config");

            // Schedule timer
            BukkitScheduler.runTaskTimer(() -> {
                synchronized (timerLock) {
                    ticksPassed += 1;
                }
            }, 0, 1);

            // Setup MySQL
            {
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setDataSourceClassName("org.mariadb.jdbc.MySQLDataSource");
                hikariConfig.setJdbcUrl(LobsterCraft.config.getString(ConfigValue.MYSQL_JDBC_URL.getPath()));
                hikariConfig.setUsername(LobsterCraft.config.getString(ConfigValue.MYSQL_USERNAME.getPath()));
                hikariConfig.setPassword(LobsterCraft.config.getString(ConfigValue.MYSQL_PASSWORD.getPath()));
                hikariConfig.setMaximumPoolSize(LobsterCraft.config.getInt(ConfigValue.MYSQL_POOL_SIZE.getPath())); // (Core count * 2) + spindle (?)
                hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(3)); // 3 seconds before throwing exceptions
                hikariConfig.setMaxLifetime(TimeUnit.SECONDS.toMillis(LobsterCraft.config.getInt(ConfigValue.MYSQL_CONNECTION_TIMEOUT.getPath()) - 60)); // one minute before MariaDB's idle timeout

                // Connect to database, close if it doesn't
                dataSource = new HikariDataSource(hikariConfig);
            }

            // Check for Vault
            if (getServer().getPluginManager().getPlugin("Vault") == null)
                throw new IllegalStateException("Failed to start Vault! Go get it before!");

            // Setup Vault
            {
                RegisteredServiceProvider<Permission> permissionServiceProvider = getServer().getServicesManager().getRegistration(Permission.class);
                RegisteredServiceProvider<Chat> chatServiceProvider = getServer().getServicesManager().getRegistration(Chat.class);

                // Check if was successful
                if ((chat = chatServiceProvider.getProvider()) == null || (permission = permissionServiceProvider.getProvider()) == null)
                    throw new IllegalStateException("Failed to start Vault's chat service!");
            }

            // Check for ProtocolLib
            if (getServer().getPluginManager().getPlugin("ProtocolLib") == null)
                throw new IllegalStateException("Failed to start ProtocolLib! Go get it before!");

            // Setup ProtocolLib
            protocolManager = ProtocolLibrary.getProtocolManager();

            // Check for VanishNoPacket
            if (getServer().getPluginManager().getPlugin("VanishNoPacket") == null)
                throw new IllegalStateException("Failed to find VanishNoPacket!");

            // Setup VanishNoPacket
            //noinspection deprecation
            vanishManager = VanishNoPacket.getManager();

            // Check for VanishNoPacket
            if (getServer().getPluginManager().getPlugin("WorldEdit") == null)
                throw new IllegalStateException("Failed to find WorldEdit!");

            // Setup WorldEdit
            worldEdit = WorldEdit.getInstance();

            // Setup services
            serverServices = new Service[]{
                    playerHandlerService = new PlayerHandlerService(),
                    playerService = new PlayerService(),
                    profileService = new ProfileService(),
                    worldService = new WorldService(),
                    xrayProtectionService = new XrayProtectionService(),
                    new CommandService(),
                    blockController = new BlockController(),
                    cityService = new CityService(),
                    constructionsService = new ConstructionsService(),
                    chatService = new ChatService()
            };

            // Start up all services
            for (Service serverService : serverServices) {
                if (!serverService.onEnable())
                    throw new IllegalStateException("Service failed: " + serverService.getName());
                else
                    LobsterCraft.logger.info("Enabled " + serverService.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            setEnabled(false);
            return;
        }

        // Announce if was successful
        logger.info(getDescription().getName() + " v" + getDescription().getVersion() + " is enabled!");
    }

    @Override
    public void onDisable() {
        // Set server as closing => no more scheduling tasks but running them synchronously
        serverClosing = true;

        // Kick every player
        for (Player player : getServer().getOnlinePlayers()) {
            // Lets make sure that the message on the disable is different than the stop command
            player.kickPlayer("§4Ocorreu um erro!");
            // Destroy isn't called by event when server is closing
            playerHandlerService.getPlayerHandlerNoRestrictions(player).destroy();
        }

        // Stop services
        for (Service serverService : serverServices) {
            try {
                serverService.onDisable();
                LobsterCraft.logger.info(serverService.getName() + " disabled successfully.");
            } catch (Exception e) {
                e.printStackTrace();
                LobsterCraft.logger.info(serverService.getName() + " failed to disabled.");
            }
        }

        // Erase variable
        profileService = null;
        playerHandlerService = null;
        playerService = null;
        worldService = null;
        xrayProtectionService = null;
        blockController = null;
        constructionsService = null;
        cityService = null;
        chatService = null;

        // Delete instances
        serverServices = null;

        // Close MySQL
        if (dataSource != null && !dataSource.isClosed())
            dataSource.close();
        dataSource = null;

        // Save configuration
        if (config != null)
            try {
                config.saveFile();
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning("Failed to save configuration file.");
            }
        config = null;

        // Announce disabled
        logger.info(getDescription().getName() + " is disabled!");
        logger = null;
    }
}
