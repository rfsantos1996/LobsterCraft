package com.jabyftw.pacocacraft.login.ban;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.util.ServerService;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final ConcurrentHashMap<Long, ArrayList<BanRecord>> cachedPlayerRecords = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        // TODO Cache valid (permanent, temporary bans that are not 'finished') MySQL bans
        PacocaCraft.logger.info("Enabled " + getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
        // TODO Save modified (new) bans on ban database
    }

    /**
     * Check on database (cache) if player name is banned
     *
     * @param playerName player's name for lookup
     *
     * @return not null response if banned
     */
    public static BanRecord isPlayerBanned(String playerName) {
        return null;
    }

    /*
     * Check on database (cache) if IP is banned
     *
     * @param ip (should be byte[4]) player IP
     *
     * @return not null response if banned
     *
    public static BanRecord isIPBanned(byte[] ip) {
        return null;
    }*/
}
