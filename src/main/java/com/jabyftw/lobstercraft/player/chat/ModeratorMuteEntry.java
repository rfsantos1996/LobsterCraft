package com.jabyftw.lobstercraft.player.chat;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.sun.istack.internal.NotNull;
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
public class ModeratorMuteEntry {

    private long muteIndex = PlayerHandler.UNDEFINED_PLAYER;

    private final long mutedId, muteDate;
    private Long moderatorId;
    private long unmuteDate;
    private String reason;

    private boolean updated = false;

    public ModeratorMuteEntry(long muteIndex, long mutedId, Long moderatorId, long muteDate, long unmuteDate, @NotNull final String reason) {
        this.muteIndex = muteIndex;
        this.mutedId = mutedId;
        this.moderatorId = moderatorId;
        this.reason = reason;
        this.muteDate = muteDate;
        this.unmuteDate = unmuteDate;
        if (reason.length() > 128)
            throw new IllegalArgumentException("Reason is more than 128 characters."); // This constructor is for database usage, but lets check anyway
    }

    public ModeratorMuteEntry(long mutedId, Long moderatorId, long unmuteDate, @NotNull final String reason) {
        this.mutedId = mutedId;
        this.moderatorId = moderatorId;
        this.muteDate = System.currentTimeMillis();
        this.unmuteDate = unmuteDate;
        this.reason = reason;
        if (reason.length() > 128) throw new IllegalArgumentException("Reason is more than 128 characters.");
    }

    public long getMuteIndex() {
        return muteIndex;
    }

    public long getMutedPlayerId() {
        return mutedId;
    }

    public Long getModeratorId() {
        return moderatorId;
    }

    public long getMuteDate() {
        return muteDate;
    }

    public long getUnmuteDate() {
        return unmuteDate;
    }

    public String getReason() {
        return reason;
    }

    public boolean isValid() {
        return unmuteDate > System.currentTimeMillis();
    }

    public void updateMute(long unmuteDate, Long moderatorId, @NotNull final String reason) {
        if (reason.length() > 128)
            throw new IllegalArgumentException("Reason has more than 128 characters. Invalid for the database setup.");
        this.unmuteDate = unmuteDate;
        this.moderatorId = moderatorId;
        this.reason = reason;
        this.updated = true;
    }

    public void unmute() {
        this.unmuteDate = 0;
    }

    public DatabaseState getDatabaseState() {
        if (muteIndex >= 0) {
            if (isValid())
                return updated ? DatabaseState.UPDATE_DATABASE : DatabaseState.ON_DATABASE;
            else
                return DatabaseState.DELETE_FROM_DATABASE;
        } else {
            return isValid() ? DatabaseState.INSERT_TO_DATABASE : DatabaseState.NOT_ON_DATABASE;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ModeratorMuteEntry && ((ModeratorMuteEntry) obj).mutedId == mutedId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(mutedId)
                .toHashCode();
    }
}
