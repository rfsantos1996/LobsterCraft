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

    LOGIN_PLAYERS_ALLOWED("login.max_number_of_players_joining", 16),
    LOGIN_PERIOD_OF_TIME("login.period_of_time_seconds", 10.0f),
    LOGIN_ALLOWED_COMMANDS("login.allowed_commands_before_login", new String[]{"login", "register", "l", "entrar", "registrar"}),
    LOGIN_TIME_BETWEEN_PROFILE_SAVES("login.time_between_profile_saves_seconds", 90),
    LOGIN_PROFILE_WAITING_TIME("login.profile_waiting_on_queue_seconds", 60 * 4),

    XRAY_TIME_TO_CONSIDER_SAME_MINE("xray.time_to_consider_same_mining_session_seconds", 60 * 4),
    XRAY_DISTANCE_TO_CONSIDER_SAME_MINE("xray.distance_to_consider_same_mining_session_blocks", 75.0d),
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

    public <T> T getValue() {
        return PacocaCraft.config.getValue(this);
    }
}
