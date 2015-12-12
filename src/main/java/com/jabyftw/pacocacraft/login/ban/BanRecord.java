package com.jabyftw.pacocacraft.login.ban;

import com.jabyftw.Util;
import com.jabyftw.pacocacraft.login.UserProfile;
import com.jabyftw.profile_util.PlayerHandler;
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
public class BanRecord {

    private final BanType banType;

    // Database variables
    private final long playerId;
    private long responsibleId = -1;
    private String responsibleName = "Unknown";
    private final long recordDate;
    private final String reason;
    private long unbanDate = 0;

    // Record variables
    private boolean onDatabase = false;

    public BanRecord(@NotNull BanType banType, long playerId, String responsibleName, long recordDate, @NotNull String reason) {
        this.banType = banType;
        this.playerId = playerId;
        this.responsibleName = responsibleName;
        this.recordDate = recordDate;
        this.reason = reason;
        this.onDatabase = true;
    }

    public BanRecord(long playerId, String responsibleName, long recordDate, @NotNull String reason, long unbanDate) {
        this(BanType.TEMPORARY_BAN, playerId, responsibleName, recordDate, reason);
        this.unbanDate = unbanDate;
    }

    public BanRecord(@NotNull BanType banType, PlayerHandler player, PlayerHandler responsible, @NotNull String reason) {
        this.banType = banType;
        // TODO make sure caller checks if player is online (user profile required)
        this.playerId = player.getProfile(UserProfile.class).getPlayerId();
        this.responsibleId = responsible.getProfile(UserProfile.class).getPlayerId();
        this.responsibleName = responsible.getPlayer().getName();
        this.recordDate = System.currentTimeMillis();
        this.reason = reason;
    }

    public BanRecord(PlayerHandler player, PlayerHandler responsible, @NotNull String reason, long unbanDate) {
        this(BanType.TEMPORARY_BAN, player, responsible, reason);
        // TODO make sure caller sums the time with currentTimeMillis
        this.unbanDate = unbanDate;
    }

    public String getKickMessage() {
        return banType.getKickMessage()
                .replaceAll("%reason", reason)
                .replaceAll("%responsible", responsibleName)
                .replaceAll("%recordDate", Util.parseTimeInMillis(recordDate, "dd/MM/yy HH:mm"))
                .replaceAll("%unbanDate", Util.parseTimeInMillis(unbanDate, "dd/MM/yy HH:mm"));
    }
}
