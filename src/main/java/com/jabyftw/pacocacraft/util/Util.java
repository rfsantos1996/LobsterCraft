package com.jabyftw.pacocacraft.util;

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
public final class Util {

    /**
     * Check String for a-z, A-Z, _ and 0-9 with given boundary
     *
     * @param string    string to be checked
     * @param minLength minimum length of the string
     * @param maxLength maximum length of the string
     *
     * @return true for valid strings
     */
    public static boolean checkString(String string, int minLength, int maxLength) {
        return string.matches("[A-Za-z_0-9]{" + minLength + "," + maxLength + "}");
    }
}
