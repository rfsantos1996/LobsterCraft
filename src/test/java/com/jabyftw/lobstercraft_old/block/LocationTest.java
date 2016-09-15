package com.jabyftw.lobstercraft_old.block;

import java.util.Scanner;

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
public class LocationTest {

    /**
     * Conclusion:
     * chunk coordinates are given by (integer) block location >> 4
     * relative to chunk block coordinates are given by block location - (chunk coordinates * 16) if negative, subtract 1
     * By reverse math we have that block coordinates are given by:
     * (chunk coordinate * 16) + relative to chunk coordinate + 1 if negative
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int x, z;
        boolean shouldStop = false;

        while(!shouldStop) {
            // Scan x
            System.out.print("Type x: ");
            x = scanner.nextInt();

            // Scan z
            System.out.print("Type z: ");
            z = scanner.nextInt();

            System.out.println();

            // Show results
            System.out.println("chunk x: " + (x >> 4));
            System.out.println("chunk z: " + (z >> 4));
            System.out.println();
            System.out.println("relative x: " + (x - (x >> 4) * 16 - (x > 0 ? 0 : 1)));
            System.out.println("relative z: " + (z - (z >> 4) * 16 - (z > 0 ? 0 : 1)));

            // Ask to stop or repeat
            System.out.println();
            System.out.print("Should stop? ");
            shouldStop = scanner.nextBoolean();
            System.out.println();
            System.out.println();
        }
    }
}
