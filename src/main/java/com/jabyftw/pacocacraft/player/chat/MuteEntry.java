package com.jabyftw.pacocacraft.player.chat;

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

    private final long playerId; // Just playerId and moderatorPlayerId are needed to check equality
    private final long moderatorPlayerId;
    private final long muteDate;

    protected long muteIndex = -1;

    public MuteEntry(final long playerId, final long moderatorPlayerId) {
        this.playerId = playerId;
        this.moderatorPlayerId = moderatorPlayerId;
        this.muteDate = System.currentTimeMillis();
    }

    public MuteEntry(final long muteIndex, final long playerId, final long moderatorPlayerId, final long muteDate) {
        this.muteIndex = muteIndex;
        this.playerId = playerId;
        this.moderatorPlayerId = moderatorPlayerId;
        this.muteDate = muteDate;
    }

    public long getMuteIndex() {
        return muteIndex;
    }

    public void setMuteIndex(final long muteIndex) {
        this.muteIndex = muteIndex;
    }

    public long getMuteDate() {
        return muteDate;
    }

    public long getModeratorPlayerId() {
        return moderatorPlayerId;
    }

    public long getPlayerId() {
        return playerId;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof MuteEntry && ((MuteEntry) obj).moderatorPlayerId == moderatorPlayerId && ((MuteEntry) obj).playerId == playerId;
    }
}
