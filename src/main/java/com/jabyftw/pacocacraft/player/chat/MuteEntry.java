package com.jabyftw.pacocacraft.player.chat;

import com.jabyftw.profile_util.DatabaseState;
import com.sun.istack.internal.NotNull;

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
public class MuteEntry {

    private final long playerId; // Just playerId and mutedPlayerId are needed to check equality
    private final long mutedPlayerId;
    private final long muteDate;

    private long muteIndex;

    public MuteEntry(long playerId, long mutedPlayerId) {
        this.playerId = playerId;
        this.mutedPlayerId = mutedPlayerId;
        this.muteDate = System.currentTimeMillis();
    }

    public MuteEntry(long muteIndex, long playerId, long mutedPlayerId, long muteDate) {
        this.muteIndex = muteIndex;
        this.playerId = playerId;
        this.mutedPlayerId = mutedPlayerId;
        this.muteDate = muteDate;
    }

    public long getMuteIndex() {
        return muteIndex;
    }

    public void setMuteIndex(long muteIndex) {
        this.muteIndex = muteIndex;
    }

    public long getMuteDate() {
        return muteDate;
    }

    public long getMutedPlayerId() {
        return mutedPlayerId;
    }

    public long getPlayerId() {
        return playerId;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof MuteEntry && ((MuteEntry) obj).mutedPlayerId == mutedPlayerId && ((MuteEntry) obj).playerId == playerId;
    }
}
