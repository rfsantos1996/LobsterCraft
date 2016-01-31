package com.jabyftw.lobstercraft.util;

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
public enum DatabaseState {

    ON_DATABASE,
    INSERT_TO_DATABASE,
    UPDATE_DATABASE,
    DELETE_FROM_DATABASE,
    NOT_ON_DATABASE;

    /**
     * Check the object, on its current state, needs synchronization with the database
     *
     * @return true if object needs to be saved.
     */
    public boolean shouldSave() {
        switch (this) {
            case INSERT_TO_DATABASE:
            case UPDATE_DATABASE:
            case DELETE_FROM_DATABASE:
                return true;
            default:
                return false;
        }
    }
}
