package com.jabyftw.pacocacraft.login;

import com.jabyftw.Util;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.login.ban.BanRecord;
import com.jabyftw.pacocacraft.login.ban.BanService;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
public class JoinListener implements Listener {

    private final UserLoginService userLoginService;

    // Changed player names cache
    public final static long TIME_REQUIRED_TO_USERNAME_BE_AVAILABLE = TimeUnit.DAYS.toMillis(PacocaCraft.config.getLong(ConfigValue.LOGIN_REQUIRED_TIME_USERNAME_AVAILABLE.getPath()));
    public final static HashMap<String, Long> cachedChangedNames = new HashMap<>();

    // Player join limit
    private static int playersPerTick = (int) Math.ceil(PacocaCraft.config.getInt(ConfigValue.LOGIN_JOIN_LIMITER_PLAYERS_ALLOWED.getPath()) /
            (PacocaCraft.config.getDouble(ConfigValue.LOGIN_JOIN_LIMITER_PERIOD_OF_TIME.getPath()) * 20d));
    private static int ticksPerJoin = (int) Math.ceil(1 / playersPerTick);
    private static long lastJoinTick = 0;

    public JoinListener(@NotNull UserLoginService userLoginService) {
        this.userLoginService = userLoginService;
        try {
            cacheChangedNames();
        } catch(SQLException e) {
            e.printStackTrace();
            PacocaCraft.logger.severe("§cFailed to cache recent changed names!");
        }
    }

    /**
     * Check if player is ok on:
     * - if server is full and don't have permission to join
     * - number of players joining per second
     * - player name (A-Za-z0-9_ with [3,16] letters)
     * - is already denied
     * - check on cached bans
     * If passed, ask and wait for MySQL response (login profile) and create UserProfile
     *
     * @param preLoginEvent async player pre-login event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    // the first thing to do on AsyncPlayerPreLoginEvent
    public void onAsyncPreLoginHighest(AsyncPlayerPreLoginEvent preLoginEvent) {
        String playerName = preLoginEvent.getName().toLowerCase(); // Lower case everything

        Long changedDate = cachedChangedNames.get(playerName);
        // Check if there is a date
        if(changedDate != null) {
            long releaseDate = changedDate + TIME_REQUIRED_TO_USERNAME_BE_AVAILABLE;

            // Check if release time will be on a future time
            if(releaseDate >= System.currentTimeMillis()) {
                preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§cNome de usuário não disponível.\n" +
                        "§cTente daqui: §4" + Util.parseTimeInMillis(Math.abs(releaseDate - System.currentTimeMillis())));
                return;
            }
        }

        // Check player name characters and length
        if(!isValidPlayerName(playerName)) {
            preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§cSeu nome contém caracteres inválidos\n§cou é muito longo/curto.");
            return;
        }

        // Check for number of joins if set to a number > 0 and if distance (in ticks) since last join is shorter than the required ticks
        if(playersPerTick > 0 && (PacocaCraft.getCurrentTick() - lastJoinTick) < ticksPerJoin) {
            preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "§4Muitos usuários entrando!\n§cAguarde alguns segundos e tente novamente");
            return;
        }
        lastJoinTick = PacocaCraft.getCurrentTick();

        // Check if server is full
        Server server = Bukkit.getServer();
        @SuppressWarnings("deprecation") OfflinePlayer offlinePlayer = server.getOfflinePlayer(playerName);
        // If is full && [ can't find player || (player isn't op || player don't have permission) ]
        if(server.getOnlinePlayers().size() > server.getMaxPlayers() &&
                (offlinePlayer == null ||
                        (!offlinePlayer.isOp() || !PacocaCraft.permission.playerHas(offlinePlayer.getBedSpawnLocation().getWorld().getName(), offlinePlayer, Permissions.JOIN_FULL_SERVER)))) {
            preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "§4Servidor lotado!\n§cAguarde alguns segundos e tente novamente");
            return;
        }

        // Check if it wasn't already denied -- Why here? Because I want to overwrite messages before
        if(preLoginEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            return;

        // Check player's ban record
        BanRecord banRecord;
        if((banRecord = BanService.isPlayerBanned(playerName)) != null) { // || (banRecord = BanService.isIPBanned(preLoginEvent.getAddress().getAddress())) != null) {
            preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§cVocê está banido\n" + banRecord.getKickMessage());
            return;
        }

        // Request profile, let other plugins do their job
        userLoginService.requestUserProfile(playerName);
    }

    /**
     * Make sure profile is ready before player login
     *
     * @param preLoginEvent async player pre-login event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    // the last thing to do on AsyncPlayerPreLoginEvent
    public void onAsyncPreLoginMonitor(AsyncPlayerPreLoginEvent preLoginEvent) {
        // Wait for profile (never lock main thread)
        if(preLoginEvent.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED)
            userLoginService.waitUserProfile(preLoginEvent.getName());
    }

    /**
     * Gets UserProfile instance requested on user login service and apply to the player
     *
     * @param joinEvent player join event
     */
    @EventHandler(priority = EventPriority.LOWEST) // the first one
    public void onPlayerJoinMonitor(PlayerJoinEvent joinEvent) {
        Player player = joinEvent.getPlayer();
        try {
            // Retrieve user profile
            UserProfile userProfile;
            userProfile = userLoginService.getUserProfile(player.getName());

            // Remove login message
            joinEvent.setJoinMessage("");

            // Create PlayerHandler (it'll add itself to the player list automatically)
            new PlayerHandler(player, userProfile);
        } catch(Exception e) {
            player.kickPlayer("§cOcorreu um erro com seu perfil de usuário!\n§cTente novamente mais tarde.");
            e.printStackTrace();
        }
    }

    /**
     * Sends UserProfile instance to waiting queue and remove player instance
     *
     * @param quitEvent player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR) // the last thing to do: destroy PlayerHandler
    public void onPlayerQuitMonitor(PlayerQuitEvent quitEvent) {
        // Destroy PlayerHandler instance
        PacocaCraft.getPlayerHandler(quitEvent.getPlayer()).destroy();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuitHighest(PlayerQuitEvent quitEvent) {
        // Update and show quit message if player is visible
        quitEvent.setQuitMessage(PacocaCraft.getPlayerHandler(quitEvent.getPlayer()).isInvisible() ? "" : "§4- §c" + quitEvent.getPlayer().getName());
    }

    public static boolean isValidPlayerName(String playerName) {
        // Check player's name (today the minimum length is 4, but there may be players using 3 letters still)
        return Util.checkStringCharactersAndLength(playerName, 3, 16);
    }

    public static void cacheChangedNames() throws SQLException {
        Connection connection = PacocaCraft.dataSource.getConnection();
        {
            // Prepare statement
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT `oldPlayerName`, `changeDate` " +
                    "FROM minecraft.recent_username_changes WHERE `recent_username_changes`.`changeDate` > ?;");

            // Cache just those who can't log in (less than the required time)
            preparedStatement.setLong(1, System.currentTimeMillis() - TIME_REQUIRED_TO_USERNAME_BE_AVAILABLE); // now - required time will filter the cached ones to be available

            // Execute query
            ResultSet resultSet = preparedStatement.executeQuery();

            // Iterate through rows
            while(resultSet.next()) {
                cachedChangedNames.put(resultSet.getString("oldPlayerName").toLowerCase(), resultSet.getLong("changeDate"));
            }
            PacocaCraft.logger.info("Cached " + cachedChangedNames.size() + " recent changed usernames.");

            // Close everything
            resultSet.close();
            preparedStatement.close();
        }
        // Close connection
        connection.close();
    }
}
