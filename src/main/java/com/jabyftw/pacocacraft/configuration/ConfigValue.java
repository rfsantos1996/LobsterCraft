package com.jabyftw.pacocacraft.configuration;

import com.jabyftw.pacocacraft.PacocaCraft;

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
public enum ConfigValue {

    MYSQL_REVISION("mysql.mysql_revision_DO_NOT_CHANGE", 1),
    MYSQL_JDBC_URL("mysql.jdbc_url", "jdbc:mysql://localhost:3306/database"),
    MYSQL_USERNAME("mysql.username", "root"),
    MYSQL_PASSWORD("mysql.password", "root"),
    MYSQL_POOL_SIZE("mysql.pool_size", 8),
    MYSQL_CONNECTION_TIMEOUT("mysql.connection_timeout_minutes", 28800),

    LOGIN_JOIN_LIMITER_PLAYERS_ALLOWED("login.max_number_of_players_joining", 16),
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
    ;

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
}
