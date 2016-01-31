package com.jabyftw.lobstercraft.player.listeners;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.NumberConversions;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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
public class JoinListener implements Listener {

    private final HashMap<String, FutureTask<PlayerHandler>> profileTaskHolder = new HashMap<>();
    private final HashMap<String, PlayerHandler> profileHandleHolder = new HashMap<>();
    private final Object handleHolderLock = new Object();

    // Number of players / number of ticks => x players can log in for each ticks (must be a double because it'll be something like 0.08 players/tick
    private final double playersPerTick = (double) LobsterCraft.config.getInt(ConfigValue.LOGIN_LIMITER_NUMBER_OF_PLAYERS.toString()) /
            (LobsterCraft.config.getInt(ConfigValue.LOGIN_LIMITER_PERIOD_OF_TIME.toString()) * 20D);
    private final int ticksPerJoin = NumberConversions.ceil(1D / playersPerTick); // Number of ticks to wait (the ceil of ticks per player)

    private long lastPlayerJoinTick;

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST) // first thing to do
    public void onAsyncPreLoginLowest(AsyncPlayerPreLoginEvent event) {
        final String playerName = event.getName().toLowerCase();

        // Check for name changes
        // TODO

        // Lets not make the server throw exceptions (it does when logging in just after server start up)
        if (LobsterCraft.getTicksPassed() < 10L) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§4Servidor inicializando...\n§cTente novamente em alguns segundos.");
            return;
        }

        // Check for amount of players logging in
        if (playersPerTick > 0 && (LobsterCraft.getTicksPassed() - lastPlayerJoinTick) < ticksPerJoin) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "§cMuitas pessoas entrando!\n§cTente novamente mais tarde.");
            return;
        }
        lastPlayerJoinTick = LobsterCraft.getTicksPassed();

        // Check if is a valid name
        if (!Util.checkStringCharactersAndLength(playerName, 3, 16)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§4Seu nome é inválido!\n§cContém caracteres inválidos ou é muito longo/curto.");
            return;
        }

        // Check if server is full
        Server server = Bukkit.getServer();
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = server.getOfflinePlayer(playerName);
        // If is full && [ can't find player || (player isn't op || player don't have permission) ]
        if (server.getOnlinePlayers().size() >= server.getMaxPlayers() && // TODO check if it is ">=" or ">" (does this player count? I don't think so, then it must be >=)
                (offlinePlayer == null ||
                        (!offlinePlayer.isOp() || !LobsterCraft.permission.playerHas(offlinePlayer.getBedSpawnLocation().getWorld().getName(), offlinePlayer, Permissions.JOIN_FULL_SERVER))
                )) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_FULL, "§4Servidor lotado!\n§cAguarde alguns segundos e tente novamente.");
            return;
        }

        // Check if player is (temporary)banned
        // TODO

        // Request player profile asynchronously
        FutureTask<PlayerHandler> task = LobsterCraft.playerHandlerService.requestProfile(playerName);
        BukkitScheduler.runTaskAsynchronously(task::run);
        profileTaskHolder.put(playerName, task);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
    public void onAsyncPreLoginHighest(AsyncPlayerPreLoginEvent event) {
        final String playerName = event.getName().toLowerCase();

        // Await player handler to be received from MySQL
        synchronized (handleHolderLock) {
            if (profileTaskHolder.containsKey(playerName))
                try {
                    profileHandleHolder.put(playerName, profileTaskHolder.remove(playerName).get());
                } catch (InterruptedException | ExecutionException | NullPointerException e) { // NullPointerException => caused by profile not being on the list
                    e.printStackTrace();
                    // This makes me use Highest instead of Monitor
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§4Falha no banco de dados.\n§cTente novamente mais tarde.");
                }
            else if(event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED)
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§4Falha no code flow.\n§cTente novamente mais tarde.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerHandler playerHandler;
        synchronized (handleHolderLock) {
            // Remove player from PlayerHandler holder
            playerHandler = profileHandleHolder.remove(event.getPlayer().getName().toLowerCase());
        }

        // Check if we're safe
        if (playerHandler == null) {
            event.getPlayer().kickPlayer("§4Falha ao detectar perfil de jogador.\n§cTente novamente mais tarde.");
            return;
        }

        // Remove join message => it'll be broadcast when signed in
        event.setJoinMessage("");

        // Initialize player handle
        playerHandler.initialize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuitHighest(PlayerQuitEvent event) {
        // Update quit message
        event.setQuitMessage(
                LobsterCraft.vanishManager.isVanished(event.getPlayer()) ? ""
                        : "§4- §c" + event.getPlayer().getName()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitMonitor(PlayerQuitEvent event) {
        // Destroy player handler
        LobsterCraft.playerHandlerService.getPlayerHandlerNoRestrictions(event.getPlayer()).destroy();
    }

}
