package com.jabyftw.lobstercraft.util;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.Location;
import org.bukkit.util.NumberConversions;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
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

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm");
    private final static DecimalFormat
            taxFormat = new DecimalFormat("##0'%"),
            moneyFormat = new DecimalFormat("'$0.00"),
            decimalFormat = new DecimalFormat("0.000");

    /**
     * The number of chunks loaded is square((chunkSize * 2) + 1):
     * '3' will give 3*2+1 or 7 that will return 49 chunks loaded
     *
     * @param searchRange the search range
     * @return the number of chunks to be loaded
     * @see Util#getMinimumChunkSize(double) about chunk size
     */
    public static int getNumberOfChunksLoaded(int searchRange) {
        int i = searchRange * 2 + 1;
        return i * i; // always equals -1, hihi
    }

    /**
     * Retrieve our chunk range number for given search distance.
     *
     * @param searchBlockDistance block's search distance
     * @return our chunk range
     * @see Util#getMinimumChunkSize(double) for more information about number of chunks loaded
     */
    public static int getMinimumChunkSize(double searchBlockDistance) {
        return NumberConversions.ceil(searchBlockDistance / 16D);
    }

    /**
     * Get an array of strings and return it without an index
     *
     * @param indexRemoved source's index to be removed
     * @param array        source
     * @return array of Strings without the removed one
     */
    public static String[] removeIndexFromString(int indexRemoved, String... array) {
        String[] strings = new String[array.length - 1];

        // Iterate through all items
        for (int i = 0; i < array.length; i++)
            // If it isn't the removed one
            if (i != indexRemoved)
                // Set our final String on current index, remembering to remove one when i > indexRemoved
                strings[i + (i > indexRemoved ? -1 : 0)] = array[i];

        return strings;
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
     * Check String for a-z, A-Z, _ and 0-9 with given boundary
     *
     * @param string    string to be checked
     * @param minLength minimum length of the string
     * @param maxLength maximum length of the string
     * @return true for valid strings
     */
    public static boolean checkStringCharactersAndLength(@Nullable String string, int minLength, int maxLength) {
        return string != null && string.matches("[A-Za-z_0-9]{" + minLength + "," + maxLength + "}");
    }

    /**
     * CHeck String for its length
     *
     * @param string    string to be tested
     * @param minLength minimum length
     * @param maxLength maximum length
     * @return true if password passes
     */
    public static boolean checkStringLength(@Nullable String string, int minLength, int maxLength) {
        return string != null && string.length() >= minLength && string.length() <= maxLength;
    }

    /**
     * Smartely search for the equal name comparing to the string
     *
     * @param string almost equal player's name
     * @return PlayerHandler that corresponds to the most equal given player name
     */
    public static PlayerHandler getPlayerThatMatches(@NotNull String string) {
        if (string.length() < 3)
            return null;

        PlayerHandler mostEqual = null;
        int equalSize = 3;

        for (PlayerHandler online : LobsterCraft.playerHandlerService.getOnlinePlayers()) {
            int thisSize = getEqualityOfNames(string.toCharArray(), online.getPlayer().getName().toCharArray());

            if (thisSize >= equalSize) {
                mostEqual = online;
                equalSize = thisSize;
            }
        }

        return mostEqual != null ? mostEqual : null;
    }

    /**
     * Smartely checks for player names. Order of arguments is important here.
     *
     * @param nameSearched the string that the user searched for
     * @param realName     player's actual name to check
     * @return the number representing the equality of both sentences
     * @see Util#equalityOfChars(char[], char[]) for its integer value
     */
    public static int getEqualityOfNames(@NotNull char[] nameSearched, @NotNull char[] realName) {
        if (nameSearched.length > realName.length)  // do not accept search being bigger than player name. Jaby (4) < (5) Jaby2
            return 0;

        int equality = equalityOfChars(nameSearched, realName);
        return realName.length > nameSearched.length ? equality - (realName.length - nameSearched.length) : equality;
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

    /**
     * Returns a number that represents a value of identity between two array of characters
     * This will return in a range of -length to +length, being length the lowest length between both arrays
     *
     * @param string1 array of characters to compare
     * @param string2 array of characters to compare
     * @return a number between -length and +length of the shortest array
     */
    public static int equalityOfChars(char[] string1, char[] string2) {
        int equality = 0;

        for (int i = 0; i < Math.min(string1.length, string2.length); i++)
            equality += Character.toLowerCase(string1[i]) == Character.toLowerCase(string2[i]) ? 1 : -1;

        return equality;
    }

    /**
     * Get the Minecraft's time based on earth time in seconds, given by the equation:
     * (earthTimeSeconds / 36) + 600
     * <p>
     * 4.000 -> 10h00
     * 8.000 -> 14h00
     * <p>
     * Time in seconds: t
     * Minecraft time: m
     * <p>
     * m = t.x + y, using these values we have that x = 0.277... (5 / 18) and y = 6.000
     *
     * @param earthTimeInSeconds time parsed as seconds
     * @return Minecraft's time
     */
    public static long getMinecraftTime(long earthTimeInSeconds) {
        return (long) (earthTimeInSeconds * 5.0d / 18.0d) - 6000L;
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
            if (matcher.group() == null || matcher.group().isEmpty())
                continue;

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

    public static String locationToString(@NotNull final Location location) {
        return "x=" + location.getBlockX() + ", " +
                "y=" + location.getBlockY() + ", " +
                "z=" + location.getBlockZ() + ", " +
                "world=" + location.getWorld().getName();
    }

    public static String retrieveMessage(String... strings) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;

        // Append all words
        for (String string : strings) {
            if (!first) stringBuilder.append(' ');
            first = false;
            stringBuilder.append(string);
        }

        // Return final string
        return stringBuilder.toString();
    }

    public static String formatDate(long timeMillis) {
        return dateFormat.format(new Date(timeMillis));
    }

    public static String formatDecimal(double decimal) {
        return decimalFormat.format(decimal);
    }

    public static String formatMoney(double decimal) {
        return moneyFormat.format(decimal);
    }

    public static String formatTaxes(double taxFee) {
        return taxFormat.format(taxFee);
    }
}
