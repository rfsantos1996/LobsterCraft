package com.jabyftw.lobstercraft.services.services_event;

import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
public class PlayerBecameOnlineEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final OnlinePlayer onlinePlayer;

    /**
     * This event is called when a OfflinePlayer becomes a OnlinePlayer (joins the server)
     *
     * @param onlinePlayer online player that just joined the server
     */
    public PlayerBecameOnlineEvent(@NotNull final OnlinePlayer onlinePlayer) {
        this.onlinePlayer = onlinePlayer;
    }

    public OnlinePlayer getOnlinePlayer() {
        return onlinePlayer;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
