package com.jabyftw.lobstercraft;

/**
 * Copyright (C) 2015  Rafael Sartori for LobsterCraft Plugin
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
public enum ConfigValue {

    MYSQL_REVISION("mysql.mysql_revision_DO_NOT_CHANGE", 1),
    MYSQL_JDBC_URL("mysql.jdbc_url", "jdbc:mysql://localhost:3306/database"),
    MYSQL_USERNAME("mysql.username", "root"),
    MYSQL_PASSWORD("mysql.password", "root"),
    MYSQL_POOL_SIZE("mysql.pool_size", 8), // the nice answer is 8
    MYSQL_CONNECTION_TIMEOUT("mysql.connection_timeout_seconds", 28800),

    LOGIN_PROFILE_SAVING_TIMEOUT("login.profile_saving.timeout_seconds", 60 * 4L), // 4 minutes to the profile be saved
    LOGIN_PROFILE_SAVING_PERIOD("login.profile_saving.period_ticks", 10 * 20L), // 10 seconds and all profiles are saved (in ticks)

    LOGIN_MESSAGE_PERIOD("login.login_message.period_ticks", 4 * 20L),
    LOGIN_MESSAGE_COMMAND_TIME("login.login_message.time_to_use_command_seconds", 60L + 30L),
    LOGIN_USE_FAST_MOVEMENT_EVENT_CHECK("login.use_fast_movement_event_check", false),
    LOGIN_PRE_SIGN_IN_COMMANDS("login.commands_allowed_to_execute_before_login", new String[]{"login", "register", "l", "entrar", "registrar", "signin", "sign", "reg"}),
    LOGIN_BLOCKED_PLAYER_NAMES("login.blocked_player_names", new String[]{"admin", "administrador", "moderador", "moderator"}),

    LOGIN_NAME_CHANGE_PLAYER_ALLOWED_TO_CHANGE("login.name_change.time_to_player_change_name_again_days", 10L),
    LOGIN_NAME_CHANGE_USERNAME_AVAILABLE("login.name_change.time_to_username_become_available_days", 15L),

    LOGIN_LIMITER_NUMBER_OF_PLAYERS("login.limiter.number_of_players", 16),
    LOGIN_LIMITER_PERIOD_OF_TIME("login.limiter.period_of_time_seconds", 10),

    PLAYER_SPEED_MINIMUM_MULTIPLIER("player.speed.minimum_speed_multiplier", 0.05d),
    PLAYER_SPEED_MAXIMUM_MULTIPLIER("player.speed.maximum_speed_multiplier", 6.0d),

    PLAYER_MAXIMUM_AMOUNT_OF_MUTES("player.chat.maximum_amount_of_personal_muted_players", 15),

    MONEY_MINIMUM_MONEY_POSSIBLE("money.minimum_possible_absolute", 2_250.0D),
    MONEY_PER_PLAYER_AMOUNT("money.server_per_player_amount", 75_000.0D),
    MONEY_PLAYER_STARTING_AMOUNT("money.player_starting_amount", 500.0D),

    MONEY_SERVER_CEIL_TAX("money.tax.server_ceil_tax", 0.40D),
    MONEY_SERVER_DEFAULT_TAX("money.tax.server_default_tax", 0.03D),
    MONEY_CITY_CEIL_TAX("money.tax.ceil_city_tax", 0.3D),
    MONEY_CITY_FLOOR_TAX("money.tax.floor_city_tax", 0.07D),
    MONEY_CITY_FLOOR_LOW_CAPACITY_TAX("money.tax.floor_city_tax_when_low_capacity", 0.16D),
    MONEY_CITY_ALARMING_CAPACITY_ECONOMY("money.tax.alarming_city_economy_capacity_trigger", 0.3D),
    //MONEY_("", ),

    CITY_STRUCTURE_BASE_PLAYER_NEEDED("city_structure.base_players_needed_for_level_up", 4),
    CITY_STRUCTURE_PLAYER_MULTIPLIER("city_structure.players_multiplier_needed_for_level_up", 3.0D / 2.0D), // On level 4 there will be 21 homes
    CITY_STRUCTURE_BASE_MONEY_NEEDED("city_structure.base_money_needed_for_level_up", 25_000.0D),
    CITY_STRUCTURE_MONEY_MULTIPLIER("city_structure.players_multiplier_needed_for_level_up", 5.0D / 4.0D), // On level 4 it'll cost 61 000

    WORLD_IGNORED_WORLDS("world.ignored_worlds", new String[]{"world_the_end", "world_nether"}),

    WORLD_PROTECTION_TOOL_HAND_MATERIAL("world.protection.tool_hand_material_for_protection_check", "STICK"),
    WORLD_PLAYER_BLOCK_SEARCH_DISTANCE("world.protection.player_block_search_distance_xyz", 12.0D),
    WORLD_TEMPORARY_PROTECTION_PROTECTED_DISTANCE("world.protection.temporary_protection.distance_for_block_check", 3.0D),
    WORLD_TEMPORARY_PROTECTION_PROTECTED_BLOCK_COUNT("world.protection.temporary_protection.block_count_for_protection", 8),
    WORLD_TEMPORARY_PROTECTION_TIME("world.protection.temporary_protection.time_temporary_block_is_kept_seconds", 60L * 4),
    WORLD_TEMPORARY_PROTECTION_TASK_PERIOD("world.protection.temporary_protection.process_block_task_period_seconds", 30L),
    WORLD_ADMINISTRATOR_BLOCK_SEARCH_DISTANCE("world.protection.administrator_block_search_distance_xz_only", 125.0D),

    WORLD_DEFAULT_LOAD_SIZE_MULTIPLIER("world.chunk_load.minimum_size_multiplier", 1.15d),
    WORLD_CHUNK_LOAD_PERIOD("world.chunk_load.period_between_loads_ticks", 3L), // as a first query, it took 30-90ms, so this will limit our scheduler to 150ms
    WORLD_CHUNK_LOAD_LIMIT("world.chunk_load.limit_of_chunks_per_load", 171), // 441 is loaded for a full request, 512 / 3 tasks will be 171 chunks/task
    WORLD_CHUNK_LOAD_NUMBER_OF_TASKS("world.chunk_load.number_of_tasks_running", 3),

    LOCATION_TELEPORT_TIME_WAITING("location.teleport.time_waiting_to_start_teleporting_ticks", 4 * 20L),
    LOCATION_TELEPORT_TIME_BETWEEN_ACCEPT_TRIGGERS("location.teleport.time_waiting_between_accept_and_teleport_ticks", 12L),

    XRAY_TIME_TO_CONSIDER_SAME_MINING("xray.time_to_consider_same_mining_session", 5 * 60L),
    XRAY_DISTANCE_TO_CONSIDER_SAME_MINING("xray.distance_to_consider_same_mining_session", 75.0D),;

    private final String path;
    private final Object defaultValue;

    <T> ConfigValue(String path, T defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return path;
    }
}
