package com.jabyftw.pacocacraft.login;

import com.jabyftw.pacocacraft.util.ServerService;

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
// TODO finish
public class BanService implements ServerService {

    @Override
    public void onEnable() {
        // Cache MySQL bans
        // Move old bans to player history and mark to delete
    }

    @Override
    public void onDisable() {
        // Save modified bans on ban database
        // Save modified bans on player history
        // Delete from ban database marked bans
    }

    // add bans methods

    /**
     * Check on database (cache) if player name is banned
     *
     * @param playerName player's name for lookup
     *
     * @return not null response if banned
     */
    public BanRecord isPlayerBanned(String playerName) {
        return null;
    }


    /**
     * Check on database (cache) if IP is banned
     *
     * @param ip (should be byte[4]?) player IP
     *
     * @return not null response if banned
     */
    public BanRecord isIPBanned(byte[] ip) {
        return null;
    }
}
