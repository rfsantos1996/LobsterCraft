package com.jabyftw.easiercommands;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionType;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public abstract class Util {

    /**
     * Parse location from given string and world
     * e.g.: "3.321;32;34" on given world will return Location(world, 3.321, 32.0, 34.0)
     *
     * @param world  location's world (if not mentioned as first argument)
     * @param string string to be parsed
     * @return Bukkit's location based on string (null if failed)
     */
    public static Location parseToLocation(@Nullable World world, @NotNull String string) {
        String[] strings = string.split(";");
        double[] position = new double[3];

        // Not enough or too much arguments, return null
        if (strings.length < 3 || strings.length > 4) return null;

        // If strings have 4 arguments, world must be the first argument
        if (strings.length == 4) {
            world = parseToWorld(strings[0]);
            System.arraycopy(strings, 1, strings, 0, strings.length - 1);
        }

        // World is not defined
        if (world == null) return null;

        // Parse arguments as numbers, if a non-number is encountered, return null
        for (int index = 0; index < strings.length; index++) {
            if (!NumberUtils.isNumber(strings[index]))
                return null;
            else
                position[index] = Double.parseDouble(strings[index]);
        }

        // Return location
        return new Location(world, position[0], position[1], position[2]);
    }

    public static World parseToWorld(@NotNull String string) {
        World mostEqual = null;
        int equality = 2;

        for (World world : Bukkit.getServer().getWorlds()) {
            int equalityOfWords = equalityOfWords(world.getName(), string);

            if (equalityOfWords >= equality) {
                mostEqual = world;
                equality = equalityOfWords;
            }
        }

        return mostEqual;
    }

    public static Material parseToMaterial(@NotNull String string) {
        Material mostEqual = null;
        int equality = 2;

        for (Material material : Material.values()) {
            boolean useUnderline = string.contains("_");

            int equalityOfWords = equalityOfWords(material.name().replaceAll("_", (useUnderline ? "" : "_")), string);
            if (equalityOfWords >= equality) {
                mostEqual = material;
                equality = equalityOfWords;
            }
        }

        return mostEqual;
    }

    public static EntityType parseToEntityType(@NotNull String string) {
        EntityType mostEqual = null;
        int equality = 2;

        for (EntityType entityType : EntityType.values()) {
            boolean useUnderline = string.contains("_");

            int equalityOfWords = equalityOfWords(entityType.name().replaceAll("_", (useUnderline ? "" : "_")), string);
            if (equalityOfWords >= equality) {
                mostEqual = entityType;
                equality = equalityOfWords;
            }
        }

        return mostEqual;
    }

    public static GameMode parseToGameMode(@NotNull String string) {
        GameMode mostEqual = null;
        int equality = 2;

        for (GameMode gameMode : GameMode.values()) {
            int equalityOfWords = equalityOfWords(gameMode.name(), string);
            if (equalityOfWords >= equality) {
                mostEqual = gameMode;
                equality = equalityOfWords;
            }
        }

        return mostEqual;
    }

    public static Enchantment parseToEnchantmentType(String string) {
        Enchantment mostEqual = null;
        int equality = 2;

        for (Enchantment enchantment : Enchantment.values()) {
            boolean useUnderline = string.contains("_");

            int equalityOfWords = equalityOfWords(enchantment.getName().replaceAll("_", (useUnderline ? "" : "_")), string);
            if (equalityOfWords >= equality) {
                mostEqual = enchantment;
                equality = equalityOfWords;
            }
        }

        return mostEqual;
    }

    public static WeatherType parseToWeatherType(@NotNull String string) {
        WeatherType mostEqual = null;
        int equality = 2;

        for (WeatherType weatherType : WeatherType.values()) {
            boolean useUnderline = string.contains("_");

            int equalityOfWords = equalityOfWords(weatherType.name().replaceAll("_", (useUnderline ? "" : "_")), string);
            if (equalityOfWords >= equality) {
                mostEqual = weatherType;
                equality = equalityOfWords;
            }
        }

        return mostEqual;
    }

    public static PotionType parseToPotionType(@NotNull String string) {
        PotionType mostEqual = null;
        int equality = 2;

        for (PotionType material : PotionType.values()) {
            boolean useUnderline = string.contains("_");

            int equalityOfWords = equalityOfWords(material.name().replaceAll("_", (useUnderline ? "" : "_")), string);
            if (equalityOfWords >= equality) {
                mostEqual = material;
                equality = equalityOfWords;
            }
        }

        return mostEqual;
    }

    public static int equalityOfWords(@NotNull String sentence1, @NotNull String sentence2) {
        int equality = equalityOfWordsIgnoringLength(sentence1, sentence2),
                absoluteDifference = Math.abs(sentence1.length() - sentence2.length());

        return absoluteDifference > 0 ? equality - absoluteDifference : equality;
    }

    public static int equalityOfWordsIgnoringLength(@NotNull String sentence1, @NotNull String sentence2) {
        String[] words1 = sentence1.split(" "),
                words2 = sentence2.split(" ");
        int equality = 0;

        for (String wordsSentence1 : words1) {
            int equalityOfWord = 0;

            for (String worldSentence2 : words2) { // for all words
                int thisEq = equalityOfChars(wordsSentence1.toCharArray(), worldSentence2.toCharArray()); // compare each word

                if (thisEq > equalityOfWord)  // if world is equal than other word
                    equalityOfWord = thisEq;
            }

            equality += equalityOfWord; // add most equal word to the equality
        }

        return equality;
    }

    public static int equalityOfChars(char[] string1, char[] string2) {
        int equality = 0;

        for (int i = 0; i < Math.min(string1.length, string2.length); i++)
            equality += Character.toLowerCase(string1[i]) == Character.toLowerCase(string2[i]) ? 1 : -1;

        return equality;
    }

    /**
     * Source: Essentials (found through Ban-Management)
     * <b>Letters used:</b> y mo w d h m s
     *
     * @param time string with the time, eg: "3w4h" - three weeks and four hours
     * @return the time in milliseconds
     * @see <a href=https://github.com/BanManagement/BanManager/blob/master/src/main/java/me/confuser/banmanager/util/DateUtils.java>Credits to Essentials</a>
     */
    public static long parseTimeDifference(@NotNull String time) {
        Pattern timePattern = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = timePattern.matcher(time);

        int years = 0, months = 0, weeks = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
        boolean found = false;

        while (matcher.find()) {
            if (matcher.group() == null || matcher.group().isEmpty()) {
                continue;
            }

            for (int i = 0; i < matcher.groupCount(); i++) {
                if (matcher.group(i) != null && !matcher.group(i).isEmpty()) {
                    found = true;
                    break;
                }
            }

            if (found) {
                if (matcher.group(1) != null && !matcher.group(1).isEmpty())
                    years = Integer.parseInt(matcher.group(1));
                if (matcher.group(2) != null && !matcher.group(2).isEmpty())
                    months = Integer.parseInt(matcher.group(2));
                if (matcher.group(3) != null && !matcher.group(3).isEmpty())
                    weeks = Integer.parseInt(matcher.group(3));
                if (matcher.group(4) != null && !matcher.group(4).isEmpty())
                    days = Integer.parseInt(matcher.group(4));
                if (matcher.group(5) != null && !matcher.group(5).isEmpty())
                    hours = Integer.parseInt(matcher.group(5));
                if (matcher.group(6) != null && !matcher.group(6).isEmpty())
                    minutes = Integer.parseInt(matcher.group(6));
                if (matcher.group(7) != null && !matcher.group(7).isEmpty())
                    seconds = Integer.parseInt(matcher.group(7));
                break;
            }
        }

        if (!found)
            throw new IllegalArgumentException("Date can't be parsed");
        if (years > 20)
            throw new IllegalArgumentException("Date is too big");

        Calendar calendar = new GregorianCalendar();

        if (years > 0)
            calendar.add(Calendar.YEAR, years);
        if (months > 0)
            calendar.add(Calendar.MONTH, months);
        if (weeks > 0)
            calendar.add(Calendar.WEEK_OF_YEAR, weeks);
        if (days > 0)
            calendar.add(Calendar.DAY_OF_MONTH, days);
        if (hours > 0)
            calendar.add(Calendar.HOUR_OF_DAY, hours);
        if (minutes > 0)
            calendar.add(Calendar.MINUTE, minutes);
        if (seconds > 0)
            calendar.add(Calendar.SECOND, seconds);

        return calendar.getTimeInMillis() - System.currentTimeMillis();
    }
}
