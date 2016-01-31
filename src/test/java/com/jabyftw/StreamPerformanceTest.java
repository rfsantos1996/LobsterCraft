package com.jabyftw;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
public class StreamPerformanceTest {

    private final static int NUMBER_OF_TESTS = 1, NUMBER_OF_ELEMENTS = 12_240_000;
    private final static DecimalFormat formatter = new DecimalFormat("0.0000");
    private final static HashSet<Integer> source = new HashSet<>();

    /**
     * ### Test 1: Player list usage ###
     * Output:
     * Took 1119,5919ms to execute stream test.
     * Took 12372,8909ms to execute parallel stream test.
     * Took 644,7727ms to execute vanilla test.
     *
     * Using 1mi tests with 50 elements (what is near for our server usage at player manipulation) => stream isn't efficient for that
     *
     * ### Test 2: Block list usage ###
     * Output:
     * Took 3075,1891ms to execute stream test.
     * Took 1510,8163ms to execute parallel stream test.
     * Took 2001,4942ms to execute vanilla test.
     *
     * Using 10k tests with 24k elements => stream isn't efficient here too...
     */
    public static void main(String[] arguments) {
        // Prepare source
        for(int i = 0; i < NUMBER_OF_ELEMENTS; i++)
            source.add(i);

        // Start stream test
        long streamTime = System.nanoTime();

        for(int i = 0; i < NUMBER_OF_TESTS; i++)
            testStream();
        System.out.println("Took " + formatter.format((System.nanoTime() - streamTime) / (double) TimeUnit.MILLISECONDS.toNanos(1)) + "ms to execute stream test.");

        // Start parallel stream test
        long parallelStreamTime = System.nanoTime();

        for(int i = 0; i < NUMBER_OF_TESTS; i++)
            testParallelStream();
        System.out.println("Took " + formatter.format((System.nanoTime() - parallelStreamTime) / (double) TimeUnit.MILLISECONDS.toNanos(1)) + "ms to execute parallel stream test.");

        // Start vanilla test
        long vanillaTime = System.nanoTime();

        for(int i = 0; i < NUMBER_OF_TESTS; i++)
            testVanilla();
        System.out.println("Took " + formatter.format((System.nanoTime() - vanillaTime) / (double) TimeUnit.MILLISECONDS.toNanos(1)) + "ms to execute vanilla test.");
    }

    public static List<Double> testStream() {
        return source.stream().filter(integer -> integer % 2 == 0).map(Math::sqrt).collect(Collectors.toList());
    }

    public static List<Double> testParallelStream() {
        return source.parallelStream().filter(integer -> integer % 2 == 0).map(Math::sqrt).collect(Collectors.toList());
    }

    public static ArrayList<Double> testVanilla() {
        ArrayList<Double> doubles = new ArrayList<>();
        for(Integer integer : source) {
            if(integer % 2 == 0)
                doubles.add(Math.sqrt(integer));
        }
        return doubles;
    }
}
