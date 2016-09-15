package com.jabyftw.lobstercraft;

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
public enum ConfigurationValues {

    /*
     * MySQL
     */
    MYSQL_REVISION("mysql.mysql_revision_DO_NOT_CHANGE", 1),
    MYSQL_JDBC_URL("mysql.mariadb_jdbc_url", "jdbc:mysql://localhost:3306/database"),
    MYSQL_USERNAME("mysql.username", "root"),
    MYSQL_PASSWORD("mysql.password", "root"),
    MYSQL_POOL_SIZE("mysql.pool_size", 8),
    MYSQL_CONNECTION_TIMEOUT_SECONDS("mysql.connection_timeout_seconds", 28800),

    /*
     * Login
     */
    LOGIN_LIMITER_PLAYERS_FOR_PERIOD("login.limiter.players_per_period", 16),
    LOGIN_LIMITER_PLAYERS_PERIOD_OF_TIME_SECONDS("login.limiter.time_period_seconds", 10),

    LOGIN_NAME_CHANGE_USERNAME_AVAILABLE_DAYS("login.name_change.time_to_username_become_available_for_everyone_days", 15L),
    LOGIN_NAME_CHANGE_PLAYER_ALLOWED_TO_CHANGE_DAYS("login.name_change.time_to_player_change_name_again_days", 10L),

    LOGIN_MESSAGE_PERIOD_BETWEEN_MESSAGES_TICKS("login.login_message.period_between_messages_ticks", 4L * 20L),
    LOGIN_MESSAGE_TIME_TO_LOGIN_SECONDS("login.login_message.time_to_login_seconds", 90L),

    LOGIN_USE_FAST_MOVEMENT_EVENT_CHECK("login.use_fast_movement_event_check", false),
    LOGIN_PRE_SIGN_IN_ALLOWED_COMMANDS("login.commands_allowed_to_execute_before_login", new String[]{"login", "register", "l", "entrar", "registrar", "signin", "sign", "reg"}),

    /*
     * Player
     */
    PLAYER_NAME_BLACKLIST("player.name_blacklist", new String[]{"admin", "moderator", "moderador", "policial", "policia", "administrador", "penis", "pinto", "vagina", "buceta", "boceta"}),
    PLAYER_DEFAULT_MONEY_AMOUNT("player.default_money_amount", 500.0D),
    PLAYER_TIME_PROFILE_KEPT_SECONDS("player.period_profile_is_kept_on_memory_seconds", 60 * 7L),
    PLAYER_TIME_BETWEEN_PROFILE_SAVES_TICKS("player.period_between_profile_saves_ticks", 20L * 30), // 30 seconds

    PLAYER_TIME_WAITING_BEFORE_TELEPORTING_TICKS("player.teleport.time_waiting_to_start_teleporting_ticks", 4 * 20L),
    PLAYER_TELEPORT_TIME_BETWEEN_ACCEPT_TRIGGERS_TICKS("player.teleport.time_waiting_between_accept_and_teleport_ticks", 12L),

    /*
     * World
     * If WORLD_HANDLING is true, WORLD_LIST will be the list of handled worlds. If false, WORLD_LIST will be ignored worlds
     */
    WORLD_LIST("world.world_list", new String[]{"world"}),
    WORLD_HANDLING("world.world_list_should_be_handled", true),

    WORLD_PROTECTION_PLAYER_DISTANCE("world.protection.player.protection_distance", 13.0D),
    WORLD_PROTECTION_PLAYER_CHECK_Y("world.protection.player.protection_check_y_axis", true),
    WORLD_PROTECTION_PLAYER_NEAR_BLOCKS_SEARCH_RADIUS("world.protection.player.maximum_radius_for_near_protected_blocks_search", 1.5D),
    WORLD_PROTECTION_PLAYER_REQUIRED_NEAR_BLOCKS_AMOUNT("world.protection.player.minimum_protected_blocks_for_protection", 7),

    WORLD_PROTECTION_PLAYER_LOADER_RANGE_MULTIPLIER("world.protection.player.chunk_loader.range_multiplier", 1.15D),
    WORLD_PROTECTION_PLAYER_LOADER_THREADS("world.protection.player.chunk_loader.amount_of_threads", 3),
    WORLD_PROTECTION_PLAYER_LOADER_CHUNKS_PER_RUN("world.protection.player.chunk_loader.amount_of_chunks_per_run", 180),
    WORLD_PROTECTION_PLAYER_LOADER_PERIOD_TICKS("world.protection.player.chunk_loader.period_ticks", 5),

    WORLD_PROTECTION_PLAYER_UNLOADER_CHUNKS_PER_RUN("world.protection.player.chunk_unloader.number_of_chunks_per_run", 150),
    WORLD_PROTECTION_PLAYER_UNLOADER_PERIOD_TICKS("world.protection.player.chunk_unloader.period_ticks", 3),
    WORLD_PROTECTION_PLAYER_UNLOADER_BLOCKS_RESTORED("world.protection.player.chunk_unloader.blocks_restored", 90),

    WORLD_PROTECTION_ADMIN_DISTANCE("world.protection.admin.protection_distance", 150.0D),
    WORLD_PROTECTION_ADMIN_CHECK_Y("world.protection.admin.protection_check_y_axis", false),

    WORLD_PROTECTION_CITY_MAX_DISTANCE_FROM_CENTER("world.protection.city.protection_distance", 125.0D),
    WORLD_PROTECTION_CITY_CHECK_Y("world.protection.city.protection_check_y_axis", false),

    /*
     * City
     */
    CITY_MINIMUM_DISTANCE_BETWEEN_CITIES("city.minimum_distance_between_cities", 150.0D),
    CITY_MAXIMUM_DISTANCE_BETWEEN_CITIES("city.maximum_distance_between_cities", 1_500.0D),
    CITY_CREATION_COST("city.creation_cost", 7_500.0D),
    CITY_BUILDER_RATIO("city.builders_players_maximum_ratio", 0.30D), // for 21 players, it'll result in 7 builders,
    CITY_HOUSE_PROTECTION_DISTANCE("city.house_protection_radius", 13.0D), // on the minimum radius (60), you "can" fit 20 houses (one is the center)

    CITY_MAXIMUM_TAX("city.taxes.maximum_tax_rate", 0.35D),
    CITY_MINIMUM_TAX("city.taxes.minimum_tax_rate", 0.05D),
    CITY_SERVER_TAX_FEE("city.taxes.server_tax_fee", 0.15D),
    CITY_SERVER_TAX_PERIOD_DAYS("city.taxes.days_between_tax_recovery", 14),

    CITY_LEVELING_INITIAL_COST("city.leveling_initial_cost", 10_000.0D),
    CITY_LEVELING_COST_MULTIPLIER("city.leveling_cost_multiplier", 1.75D), // level 9->10 will cost 879.638,82

    CITY_LEVELING_INITIAL_RANGE("city.leveling.initial_protection_range", 60.0D),
    CITY_LEVELING_RANGE_PER_LEVEL("city.leveling.range_per_level", 7.10D), // 124 in total

    CITY_LEVELING_INITIAL_PLAYER_AMOUNT("city.leveling.initial_player_amount", 3),
    CITY_LEVELING_PLAYER_PER_LEVEL("city.leveling.player_per_level", 2), // 21 in total

    CITY_LEVELING_INITIAL_STORE_ITEMS("city.leveling.initial_store_items_amount", 4),
    CITY_LEVELING_STORE_ITEMS_PER_LEVEL("city.leveling.store_items_per_level", 3.50D), // 36 in total
    CITY_LEVELING_MAXIMUM_AMOUNT_OF_ITEM_STORE("city.leveling.maximum_amount_of_items_on_store", 36),

    CITY_LEVELING_INITIAL_INVENTORY_ITEMS("city.leveling.initial_inventory_items_amount", 6),
    CITY_LEVELING_INVENTORY_ITEMS_PER_LEVEL("city.leveling.inventory_items_per_level", 5.30D), // 54 in total
    CITY_LEVELING_MAXIMUM_AMOUNT_OF_ITEM_INVENTORY("city.leveling.maximum_amount_of_items_on_inventory", 54),

    XRAY_TIME_TO_CONSIDER_SAME_MINING_SECONDS("xray.time_to_consider_same_mining_session_seconds", 5 * 60L),
    XRAY_DISTANCE_CONSIDER_SAME_MINE("xray.distance_to_consider_same_mining_session", 50.0D);

    private final String string;
    private final Object defaultValue;

    ConfigurationValues(final String string, final Object defaultValue) {
        this.string = string;
        this.defaultValue = defaultValue;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return string;
    }
}
