package com.jabyftw.pacocacraft.player.chat;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.chat.commands.MuteCommand;
import com.jabyftw.pacocacraft.player.chat.commands.ReplyCommand;
import com.jabyftw.pacocacraft.player.chat.commands.UnmuteCommand;
import com.jabyftw.pacocacraft.player.chat.commands.WhisperCommand;
import com.jabyftw.pacocacraft.util.ServerService;
import org.bukkit.Bukkit;

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
public class ChatService implements ServerService {

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginCommand("whisper").setExecutor(new WhisperCommand());
        Bukkit.getServer().getPluginCommand("r").setExecutor(new ReplyCommand());
        Bukkit.getServer().getPluginCommand("mute").setExecutor(new MuteCommand());
        Bukkit.getServer().getPluginCommand("unmute").setExecutor(new UnmuteCommand());

        Bukkit.getPluginManager().registerEvents(new ChatListener(), PacocaCraft.pacocaCraft);
        PacocaCraft.logger.info("Enabled " + getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
    }
}
