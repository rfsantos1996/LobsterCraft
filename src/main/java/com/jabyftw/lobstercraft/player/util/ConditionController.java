package com.jabyftw.lobstercraft.player.util;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.sun.istack.internal.NotNull;

import java.util.EnumMap;
import java.util.concurrent.TimeUnit;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
public class ConditionController {

    private final EnumMap<Condition, Long> conditionMap = new EnumMap<>(Condition.class);
    private final PlayerHandler playerHandler;

    public ConditionController(@NotNull final PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
    }

    public void insertCondition(@NotNull final Condition condition) {
        conditionMap.put(condition, System.currentTimeMillis());
    }

    public boolean sendMessageIfNotReady(@NotNull final Condition condition, @NotNull final String message) {
        if (playerHandler.isLoggedIn() && !insertConditionIfReady(condition)) {
            playerHandler.sendMessage(message);
            return true;
        }
        return false;
    }

    public boolean sendMessageIfConditionReady(@NotNull final Condition condition, @NotNull final String message) {
        if (playerHandler.isLoggedIn() && insertConditionIfReady(condition)) {
            playerHandler.sendMessage(message);
            return true;
        }
        return false;
    }

    public boolean insertConditionIfReady(@NotNull final Condition condition) {
        if (isConditionReady(condition)) {
            conditionMap.put(condition, System.currentTimeMillis());
            return true;
        }
        // Condition not ready
        return false;
    }

    /**
     * Check if the condition is ready to be triggered.
     * Returns true if the condition wasn't inserted OR if the condition is greater than the threshold
     *
     * @param condition condition enum
     * @return true if the condition has passed its wait time
     */
    public boolean isConditionReady(@NotNull final Condition condition) {
        return (System.currentTimeMillis() - conditionMap.getOrDefault(condition, 0L)) > condition.getConditionTime();
    }

    public enum Condition {

        PROTECTION_CHECK(TimeUnit.MILLISECONDS.toMillis(500)),
        PROTECTION_BEING_LOADED(TimeUnit.SECONDS.toMillis(3)),
        PROTECTION_ADMINISTRATOR_BLOCKS(TimeUnit.SECONDS.toMillis(5)),
        PROTECTION_PLAYER_BLOCKS(TimeUnit.SECONDS.toMillis(3));

        private final long conditionTime;

        Condition(long conditionTime) {
            this.conditionTime = conditionTime;
        }

        public long getConditionTime() {
            return conditionTime;
        }
    }
}
