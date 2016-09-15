package com.jabyftw.easiercommands;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OfflinePlayer;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
public abstract class READ_ME {

    public static final boolean DEBUG = false;

    public static final JavaPlugin PLUGIN = LobsterCraft.plugin;
    public static final Logger LOGGER = LobsterCraft.logger;

    public static final Class<?>
            PLAYER_CLASS = OnlinePlayer.class,
            OFFLINE_PLAYER_CLASS = OfflinePlayer.class;

    public static final String
            DESCRIPTION_HEADER = "§6",
            USAGE_HEADER = "§6Uso: §c",
            PERMISSION_HEADER = "§cVocê não tem permissão para isto!",
            DEBUG_HEADER = "[DEBUG] ";

    public static Object getPlayerThatMatches(String string) {
        return LobsterCraft.servicesManager.playerHandlerService.matchOnlinePlayer(string, OnlinePlayer.OnlineState.LOGGED_IN);
    }

    public static Object getOfflinePlayerThatMatches(String string) {
        return LobsterCraft.servicesManager.playerHandlerService.matchOfflinePlayer(string);
    }

    public static Object getPlayerClassInstance(Player player) {
        // Must be without restrictions, as we use a command to login
        return LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(player, null);
    }

    public static void sendCommandSenderMessage(CommandSender commandSender, String coloredMessage) {
        commandSender.sendMessage(coloredMessage);
    }
}
