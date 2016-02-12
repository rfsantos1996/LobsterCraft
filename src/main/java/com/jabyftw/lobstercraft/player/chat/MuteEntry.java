package com.jabyftw.lobstercraft.player.chat;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
public class MuteEntry {

    private long muteIndex = PlayerHandler.UNDEFINED_PLAYER;
    private final long mutedId, ownerId, muteDate;

    public MuteEntry(long muteIndex, long mutedId, long ownerId, long muteDate) {
        this(mutedId, ownerId, muteDate);
        this.muteIndex = muteIndex;
    }

    public MuteEntry(long mutedId, long ownerId, long muteDate) {
        this.mutedId = mutedId;
        this.ownerId = ownerId;
        this.muteDate = muteDate;
    }

    public long getMuteIndex() {
        return muteIndex;
    }

    public long getMutedPlayerId() {
        return mutedId;
    }

    public long getOwnerPlayerId() {
        return ownerId;
    }

    public long getMuteDate() {
        return muteDate;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MuteEntry && ((MuteEntry) obj).mutedId == mutedId && ((MuteEntry) obj).ownerId == ownerId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(mutedId)
                .append(ownerId)
                .toHashCode();
    }
}
