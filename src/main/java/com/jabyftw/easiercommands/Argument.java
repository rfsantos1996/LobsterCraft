package com.jabyftw.easiercommands;

import com.sun.istack.internal.NotNull;

import java.util.HashMap;
import java.util.Set;

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
public class Argument {

    private final HashMap<ArgumentType, Object> arguments = new HashMap<>();

    public Argument() {
    }

    /**
     * Add argument type object to the argument
     *
     * @param argumentType the argument type of the object
     * @param object       the object that fits into the argument
     * @param <T>          any type
     */
    protected <T> void addArgumentType(ArgumentType argumentType, @NotNull T object) {
        this.arguments.put(argumentType, object);
    }

    /**
     * Get argument object based on required type
     *
     * @param expectedClass required type
     *
     * @return object of the expected type, or null if none find
     */
    @SuppressWarnings("unchecked")
    public <T> T getArgument(Class<T> expectedClass) {
        for(ArgumentType argumentType : getArgumentTypes()) {
            // Loop through available arguments until a fit
            if(argumentType.getClazz().isAssignableFrom(expectedClass))
                return (T) arguments.get(argumentType);
        }
        return null;
    }

    /**
     * @return a set of available types on this argument
     */
    protected Set<ArgumentType> getArgumentTypes() {
        return arguments.keySet();
    }

    @Override
    public String toString() {
        return getClass().getName() + " -> " + getArgumentTypes().toString();
    }
}

