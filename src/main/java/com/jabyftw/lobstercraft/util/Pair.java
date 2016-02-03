package com.jabyftw.lobstercraft.util;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

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
public class Pair<A, B> {

    private final A a;
    private B b;

    public Pair(@NotNull A a, @Nullable B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public void setB(@Nullable B b) {
        this.b = b;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Pair && ((Pair) obj).a.equals(a)) || (a.equals(obj));
    }
}
