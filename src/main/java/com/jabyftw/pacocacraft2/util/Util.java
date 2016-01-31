package com.jabyftw.pacocacraft2.util;

import com.jabyftw.pacocacraft2.PacocaCraft;
import com.jabyftw.pacocacraft2.profile_util.PlayerHandler;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionType;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public abstract class Util {

    private final static double minecraftTime_x = 1.0d / 36d;
    private final static long minecraftTime_y = 600L;

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

    public static int getEqualityOfNames(@NotNull char[] firstWord, @NotNull char[] secondWord) {
        if (firstWord.length > secondWord.length)  // do not accept search being bigger than player name. Jaby (4) < (5) Jaby2
            return 0;

        int equality = equalityOfChars(firstWord, secondWord);
        return secondWord.length > firstWord.length ? equality - (secondWord.length - firstWord.length) : equality;
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

    public static PlayerHandler getPlayerThatMatches(@NotNull String string) {
        if (string.length() < 3)
            return null;

        PlayerHandler mostEqual = null;
        int equalSize = 3;

        for (PlayerHandler online : PacocaCraft.playerMap.values()) {
            int thisSize = getEqualityOfNames(string.toCharArray(), online.getPlayer().getName().toCharArray());

            if (thisSize >= equalSize) {
                mostEqual = online;
                equalSize = thisSize;
            }
        }

        return mostEqual != null ? mostEqual : null;
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

    /**
     * Check String for a-z, A-Z, _ and 0-9 with given boundary
     *
     * @param string    string to be checked
     * @param minLength minimum length of the string
     * @param maxLength maximum length of the string
     * @return true for valid strings
     */
    public static boolean checkStringCharactersAndLength(@NotNull String string, int minLength, int maxLength) {
        return string.matches("[A-Za-z_0-9]{" + minLength + "," + maxLength + "}");
    }

    /**
     * CHeck String for its length
     *
     * @param string    string to be tested
     * @param minLength minimum length
     * @param maxLength maximum length
     * @return true if password passes
     */
    public static boolean checkStringLength(@NotNull String string, int minLength, int maxLength) {
        return string.length() >= minLength && string.length() <= maxLength;
    }

    /**
     * Parse time to string given millisecond-stored time
     *
     * @param timeInMillis time in milliseconds
     * @param dateFormat   SimpleDateFormat compatible format
     * @return string matching the desired date format
     */
    public static String parseTimeInMillis(long timeInMillis, @NotNull String dateFormat) {
        Date date = new Date();
        date.setTime(timeInMillis);
        return new SimpleDateFormat(dateFormat).format(date);

    }

    /**
     * Parse time to string given millisecond time
     *
     * @param timeInMillis time in milliseconds
     * @return string matching 0h00m00s format
     */
    public static String parseTimeInMillis(long timeInMillis) {
        return String.format(
                "%dh" + "%02dm" + "%02ds", // 0h00m00s
                TimeUnit.MILLISECONDS.toHours(timeInMillis),
                TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % TimeUnit.MINUTES.toSeconds(1)
        );
    }

    /**
     * Encrypt string using SHA-256
     *
     * @param string string to be encrypted
     * @return encrypted string acquired from encrypted bytes
     * @throws NoSuchAlgorithmException in case of encryption algorithm wasn't found
     * @see <a href="https://github.com/Xephi/AuthMeReloaded/blob/master/src/main/java/fr/xephi/authme/security/crypts/SHA256.java">AuthMe Reloaded</a>
     */
    public static String encryptString(@NotNull String string) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.reset();
        sha256.update(string.getBytes());
        byte[] digest = sha256.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }

    /**
     * 0 -> 6h00
     * 18.000 -> 0h00
     * <p>
     * Time in seconds: t
     * Minecraft time: m
     * <p>
     * m = t.x + y, using these values we have that x = 1/36 and y = 600
     *
     * @param earthTimeInSeconds time parsed as seconds
     * @return Minecraft's time
     * @see Util#parseTimeDifference(String) will return time in millis, parse it to seconds
     */
    public static long getMinecraftTime(long earthTimeInSeconds) {
        return (long) (earthTimeInSeconds * (1.0d / 36.0d)) + 600L;
    }

    public static void sendPlayerMessage(@NotNull PlayerHandler playerHandler, @NotNull final String message) {
        playerHandler.getProfile(ChatProfile.class).sendServerMessage(message);
    }

    /**
     * Send an message to a command sender. If it is a player, it'll send it through the ChatProfile; normal message, otherwise
     *
     * @param commandSender given player
     * @return player's chat profile
     */
    public static void sendCommandSenderMessage(@NotNull final CommandSender commandSender, @NotNull final String message) {
        if (commandSender instanceof Player)
            sendPlayerMessage(PacocaCraft.getPlayerHandler((Player) commandSender), message);
        else
            commandSender.sendMessage(message);
    }
}
