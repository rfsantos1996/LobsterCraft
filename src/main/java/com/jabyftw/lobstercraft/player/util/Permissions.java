package com.jabyftw.lobstercraft.player.util;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Email address: rafael.sartori96@gmail.com
 */
public abstract class Permissions {

    public static final String
            JOIN_VANISHED = "lobstercraft.join.join_vanished",
            JOIN_FULL_SERVER = "lobstercraft.join.join_full_server",
            JOIN_CHANGE_USERNAME = "lobstercraft.join.change_username",

    PROTECTION_ADMINISTRATOR_BUILD_MODE = "lobstercraft.protection.constructor_build_mode",
            PROTECTION_CREATE_ADMINISTRATOR_BUILDINGS = "lobstercraft.protection.create_buildings",

    CHAT_MUTE = "lobstercraft.chat.mute",
            CHAT_ADMIN_MUTE = "lobstercraft.chat.administrator_mute",
            CHAT_MUTE_EXCEPTION = "lobstercraft.chat.mute.exception",
            CHAT_WHISPER = "lobstercraft.chat.whisper",

    PLAYER_GAMEMODE_CHANGE = "lobstercraft.player.gamemode",
            PLAYER_GAMEMODE_CHANGE_OTHERS = "lobstercraft.player.gamemode.others",
            PLAYER_SPAWN_MOBS = "lobstercraft.player.spawn_mobs",
            PLAYER_SPAWN_MOBS_ON_OTHERS = "lobstercraft.player.spawn_mobs.on_others",
            PLAYER_GOD_MODE = "lobstercraft.player.god_mode",
            PLAYER_GOD_MODE_OTHERS = "lobstercraft.player.god_mode.others",
            PLAYER_EXP = "lobstercraft.player.exp",
            PLAYER_EXP_OTHERS = "lobstercraft.player.exp.to_others",
            PLAYER_HEAL = "lobstercraft.player.heal",
            PLAYER_HEAL_OTHERS = "lobstercraft.player.heal.to_others",
            PLAYER_LIST = "lobstercraft.player.list",
            PLAYER_LIST_HIDDEN = "lobstercraft.player.list.not_shown",
            PLAYER_CLEAR_ENCHANTMENT = "lobstercraft.player.clear_enchantment",
            PLAYER_HAT = "lobstercraft.player.hat",
            PLAYER_HAT_OTHERS = "lobstercraft.player.hat.others",
            PLAYER_SPEED = "lobstercraft.player.speed",
            PLAYER_SPEED_OTHERS = "lobstercraft.player.speed.others",
            PLAYER_KILL_ENTITIES = "lobstercraft.player.kill_all_entities",
            PLAYER_INVENTORY_SPY = "lobstercraft.player.inventory_spy",
            PLAYER_INVENTORY_SPY_MODIFY = "lobstercraft.player.inventory_spy.modify",
            PLAYER_LEVEL_CHANGE = "lobstercraft.player.level_change",
            PLAYER_LEVEL_CHANGE_OTHERS = "lobstercraft.player.level_change.others",
            PLAYER_KILL = "lobstercraft.player.player_kill",
            PLAYER_HUNGER_CHANGE = "lobstercraft.player.hunger_level",
            PLAYER_HUNGER_CHANGE_OTHERS = "lobstercraft.player.hunger_level.others",
            PLAYER_CLEAR_INVENTORY = "lobstercraft.player.clear_inventory",
            PLAYER_CLEAR_INVENTORY_OTHERS = "lobstercraft.player.clear_inventory.others",
            PLAYER_FLY = "lobstercraft.player.fly",
            PLAYER_FLY_OTHERS = "lobstercraft.player.fly.others",
            PLAYER_SUICIDE = "lobstercraft.player.suicide",
            PLAYER_GIVE = "lobstercraft.player.give",
            PLAYER_GIVE_OTHERS = "lobstercraft.player.give.others",
            PLAYER_PLAYER_WEATHER = "lobstercraft.player.player_weather",
            PLAYER_PLAYER_TIME = "lobstercraft.player.player_time",
            PLAYER_WORKBENCH = "lobstercraft.player.workbench",
            PLAYER_WORKBENCH_OTHERS = "lobstercraft.player.workbench.others",
            PLAYER_REPAIR = "lobstercraft.player.repair",
            PLAYER_PENDING_ITEMS = "lobstercraft.player.pending_items",
            PLAYER_ENCHANTMENT = "lobstercraft.player.enchantment",
            PLAYER_ENCHANTMENT_UNSAFE = "lobstercraft.player.enchantment.allow_unsafe",

    WORLD_CREATE_PORTALS = "lobstercraft.world.create_portals",

    LOCATION_TELEPORT_INSTANTANEOUSLY = "lobstercraft.location.teleport_instantly",
            LOCATION_TELEPORT_QUIETLY = "lobstercraft.location.teleport_quietly",
            LOCATION_TELEPORT = "lobstercraft.location.teleport",
            LOCATION_TELEPORT_TO_THE_TOP = "lobstercraft.location.teleport.to_top",
            LOCATION_TELEPORT_OTHERS_TO_THE_TOP = "lobstercraft.location.teleport.others_to_top",
            LOCATION_TELEPORT_BACK = "lobstercraft.location.teleport.last_location",
            LOCATION_TELEPORT_OTHERS = "lobstercraft.location.teleport.others",
            LOCATION_TELEPORT_TO_PLAYERS = "lobstercraft.location.teleport.to_players",
            LOCATION_SPAWN = "lobstercraft.location.spawn",
            LOCATION_SPAWN_OTHERS = "lobstercraft.location.spawn.teleport_others",
            LOCATION_CHANGE_WORLD = "lobstercraft.location.change_world",
            LOCATION_CHANGE_WORLD_OTHERS = "lobstercraft.location.change_world.others",
            LOCATION_SET_SPAWN = "lobstercraft.location.spawn.set_location";
}
