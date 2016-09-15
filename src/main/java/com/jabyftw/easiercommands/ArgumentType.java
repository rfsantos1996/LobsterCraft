package com.jabyftw.easiercommands;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionType;

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
public enum ArgumentType {

    LOCATION(Location.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return Util.parseToLocation(
                    commandSender instanceof Player ? ((Player) commandSender).getWorld() : null,
                    string
            );
        }
    },
    WORLD(World.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return Util.parseToWorld(string);
        }
    },

    MATERIAL(Material.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return Util.parseToMaterial(string);
        }
    },
    ENTITY_TYPE(EntityType.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return Util.parseToEntityType(string);
        }
    },
    POTION_TYPE(PotionType.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return Util.parseToPotionType(string);
        }
    },
    WEATHER_TYPE(WeatherType.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return Util.parseToWeatherType(string);
        }
    },
    GAME_MODE_TYPE(GameMode.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return Util.parseToGameMode(string);
        }
    },
    ENCHANTMENT_TYPE(Enchantment.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return Util.parseToEnchantmentType(string);
        }
    },

    PLAYER_NAME(READ_ME.PLAYER_CLASS) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return READ_ME.getPlayerThatMatches(string);
        }
    },
    OFFLINE_PLAYER_NAME(READ_ME.OFFLINE_PLAYER_CLASS) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return READ_ME.getOfflinePlayerThatMatches(string);
        }
    },
    PLAYER_IP(String.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            for (Player player : Bukkit.getServer().getOnlinePlayers())
                if (player.getAddress().getAddress().getHostName().equalsIgnoreCase(string))
                    return string;
            return null;
        }
    },

    INTEGER(int.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            try {
                return Integer.valueOf(string);
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    },
    LONG(long.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            try {
                return Long.valueOf(string);
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    },
    SHORT(short.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            try {
                return Short.valueOf(string);
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    },
    BYTE(byte.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            try {
                return Byte.valueOf(string);
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    },
    FLOAT(float.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            try {
                return Float.valueOf(string);
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    },
    DOUBLE(double.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            try {
                return Integer.valueOf(string);
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    },

    TIME_DIFFERENCE(Long.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return Util.parseTimeDifference(string);
        }
    },

    STRING(String.class) {
        @Override
        protected Object isPossible(CommandSender commandSender, String string) {
            return string;
        }
    };

    private final Class<?> clazz;

    /**
     * Ordered by priority of lookup!
     *
     * @param clazz object class
     */
    ArgumentType(Class<?> clazz) {
        this.clazz = clazz;
    }

    public static Argument handleArgument(CommandSender commandSender, String string) {
        final Argument argument = new Argument();

        for (ArgumentType argumentType : ArgumentType.values()) {
            // Acquire argument type object for each type
            Object argumentTypeObject = null;
            try {
                argumentTypeObject = argumentType.isPossible(commandSender, string);
            } catch (Exception ignored) {
            }

            // Check if argument isn't null and add to the possible arguments
            if (argumentTypeObject != null)
                argument.addArgumentType(argumentType, argumentTypeObject);
        }

        return argument;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    protected abstract Object isPossible(CommandSender commandSender, String string);
}
