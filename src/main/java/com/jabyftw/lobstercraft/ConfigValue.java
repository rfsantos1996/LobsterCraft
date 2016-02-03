package com.jabyftw.lobstercraft;

/**
 * Copyright (C) 2015  Rafael Sartori for LobsterCraft Plugin
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
public enum ConfigValue {

    MYSQL_REVISION("mysql.mysql_revision_DO_NOT_CHANGE", 1),
    MYSQL_JDBC_URL("mysql.jdbc_url", "jdbc:mysql://localhost:3306/database"),
    MYSQL_USERNAME("mysql.username", "root"),
    MYSQL_PASSWORD("mysql.password", "root"),
    MYSQL_POOL_SIZE("mysql.pool_size", 6), // the nice answer is 8
    MYSQL_CONNECTION_TIMEOUT("mysql.connection_timeout_seconds", 28800),

    LOGIN_PROFILE_SAVING_TIMEOUT("login.profile_saving.timeout_seconds", 60 * 4L), // 4 minutes to the profile be saved
    LOGIN_PROFILE_SAVING_PERIOD("login.profile_saving.period_ticks", 10 * 20L), // 10 seconds and all profiles are saved (in ticks)

    LOGIN_USE_FAST_MOVEMENT_EVENT_CHECK("login.use_fast_movement_event_check", false),
    LOGIN_PRE_SIGN_IN_COMMANDS("login.commands_allowed_to_execute_before_login", new String[]{"login", "register", "l", "entrar", "registrar", "signin", "sign", "reg"}),

    LOGIN_LIMITER_NUMBER_OF_PLAYERS("login.limiter.number_of_players", 16),
    LOGIN_LIMITER_PERIOD_OF_TIME("login.limiter.period_of_time_seconds", 10),

    PLAYER_SPEED_MINIMUM_MULTIPLIER("player.speed.minimum_speed_multiplier", 0.05d),
    PLAYER_SPEED_MAXIMUM_MULTIPLIER("player.speed.maximum_speed_multiplier", 6.0d),

    WORLD_IGNORED_WORLDS("world.ignored_worlds", new String[]{"world_the_end", "world_nether"}),

    WORLD_PLAYER_BLOCK_SEARCH_DISTANCE("world.protection.player_block_search_distance_xyz", 12.0D),
    WORLD_ADMINISTRATOR_BLOCK_SEARCH_DISTANCE("world.protection.administrator_block_search_distance_xz_only", 125.0D),

    WORLD_DEFAULT_LOAD_SIZE_MULTIPLIER("world.chunk_load.minimum_size_multiplier", 1.15d),
    WORLD_CHUNK_LOAD_PERIOD("world.chunk_load.period_between_loads_ticks", 7L), // as a first query, it took 30-90ms, so this will limit our scheduler to 200ms
    WORLD_CHUNK_LOAD_LIMIT("world.chunk_load.limit_of_chunks_per_load", 500), // 441 is loaded for a full request

    LOCATION_TELEPORT_TIME_WAITING("location.teleport.time_waiting_to_start_teleporting_ticks", 4 * 20L),
    LOCATION_TELEPORT_TIME_BETWEEN_ACCEPT_TRIGGERS("location.teleport.time_waiting_between_accept_and_teleport_ticks", 12L),

    XRAY_TIME_TO_CONSIDER_SAME_MINING("xray.time_to_consider_same_mining_session", 5 * 60L),
    XRAY_DISTANCE_TO_CONSIDER_SAME_MINING("xray.distance_to_consider_same_mining_session", 75.0D),;

    /*LOGIN_JOIN_LIMITER_PLAYERS_ALLOWED("login.max_number_of_players_joining", 16),
    LOGIN_JOIN_LIMITER_PERIOD_OF_TIME("login.period_of_time_seconds", 10.0d),
    LOGIN_BEFORE_LOGIN_ALLOWED_COMMANDS("login.allowed_commands_before_login", new String[]{"login", "register", "l", "entrar", "registrar"}),
    LOGIN_TIME_BETWEEN_PROFILE_SAVES("login.time_between_profile_saves_seconds", 30L),
    LOGIN_PROFILE_WAITING_TIME("login.profile_waiting_on_queue_seconds", 60 * 4L),
    LOGIN_REQUIRED_TIME_CHANGE_USERNAME("login.required_time_to_change_username_days", 10),
    LOGIN_REQUIRED_TIME_USERNAME_AVAILABLE("login.required_time_to_username_be_available_days", 20),
    LOGIN_FAST_MOVEMENT_CHECK("login.fast_movement_check", false),

    XRAY_TIME_TO_CONSIDER_SAME_MINE("xray.time_to_consider_same_mining_session_seconds", 60 * 4L),
    XRAY_DISTANCE_TO_CONSIDER_SAME_MINE("xray.distance_to_consider_same_mining_session_blocks", 75.0d),

    TELEPORT_TIME_WAITING("teleport.time_waiting_seconds", 4L),
    TELEPORT_REQUEST_TIMEOUT("teleport.request_timeout_seconds", 60 * 2L),
    TELEPORT_TIME_ACCEPT_TELEPORT("teleport.time_between_accept_and_teleport_ticks", 12L),

    PLAYER_TIME_CLEAR_INVENTORY_CONFIRMATION("player.time_to_confirm_clear_inventory_seconds", 45L),
    PLAYER_TIME_SUICIDE_CONFIRMATION("player.time_to_confirm_suicide_seconds", 45L),
    PLAYER_TIME_UNSAFE_ENCHANTMENT_CONFIRMATION("player.time_to_confirm_unsafe_enchantment_seconds", 30L),
    PLAYER_SPEED_MINIMUM_MULTIPLIER("player.speed.minimum_speed_multiplier", 0.05d),
    PLAYER_SPEED_MAXIMUM_MULTIPLIER("player.speed.maximum_speed_multiplier", 6.0d),
    PLAYER_FIXED_CHAT_REFRESH_DELAY("player.fixed_chat_refresh_delay_seconds", 7L),
    PLAYER_MAXIMUM_MUTED_PLAYERS("player.maximum_number_of_muted_players", 15),

    BLOCK_IGNORED_WORLDS("block.ignored_worlds", new String[]{"world_the_end", "world_nether"}),
    BLOCK_LOAD_CHUNK_RANGE("block.load_chunk_range", 3),
    BLOCK_LOAD_CHUNK_PERIOD("block.load_task_period_ticks", 20L),
    BLOCK_LOAD_CHUNK_LIMIT_PER_RUN("block.load_task_max_number_of_chunks", 3 * 7 * 7), // the number of chunks in a range of 3 is 7*7 (49) so, 3*49 will be our limit*/

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
