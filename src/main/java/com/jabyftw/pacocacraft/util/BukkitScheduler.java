package com.jabyftw.pacocacraft.util;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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
public abstract class BukkitScheduler {

    /**
     * @param plugin   the plugin that schedules the task
     * @param runnable the task to be ran
     *
     * @return a BukkitTask if server is not closing, null otherwise
     */
    public static BukkitTask runTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if(PacocaCraft.isServerClosing())
            runnable.run();
        else
            return Bukkit.getScheduler().runTask(plugin, runnable);
        return null;
    }

    /**
     * @param plugin   the plugin that schedules the task
     * @param runnable the task to be ran
     *
     * @return a BukkitTask if server is not closing, null otherwise
     */
    public static BukkitTask runTaskAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if(PacocaCraft.isServerClosing())
            runnable.run();
        else
            return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        return null;

    }

    /**
     * @param plugin   the plugin that schedules the task
     * @param runnable the task to be ran
     * @param delay    task delay, ignored when server is closing
     *
     * @return a BukkitTask if server is not closing, null otherwise
     *
     * @throws IllegalStateException if server is closing and tried to schedule a task for future
     */
    public static BukkitTask runTaskLater(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) throws IllegalStateException {
        if(PacocaCraft.isServerClosing())
            throw new IllegalStateException("Server is closing, can't schedule task to later!");
        else
            return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    /**
     * @param plugin   the plugin that schedules the task
     * @param runnable the task to be ran
     * @param delay    task delay, ignored when server is closing
     *
     * @return a BukkitTask if server is not closing, null otherwise
     *
     * @throws IllegalStateException if server is closing and tried to schedule a task for future
     */
    public static BukkitTask runTaskLaterAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) throws IllegalStateException {
        if(PacocaCraft.isServerClosing())
            throw new IllegalStateException("Server is closing, can't schedule task to later!");
        else
            return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    /**
     * @param plugin   the plugin that schedules the task
     * @param runnable the task to be ran
     * @param delay    task delay, ignored when server is closing
     * @param period   period between task ran
     *
     * @return a BukkitTask if server is not closing, null otherwise
     *
     * @throws IllegalStateException if server is closing and tried to schedule a task for future
     */
    public static BukkitTask runTaskTimer(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) throws IllegalStateException {
        if(PacocaCraft.isServerClosing())
            throw new IllegalStateException("Server is closing, can't schedule task to later!");
        else
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
    }

    /**
     * @param plugin   the plugin that schedules the task
     * @param runnable the task to be ran
     * @param delay    task delay, ignored when server is closing
     * @param period   period between task ran
     *
     * @return a BukkitTask if server is not closing, null otherwise
     *
     * @throws IllegalStateException if server is closing and tried to schedule a task for future
     */
    public static BukkitTask runTaskTimerAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) throws IllegalStateException {
        if(PacocaCraft.isServerClosing())
            throw new IllegalStateException("Server is closing, can't schedule task to later!");
        else
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
    }
}
