package com.jabyftw.easiercommands;

import com.jabyftw.Util;
import com.jabyftw.pacocacraft.player.UserProfile;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionType;

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
public enum ArgumentType {

    LOCATION(Location.class),
    WORLD(World.class),

    TIME_DIFFERENCE(Long.class),
    //NUMBER(Number.class), // Invalid, you couldn't use other values than double o.o

    INTEGER(int.class),
    LONG(long.class),
    SHORT(short.class),
    BYTE(byte.class),
    FLOAT(float.class),
    DOUBLE(double.class),

    ENTITY_TYPE(EntityType.class),
    POTION_TYPE(PotionType.class),
    WEATHER_TYPE(WeatherType.class),
    MATERIAL(Material.class),

    PLAYER_NAME(UserProfile.class),

    PLAYER_IP(String.class),
    STRING(String.class);

    private final Class<?> clazz;

    ArgumentType(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public static Argument handleArgument(CommandSender commandSender, String string) {
        final Argument argument = new Argument(string);

        { // Check for location
            Location location = Util.parseToLocation(
                    commandSender instanceof Player ?
                            ((Player) commandSender).getWorld() :
                            null,
                    string
            );
            if(location != null)
                argument.addArgumentType(ArgumentType.LOCATION, location);
        }

        { // Check for world
            World world = Util.parseToWorld(string);
            if(world != null)
                argument.addArgumentType(ArgumentType.WORLD, world);
        }

        { // Check for materials
            Material material = Util.parseToMaterial(string);
            if(material != null)
                argument.addArgumentType(MATERIAL, material);
        }

        { // Check for entity types
            EntityType entityType = Util.parseToEntityType(string);
            if(entityType != null)
                argument.addArgumentType(ENTITY_TYPE, entityType);
        }

        { // Check for weather types
            WeatherType weatherType = Util.parseToWeatherType(string);
            if(weatherType != null)
                argument.addArgumentType(WEATHER_TYPE, weatherType);
        }

        { // Check for potion types
            PotionType potionType = Util.parseToPotionType(string);
            if(potionType != null)
                argument.addArgumentType(POTION_TYPE, potionType);
        }

        { // Check for player names
            UserProfile playerThatMatches = Util.getPlayerThatMatches(string);
            if(playerThatMatches != null)
                argument.addArgumentType(PLAYER_NAME, playerThatMatches);
        }

        { // Check for player IP
            for(Player player : Bukkit.getServer().getOnlinePlayers()) {
                if(player.getAddress().getAddress().getHostName().equalsIgnoreCase(string)) {
                    argument.addArgumentType(PLAYER_IP, argument);
                    break; // Don't repeat
                }
            }
        }

        { // Check for numbers
            if(NumberUtils.isNumber(string)) {
                try {
                    argument.addArgumentType(DOUBLE, Double.valueOf(string));
                } catch(NumberFormatException ignored) {
                }
                try {
                    argument.addArgumentType(FLOAT, Float.valueOf(string));
                } catch(NumberFormatException ignored) {
                }
                try {
                    argument.addArgumentType(INTEGER, Integer.valueOf(string));
                } catch(NumberFormatException ignored) {
                }
                try {
                    argument.addArgumentType(LONG, Long.valueOf(string));
                } catch(NumberFormatException ignored) {
                }
                try {
                    argument.addArgumentType(SHORT, Short.valueOf(string));
                } catch(NumberFormatException ignored) {
                }
                try {
                    argument.addArgumentType(BYTE, Byte.valueOf(string));
                } catch(NumberFormatException ignored) {
                }
            }
        }

        { // Check for date difference
            try {
                long timeDifference = Util.parseTimeDifference(string);
                argument.addArgumentType(TIME_DIFFERENCE, timeDifference);
            } catch(IllegalArgumentException ignored) {
            }
        }

        return argument;
    }
}
