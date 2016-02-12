package com.jabyftw.lobstercraft.player.util;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
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
public class BannedPlayerEntry {

    private final long recordId, playerId;
    private final BanType banType;
    private final String reason;
    private final long recordDate;
    private final Long unbanDate, responsibleId;

    public BannedPlayerEntry(long recordId, long playerId, @Nullable Long responsibleId, @NotNull final BanType banType, @NotNull final String reason, long recordDate, @Nullable Long unbanDate) {
        this.recordId = recordId;
        this.playerId = playerId;
        this.responsibleId = responsibleId;
        this.banType = banType;
        this.reason = reason;
        this.recordDate = recordDate;
        this.unbanDate = unbanDate;
    }

    public long getPlayerId() {
        return playerId;
    }

    /**
     * Get the responsible's playerId, -1 means that it was banned by the console
     *
     * @return playerId, -1 in case of the console
     * @see com.jabyftw.lobstercraft.player.PlayerHandler#UNDEFINED_PLAYER constant -1 meaning unregistered player or, in some cases, the console
     */
    public long getResponsibleId() {
        return responsibleId;
    }

    public long getRecordDate() {
        return recordDate;
    }

    public BanType getBanType() {
        return banType;
    }

    public String getReason() {
        return reason;
    }

    /**
     * @return the unban date, null if player is permanently banned
     */
    public Long getUnbanDate() {
        return unbanDate;
    }

    public boolean isPermanent() {
        return unbanDate == null;
    }

    public boolean isBanned() {
        return (unbanDate == null && banType == BanType.PLAYER_PERMANENTLY_BANNED) ||
                (banType == BanType.PLAYER_TEMPORARILY_BANNED && unbanDate != null && unbanDate > System.currentTimeMillis());
    }

    public boolean responsibleIsAPlayer() {
        return responsibleId != null;
    }

    public String getKickMessage() {
        return banType.getKickMessageBase()
                .replaceAll("%responsible%", responsibleIsAPlayer() ? LobsterCraft.playerHandlerService.getOfflinePlayer(responsibleId).getPlayerName() : "console")
                .replaceAll("%reason%", reason)
                .replaceAll("%unbanDate%", unbanDate == null ? "indefinidamente" : Util.formatDate(unbanDate));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BannedPlayerEntry && ((BannedPlayerEntry) obj).recordId == recordId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(5, 63).append(recordId).toHashCode();
    }

    public enum BanType {

        PLAYER_KICKED((byte) 1, "expulsão",
                "§6Você foi expulso por §c%responsible%\n" +
                        "§6Motivo: §c\"%reason%\""
        ),
        PLAYER_PERMANENTLY_BANNED((byte) 2, "ban temporário",
                "§6Você foi banido por §c%responsible%\n" +
                        "§6Motivo: §c\"%reason%\"\n" +
                        "§6Banido permanentemente"
        ),
        PLAYER_TEMPORARILY_BANNED((byte) 3, "ban permanente",
                "§6Você foi banido por §c%responsible%\n" +
                        "§6Motivo: §c\"%reason%\"\n" +
                        "§6Banido até §c%unbanDate%"
        );

        private final byte type;
        private final String typeName, kickMessage;

        BanType(byte type, @NotNull final String typeName, @NotNull final String kickMessage) {
            this.type = type;
            this.typeName = typeName;
            this.kickMessage = kickMessage;
        }

        public byte getType() {
            return type;
        }

        public String getTypeName() {
            return typeName;
        }

        public static BanType getBanType(int type) {
            return getBanType((byte) type);
        }

        public static BanType getBanType(byte type) {
            for (BanType banType : values())
                if (banType.getType() == type)
                    return banType;
            return null;
        }

        public String getKickMessageBase() {
            return kickMessage;
        }
    }
}
