package com.jabyftw.pacocacraft.login;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.location.TeleportProfile;
import com.jabyftw.pacocacraft.player.UserProfile;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.ExecutionException;

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
public class PlayerListener implements Listener {

    private final UserLoginService userLoginService;

    // Player join limit
    private static int playersPerTick = (int) Math.ceil(ConfigValue.LOGIN_PLAYERS_ALLOWED.<Integer>getValue() / (ConfigValue.LOGIN_PERIOD_OF_TIME.<Float>getValue() * 20f));
    private static int ticksPerJoin = (int) Math.ceil(1 / playersPerTick);
    private static long lastJoinTick = 0;

    public PlayerListener(UserLoginService userLoginService) {
        this.userLoginService = userLoginService;
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
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onAsyncPreLoginHighest(AsyncPlayerPreLoginEvent preLoginEvent) {
        String playerName = preLoginEvent.getName().toLowerCase(); // Lower case everything

        // Check if server is full
        Server server = PacocaCraft.server;
        @SuppressWarnings("deprecation") OfflinePlayer offlinePlayer = server.getOfflinePlayer(playerName);
        // If is full && [ can't find player || (player isn't op || player don't have permission) ]
        if(server.getOnlinePlayers().size() > server.getMaxPlayers() &&
                (offlinePlayer == null ||
                        (!offlinePlayer.isOp() || !PacocaCraft.permission.playerHas(offlinePlayer.getBedSpawnLocation().getWorld().getName(), offlinePlayer, Permissions.JOIN_FULL_SERVER)))) {
            preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "§4Servidor lotado!\n§cAguarde alguns segundos e tente novamente");
            return;
        }

        // Check for number of joins if set to a number > 0 and if distance (in ticks) since last join is shorter than the required ticks
        if(playersPerTick > 0 && (PacocaCraft.currentTick - lastJoinTick) < ticksPerJoin) {
            preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "§4Muitos usuários entrando!\n§cAguarde alguns segundos e tente novamente");
            return;
        }
        lastJoinTick = PacocaCraft.currentTick;

        // Check player's name (today the minimum length is 4, but there may be players using 3 letters still)
        if(!com.jabyftw.Util.checkString(playerName, 3, 16)) {
            preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§cSeu nome contém caracteres inválidos\n§cou é muito longo/curto.");
            return;
        }

        // Check if it wasn't already denied -- Why here? Because I want to overwrite messages
        if(preLoginEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            return;

        // Check player's ban record
        BanRecord banRecord;
        if((banRecord = PacocaCraft.banService.isPlayerBanned(playerName)) != null || (banRecord = PacocaCraft.banService.isIPBanned(preLoginEvent.getAddress().getAddress())) != null) {
            preLoginEvent.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§cVocê está banido:\n" + banRecord.getMessage());
            return;
        }

        // Request profile, let other plugins do their job
        userLoginService.requestProfile(playerName);
    }

    /**
     * Make sure profile is ready before player login
     *
     * @param preLoginEvent async player pre-login event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onAsyncPreLoginLow(AsyncPlayerPreLoginEvent preLoginEvent) {
        // quietly join profile (never lock main thread)
        userLoginService.awaitProfile(preLoginEvent.getName().toLowerCase());
    }

    /**
     * Gets UserProfile instance requested on user login service and apply to the player
     *
     * @param joinEvent player join event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerJoinEvent joinEvent) {
        Player player = joinEvent.getPlayer();

        // Retrieve user profile
        UserProfile userProfile;
        try {
            userProfile = userLoginService.getProfile(player.getName().toLowerCase());
        } catch(ExecutionException | InterruptedException e) {
            player.kickPlayer("§cOcorreu um erro com seu perfil de usuário!\n§cTente novamente mais tarde.");
            joinEvent.setJoinMessage("");
            e.printStackTrace();
            return;
        }

        // Apply player to user profile
        userProfile.setPlayerInstance(player);

        // Update login message
        joinEvent.setJoinMessage(PacocaCraft.permission.playerHas(player, Permissions.JOIN_VANISHED) ? "" : "§b+ §3" + player.getName());
    }


}