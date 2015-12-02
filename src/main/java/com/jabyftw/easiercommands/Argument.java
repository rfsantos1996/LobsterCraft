package com.jabyftw.easiercommands;

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

    public Argument(String argument) {
        addArgumentType(ArgumentType.STRING, argument);
    }

    public void addArgumentType(ArgumentType argumentType, Object object) {
        this.arguments.put(argumentType, object);
    }

    public Object getArgument(Class<?> expectedClass) {
        for(ArgumentType argumentType : getArgumentTypes()) {
            if(argumentType.getClazz().isAssignableFrom(expectedClass))
                return arguments.get(argumentType);
        }

        return null;
    }

    public Set<ArgumentType> getArgumentTypes() {
        return arguments.keySet();
    }

    @Override
    public String toString() {
        return getClass().getName() + " -> " + getArgumentTypes().toString();
    }
}

