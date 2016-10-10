package com.jabyftw.lobstercraft.services.services_event;

import com.sun.istack.internal.NotNull;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import javax.annotation.Nullable;

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
public class AsyncPlayerPreJoinEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private AsyncPlayerPreLoginEvent.Result result = AsyncPlayerPreLoginEvent.Result.ALLOWED;
    private String kickMessage = null;

    /**
     * This event is called when a OfflinePlayer is joining the server (asynchronous)
     */
    public AsyncPlayerPreJoinEvent() {
    }

    @Override
    public boolean isCancelled() {
        return result != AsyncPlayerPreLoginEvent.Result.ALLOWED;
    }

    public AsyncPlayerPreLoginEvent.Result getResult() {
        return result;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public void setCancelled(@NotNull AsyncPlayerPreLoginEvent.Result result, @Nullable String kickMessage) {
        this.result = result;
        if (result != AsyncPlayerPreLoginEvent.Result.ALLOWED && kickMessage == null)
            throw new IllegalStateException("Can't deny join without giving a reason");
        this.kickMessage = kickMessage;
    }

    @Deprecated
    @Override
    public void setCancelled(boolean cancel) {
        throw new NotImplementedException("Method not implemented!");
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
