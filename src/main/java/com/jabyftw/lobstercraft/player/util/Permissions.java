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
public enum Permissions {

    JOIN_FULL_SERVER("join", "full_server"),
    JOIN_VANISHED("join", "vanished"),
    JOIN_CHANGE_USERNAME("join", "change_username"),

    BAN_EXCEPTION("ban", "player_is_exception"),
    BAN_SEE_HISTORY("ban", "check_player_history"),
    BAN_PLAYER_PERMANENTLY("ban", "ban_permanently_player"),
    BAN_PLAYER_TEMPORARILY("ban", "ban_temporarily_player"),
    BAN_PLAYER_KICK("ban", "kick_player"),

    PLAYER_SAFE_PLAYER("player", "is_safe_player"),
    PLAYER_CLEAR_ENCHANTMENT("player", "clear_enchantment"),
    PLAYER_CLEAR_INVENTORY("player", "clear_inventory"),
    PLAYER_CLEAR_INVENTORY_OTHERS("player", "clear_inventory", "others"),
    PLAYER_ENCHANTMENT("player", "enchantment"),
    PLAYER_ENCHANTMENT_UNSAFE("player", "enchantment", "unsafe"),
    PLAYER_EXP("player", "exp"),
    PLAYER_EXP_OTHERS("player", "exp", "others"),
    PLAYER_HUNGER("player", "hunger_level"),
    PLAYER_HUNGER_OTHERS("player", "hunger_level", "others"),
    PLAYER_FLY("player", "fly"),
    PLAYER_FLY_OTHERS("player", "fly", "others"),
    PLAYER_GAME_MODE("player", "game_mode"),
    PLAYER_GAME_MODE_OTHERS("player", "game_mode", "others"),
    PLAYER_GIVE("player", "give"),
    PLAYER_GIVE_OTHERS("player", "give", "others"),
    PLAYER_GOD_MODE("player", "god_mode"),
    PLAYER_GOD_MODE_OTHERS("player", "god_mode", "others"),
    PLAYER_HAT("player", "hat"),
    PLAYER_HAT_OTHERS("player", "hat", "others"),
    PLAYER_HEAL("player", "heal"),
    PLAYER_HEAL_OTHERS("player", "heal", "others"),
    PLAYER_INVENTORY_SPY("player", "inventory_spy"),
    PLAYER_INVENTORY_SPY_MODIFY("player", "inventory_spy", "modify"),
    PLAYER_KILL("player", "kill_players"),
    PLAYER_KILL_ENTITIES("player", "kill_all_entities"),
    PLAYER_LEVEL_CHANGE("player", "level_change"),
    PLAYER_LEVEL_CHANGE_OTHERS("player", "level_change", "others"),
    PLAYER_LIST("player", "list"),
    PLAYER_LIST_HIDDEN("player", "list", "see_hidden"),
    PLAYER_PENDING_ITEMS("player", "pending_items"),
    PLAYER_PLAYER_TIME("player", "player_time"),
    PLAYER_PLAYER_WEATHER("player", "player_weather"),
    PLAYER_REPAIR("player", "repair"),
    PLAYER_SPAWN_MOBS("player", "spawn_mobs"),
    PLAYER_SPAWN_MOBS_OTHERS("player", "spawn_mobs", "others"),
    PLAYER_SPEED("player", "speed"),
    PLAYER_SPEED_OTHERS("player", "speed", "others"),
    PLAYER_SUICIDE("player", "suicide"),
    PLAYER_WORKBENCH("player", "workbench"),
    PLAYER_WORKBENCH_OTHERS("player", "workbench", "others"),

    LOCATION_TELEPORT_INSTANTANEOUSLY("location", "teleport", "instantly"),
    LOCATION_TELEPORT_QUIETLY("location", "teleport", "quietly"),
    LOCATION_TELEPORT_BACK("location", "teleport", "back"),
    LOCATION_TELEPORT_HERE("location", "teleport", "here"),
    LOCATION_TELEPORT_SAVE_LAST_LOCATION("location", "teleport", "save_last_location"),
    LOCATION_TELEPORT_TO_PLAYER("location", "teleport_to_player"),
    LOCATION_TELEPORT_TO_PLAYER_OTHERS("location", "teleport_to_player", "others"),
    LOCATION_TELEPORT_TO_LOCATION("location", "teleport_to_location"),
    LOCATION_TELEPORT_TO_LOCATION_OTHERS("location", "teleport_to_location", "others"),
    LOCATION_TELEPORT_TO_SPAWN("location", "teleport_to_spawn"),
    LOCATION_TELEPORT_TO_SPAWN_OTHERS("location", "teleport_to_spawn", "others"),
    LOCATION_TELEPORT_TO_TOP("location", "teleport_to_the_top"),
    LOCATION_TELEPORT_TO_TOP_OTHERS("location", "teleport_to_the_top", "others"),
    LOCATION_CHANGE_WORLD("location", "change_world"),
    LOCATION_CHANGE_WORLD_SPAWN("location", "change_world_spawn"),
    LOCATION_CHANGE_WORLD_OTHERS("location", "change_world", "others"),

    WORLD_CREATE_PORTALS("world", "create_portals"),
    WORLD_PROTECTION_USE_TOOL("world", "protection", "use_tool"),
    WORLD_PROTECTION_IGNORE_PLAYER_BLOCKS("world", "protection", "ignore_player_blocks"),
    WORLD_PROTECTION_IGNORE_CITY_HOUSES_BLOCKS("world", "protection", "ignore_city_houses_blocks"),
    WORLD_PROTECTION_IGNORE_ADMIN_BLOCKS("world", "protection", "ignore_administrator_blocks"),
    WORLD_PROTECTION_IGNORE_CITY_STRUCTURES("world", "protection", "ignore_city_structures"),

    EXIT_COMMAND("exit_command");

    private static final String PREFIX = "lobstercraft.";
    private final String permissionNode;

    Permissions(String... nodes) {
        StringBuilder builder = new StringBuilder(PREFIX);
        // Create final node => prefix + (dot + nodes)
        for (String node : nodes)
            builder.append('.').append(node);
        // Finish string build
        permissionNode = builder.toString();
    }

    @Override
    public String toString() {
        return permissionNode;
    }
}
