package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.util.DatabaseState;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
public abstract class Profile {

    private final long playerId;
    protected DatabaseState databaseState = DatabaseState.NOT_ON_DATABASE;
    private PlayerHandler playerHandler = null;

    protected Profile(long playerId) {
        this.playerId = playerId;
    }

    public final void applyProfile(@NotNull final PlayerHandler playerHandler) {
        this.playerHandler = playerHandler;
        playerHandler.playerProfiles.put(getProfileType(), this);
        onProfileApplication(playerHandler);
    }

    public final void destroyProfile() {
        onProfileDestruction();
    }

    protected abstract void onProfileApplication(@NotNull final PlayerHandler playerHandler);

    protected abstract void onProfileDestruction();

    /*
     * Getters and setters
     */

    public final ProfileType getProfileType() {
        return ProfileType.getType(this);
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    public long getPlayerId() {
        return playerId;
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    protected void setAsModified() {
        if (databaseState == DatabaseState.NOT_ON_DATABASE)
            databaseState = DatabaseState.INSERT_TO_DATABASE;
        if (databaseState == DatabaseState.ON_DATABASE)
            databaseState = DatabaseState.UPDATE_DATABASE;
        // If is DELETE_DATABASE | INSERT_DATABASE | UPDATE_DATABASE, continue DELETE_DATABASE | INSERT_DATABASE | UPDATE_DATABASE
    }

    // I'm making equals(Object) and hashCode() final so I can remember that I need to check these parameters when (possibly, in the future) overriding it

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof Profile && ((Profile) obj).playerId == playerId && ((Profile) obj).getProfileType() == getProfileType();
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder(57, 59)
                .append(getProfileType())
                .append(playerId)
                .toHashCode();
    }
}
