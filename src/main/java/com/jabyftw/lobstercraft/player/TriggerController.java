package com.jabyftw.lobstercraft.player;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.concurrent.TimeUnit;

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
public class TriggerController {

    private final EnumMap<TemporaryTrigger, Long> triggerTimeMap = new EnumMap<>(TemporaryTrigger.class);
    private final Player player;

    public TriggerController(@NotNull final Player player) {
        this.player = player;
    }

    /**
     * @param temporaryTrigger the trigger used
     * @param message          message to send the player if trigger wasn't triggered ("first time used" on the past minutes)
     * @return true if it wasn't triggered (last use was greater than the threshold)
     */
    public boolean sendMessageIfNotTriggered(@NotNull final TemporaryTrigger temporaryTrigger, @Nullable final String... message) {
        if (!insertTriggerIfReady(temporaryTrigger)) {
            if (message != null && player.isOnline()) player.sendMessage(message);
            return true;
        }
        return false;
    }

    /**
     * @param temporaryTrigger the trigger used
     * @param message          message to send the player if the trigger was ready ("second time used" on the past seconds)
     * @return true if it was triggered (last use was less than the threshold)
     */
    public boolean sendMessageIfTriggered(@NotNull final TemporaryTrigger temporaryTrigger, @Nullable final String message) {
        if (insertTriggerIfReady(temporaryTrigger)) {
            if (message != null && player.isOnline()) player.sendMessage(message);
            return true;
        }
        return false;
    }

    /**
     * Check if the temporaryTrigger is ready to be triggered again, that means: the time since last used is greater than the trigger threshold.
     *
     * @param temporaryTrigger trigger used
     * @return true if the trigger waited more than enough (more than the threshold)
     */
    private boolean insertTriggerIfReady(@NotNull final TemporaryTrigger temporaryTrigger) {
        if ((System.currentTimeMillis() - triggerTimeMap.getOrDefault(temporaryTrigger, 0L)) > temporaryTrigger.getConditionTime()) {
            triggerTimeMap.put(temporaryTrigger, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public enum TemporaryTrigger {

        PLAYER_UNSAFE_ENCHANTMENT_CHECK(TimeUnit.SECONDS.toMillis(10)),
        PLAYER_CLEAR_INVENTORY_CHECK(TimeUnit.SECONDS.toMillis(10)),
        PLAYER_SUICIDE_CHECK(TimeUnit.SECONDS.toMillis(15)),
        PROTECTION_CHECK(TimeUnit.MILLISECONDS.toMillis(500)),
        PROTECTION_BUILD_MODE_WARNING(TimeUnit.MINUTES.toMillis(2)),
        PROTECTION_BEING_LOADED(TimeUnit.SECONDS.toMillis(3)),
        PROTECTION_RESPONSE(TimeUnit.SECONDS.toMillis(5)),
        PROTECTION_MINIMUM_HEIGHT(TimeUnit.MINUTES.toMillis(4)),

        DELETE_CONSTRUCTION_CHECK(TimeUnit.SECONDS.toMillis(15)),
        WORLD_EDIT_PLAYER_PROTECTION_WARNING(TimeUnit.SECONDS.toMillis(30));

        private final long conditionTime;

        TemporaryTrigger(long conditionTime) {
            this.conditionTime = conditionTime;
        }

        private long getConditionTime() {
            return conditionTime;
        }
    }
}
