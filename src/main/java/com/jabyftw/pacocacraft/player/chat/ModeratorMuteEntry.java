package com.jabyftw.pacocacraft.player.chat;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
public class ModeratorMuteEntry extends MuteEntry {

    private final String muteReason; // Maximum of 128 characters
    private final long unmuteDate; // -1 meaning no-end mute

    public ModeratorMuteEntry(final long moderatorId, final String muteReason) { // Console
        this(-1, moderatorId, muteReason, -1);
    }

    public ModeratorMuteEntry(final long moderatorId, final long unmuteDate, final String muteReason) { // Console (temporary mute)
        this(-1, moderatorId, muteReason, unmuteDate);
    }

    public ModeratorMuteEntry(final long playerId, final long moderatorId, final String muteReason, long unmuteDate) { // Default (temporary mute - can't create another constructor with a Default permanent mute)
        super(playerId, moderatorId);
        // Check for string limits
        if(muteReason.length() > 128)
            throw new IllegalArgumentException("The mute reason can't exceed 128 characters.");
        this.muteReason = muteReason;
        this.unmuteDate = unmuteDate;
    }

    public ModeratorMuteEntry(final long muteIndex, final long playerId, final long moderatorId, final long muteDate, final String muteReason, final long unmuteDate) { // Database record
        super(muteIndex, playerId, moderatorId, muteDate);
        this.muteReason = muteReason;
        this.unmuteDate = unmuteDate;
    }

    public long getUnmuteDate() {
        return unmuteDate;
    }

    public String getMuteReason() {
        return muteReason;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof MuteEntry && ((MuteEntry) obj).getPlayerId() == getPlayerId();
    }
}
