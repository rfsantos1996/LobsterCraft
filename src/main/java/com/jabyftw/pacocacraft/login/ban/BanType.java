package com.jabyftw.pacocacraft.login.ban;

import com.jabyftw.Util;

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
public enum BanType {

    /*
     * OBS: kick message starts with "§cVocê foi expulso\n"
     * Possible information: %reason %recordDate %unbanDate %responsible
     */

    KICK(0) {
        @Override
        public String getKickMessage() {
            return "§cMotivo: §6%reason\n" +
                    "§cPor §6%responsible";
        }
    },

    TEMPORARY_BAN(1) {
        @Override
        public String getKickMessage() {
            return "§cMotivo: §6%reason\n" +
                    "§cPor §6%responsible\n" +
                    "§cBanido até §6%unbanDate";
        }
    },

    PERMANENT_BAN(2) {
        @Override
        public String getKickMessage() {
            return "§cMotivo: §6%reason\n" +
                    "§cPor §6%responsible\n" +
                    "§cBanido permanentemente";
        }
    };

    private final int databaseId;

    BanType(int databaseId) {
        this.databaseId = databaseId;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public static BanType getBanTypeFromId(int databaseId) {
        for(BanType banType : values())
            if(banType.getDatabaseId() == databaseId)
                return banType;
        return null;
    }

    public abstract String getKickMessage();
}
