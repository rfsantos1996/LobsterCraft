package com.jabyftw.easiercommands;

import com.sun.istack.internal.NotNull;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
public enum SenderType {

    PLAYER,
    CONSOLE,
    BOTH;

    /**
     * Check if command sender can fit into the sender type
     *
     * @param commandSender given sender of the command
     *
     * @return true if command sender fits into the requirement
     */
    public boolean canHandleCommandSender(@NotNull CommandSender commandSender) {
        return this == BOTH || (commandSender instanceof Player && this == PLAYER) || (!(commandSender instanceof Player) && this == CONSOLE);
    }
}