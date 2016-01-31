package com.jabyftw;

import com.jabyftw.lobstercraft.util.Util;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.NoSuchAlgorithmException;

/**
 * Copyright (C) 2015  Rafael Sartori for LobsterCraft Plugin
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
public class StringLengthTest {

    public static void main(String[] args) {
        try {
            // Make it the range of the password
            for(int length = 4; length <= 26; length++) {
                String string = RandomStringUtils.random(length, true, true);
                String encryptedString = Util.encryptString(string);
                System.out.println("Encrypting a " + length + " string resulted on a " + encryptedString.length() + " encrypted length string: " + string + " -> " + encryptedString);
            }

            // Set variables for testing
            String IN = "N3VIg4dv1FO0LQuauHeuC6", PRE_ENCRYPTED_OUT = "4be9cc85bf5abc7cb99019a84256e938fbc4fbafa80598b297dc8bc3e22f11dd";

            // Testing if it is safe on every example
            String encryptionOut = Util.encryptString(IN);
            System.out.println("\nTesting safety: (" + encryptionOut + " == " + PRE_ENCRYPTED_OUT + " )? " + encryptionOut.equals(PRE_ENCRYPTED_OUT));
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // Test passed, every encrypted string is 64 letters length
    }
}
