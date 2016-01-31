package com.jabyftw.lobstercraft.player.util;

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
public abstract class Permissions {

    public static final String
            JOIN_VANISHED = "lobstercraft.join.join_vanished",
            JOIN_FULL_SERVER = "lobstercraft.join.join_full_server",

    PROTECTION_ADMINISTRATOR_BUILD_MODE = "lobstercraft.protection.constructor_build_mode",
            PROTECTION_CREATE_ADMINISTRATOR_BUILDINGS = "lobstercraft.protection.create_buildings",

    PLAYER_GAMEMODE_CHANGE = "lobstercraft.gamemode",
            PLAYER_GAMEMODE_CHANGE_OTHERS = "lobstercraft.gamemode.others",

    LOCATION_TELEPORT_INSTANTANEOUSLY = "lobstercraft.location.teleport_instantly",
            LOCATION_TELEPORT_QUIETLY = "lobstercraft.location.teleport_quietly";
}
