package com.jabyftw.lobstercraft_old.block;

import java.util.HashSet;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
public class OverriddenMethodTest {

    public static void main(String[] arguments) {

        HashSet<BaseClass> baseClasses = new HashSet<>();

        baseClasses.add(new BaseClass());
        baseClasses.add(new HigherClass());

        baseClasses.forEach(BaseClass::baseMethod);

        /*
         * Result, HigherClass will have his method used (as I expected)
         * I believe C++ I would need to call it virtual
         */
    }

    private static class BaseClass {

        protected void baseMethod() {
            System.out.println("BaseMethod");
        }
    }

    private static class HigherClass extends BaseClass {

        @Override
        protected void baseMethod() {
            System.out.println("Overriden BaseMethod");
        }
    }
}
