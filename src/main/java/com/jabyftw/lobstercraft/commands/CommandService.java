package com.jabyftw.lobstercraft.commands;

import com.jabyftw.lobstercraft.commands.chat.*;
import com.jabyftw.lobstercraft.commands.location.*;
import com.jabyftw.lobstercraft.commands.login.ChangePasswordCommand;
import com.jabyftw.lobstercraft.commands.login.ChangeUsernameCommand;
import com.jabyftw.lobstercraft.commands.login.LoginCommand;
import com.jabyftw.lobstercraft.commands.login.RegisterCommand;
import com.jabyftw.lobstercraft.commands.player.*;
import com.jabyftw.lobstercraft.commands.protection.BuildModeCommand;
import com.jabyftw.lobstercraft.util.Service;
import org.bukkit.Bukkit;
import org.bukkit.Server;

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
public class CommandService extends Service {

    public static WorldCommand worldCommand;

    @Override
    public boolean onEnable() {
        Server server = Bukkit.getServer();
        {
            // chat
            server.getPluginCommand("mute").setExecutor(new MuteCommand());
            server.getPluginCommand("unmute").setExecutor(new UnmuteCommand());
            server.getPluginCommand("whisper").setExecutor(new WhisperCommand());
            server.getPluginCommand("reply").setExecutor(new ReplyCommand());
            server.getPluginCommand("adminmute").setExecutor(new AdminMuteCommand());
            server.getPluginCommand("adminunmute").setExecutor(new AdminUnmuteCommand());
            // location
            server.getPluginCommand("teleport").setExecutor(new TeleportCommand());
            server.getPluginCommand("teleporthere").setExecutor(new TeleportHereCommand());
            server.getPluginCommand("spawnset").setExecutor(new SpawnSetCommand());
            server.getPluginCommand("spawn").setExecutor(new SpawnCommand());
            server.getPluginCommand("world").setExecutor(new WorldCommand());
            server.getPluginCommand("back").setExecutor(new BackCommand());
            server.getPluginCommand("top").setExecutor(new TopCommand());
            // login
            server.getPluginCommand("login").setExecutor(new LoginCommand());
            server.getPluginCommand("register").setExecutor(new RegisterCommand());
            server.getPluginCommand("changepassword").setExecutor(new ChangePasswordCommand());
            server.getPluginCommand("changeusername").setExecutor(new ChangeUsernameCommand());
            // player
            server.getPluginCommand("gamemode").setExecutor(new GameModeCommand());
            server.getPluginCommand("godmode").setExecutor(new GodModeCommand());
            server.getPluginCommand("fly").setExecutor(new FlyCommand());
            server.getPluginCommand("item").setExecutor(new PendingItemsCommand());
            server.getPluginCommand("give").setExecutor(new GiveCommand());
            server.getPluginCommand("heal").setExecutor(new HealCommand());
            server.getPluginCommand("enchant").setExecutor(new EnchantCommand());
            server.getPluginCommand("clearenchantment").setExecutor(new ClearEnchantmentCommand());
            server.getPluginCommand("workbench").setExecutor(new WorkbenchCommand());
            server.getPluginCommand("list").setExecutor(new ListCommand());
            server.getPluginCommand("clear").setExecutor(new ClearInventoryCommand());
            server.getPluginCommand("suicide").setExecutor(new SuicideCommand());
            server.getPluginCommand("kill").setExecutor(new KillPlayersCommand());
            server.getPluginCommand("killall").setExecutor(new KillEntitiesCommand());
            server.getPluginCommand("spawnmob").setExecutor(new SpawnEntitiesCommand());
            server.getPluginCommand("pweather").setExecutor(new PlayerWeatherCommand());
            server.getPluginCommand("ptime").setExecutor(new PlayerTimeCommand());
            server.getPluginCommand("speed").setExecutor(new SpeedCommand());
            server.getPluginCommand("repair").setExecutor(new RepairCommand());
            server.getPluginCommand("spyinv").setExecutor(new InventorySpyCommand());
            server.getPluginCommand("exp").setExecutor(new ExpCommand());
            server.getPluginCommand("level").setExecutor(new LevelCommand());
            server.getPluginCommand("feed").setExecutor(new FeedEventCommand());
            server.getPluginCommand("hat").setExecutor(new HatCommand());
            // protection
            server.getPluginCommand("buildmode").setExecutor(new BuildModeCommand());
        }
        return true;
    }

    @Override
    public void onDisable() {

    }
}
