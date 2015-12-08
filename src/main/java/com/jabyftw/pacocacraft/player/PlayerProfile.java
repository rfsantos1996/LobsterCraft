package com.jabyftw.pacocacraft.player;

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
public abstract class PlayerProfile extends BasePlayerProfile {

    protected final long playerId;

    /**
     * Create PlayerProfile that requires UserProfile's player identification number (in other words, every other profile)
     *
     * @param profileType profile's type
     * @param playerId    player's identification number on database
     */
    public PlayerProfile(ProfileType profileType, long playerId) {
        super(profileType);
        this.playerId = playerId;
    }

    public long getPlayerId() {
        return playerId;
    }
}
