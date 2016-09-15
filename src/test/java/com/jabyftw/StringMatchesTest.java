package com.jabyftw;

import com.jabyftw.lobstercraft.util.Util;

import java.text.DecimalFormat;

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
public class StringMatchesTest {

    public static void main(String[] args) {
        System.out.println(new DecimalFormat("%").format(0.3D));
        System.out.println(new DecimalFormat("%").format(0.03D));
        System.out.println(new DecimalFormat("%").format(3.0D));
        System.out.println(new DecimalFormat("##0").format(0.3D * 100) + "%");
        System.out.println(new DecimalFormat("##0").format(0.03D * 100) + "%");
        System.out.println(new DecimalFormat("##0").format(3.0D * 100) + "%");
        System.out.println(new DecimalFormat("##0%").format(0.3D));
        System.out.println(new DecimalFormat("##0%").format(0.03D));
        System.out.println(new DecimalFormat("##0%").format(3.0D));

        String[] strings = {
                "City Name Valid",
                "CITY_NAME -VALID",
                "CITY_NAME -VALIDCITY_NAME NOT VALID",
                "Pang",
                "Nva"
        };
        String[] regexes = {
                "[A-Za-z _0-9]{" + 4 + "," + 24 + "}"
        };
        for (String test : strings)
            for (String regex : regexes)
                System.out.println(Util.appendStrings("Checking \"", test, "\". Matches with ", regex, "? ", test.matches(regex)));
    }
}
