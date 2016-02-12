package com.jabyftw.lobstercraft.player.util;

import com.jabyftw.lobstercraft.ConfigValue;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OfflinePlayerHandler;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

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
public class NameChangeEntry {

    private final static long
            NAME_AVAILABLE = TimeUnit.DAYS.toMillis(LobsterCraft.config.getLong(ConfigValue.LOGIN_NAME_CHANGE_USERNAME_AVAILABLE.toString())),
            CAN_CHANGE_AGAIN = TimeUnit.DAYS.toMillis(LobsterCraft.config.getLong(ConfigValue.LOGIN_NAME_CHANGE_PLAYER_ALLOWED_TO_CHANGE.toString()));

    private final long ownerId;
    private long changeDate;
    private String oldPlayerName;

    public NameChangeEntry(@NotNull final OfflinePlayerHandler offlinePlayerHandler) {
        this.ownerId = offlinePlayerHandler.getPlayerId();
        this.oldPlayerName = offlinePlayerHandler.getPlayerName();
        this.changeDate = System.currentTimeMillis();
    }

    public NameChangeEntry(long ownerId, @NotNull final String oldPlayerName, long changeDate) {
        this.ownerId = ownerId;
        this.oldPlayerName = oldPlayerName;
        this.changeDate = changeDate;
    }

    public String getOldPlayerName() {
        return oldPlayerName;
    }

    public NameChangeEntry setOldPlayerName(@NotNull final String oldPlayerName) {
        this.oldPlayerName = oldPlayerName;
        this.changeDate = System.currentTimeMillis();
        return this;
    }

    public long getPlayerId() {
        return ownerId;
    }

    public long getChangeDate() {
        return changeDate;
    }

    public boolean isNameAvailable() {
        return System.currentTimeMillis() - changeDate > NAME_AVAILABLE;
    }

    public boolean canPlayerChangeUsername() {
        return System.currentTimeMillis() - changeDate > CAN_CHANGE_AGAIN;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3, 97).append(ownerId).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NameChangeEntry && ((NameChangeEntry) obj).ownerId == ownerId;
    }

    public static void saveNameChangeEntry(boolean insertEntry, @NotNull final NameChangeEntry nameChangeEntry) throws SQLException {
        // Retrieve connection
        Connection connection = LobsterCraft.dataSource.getConnection();

        // Prepare statement
        PreparedStatement preparedStatement;
        if (insertEntry)
            preparedStatement = connection.prepareStatement("INSERT INTO `minecraft`.`player_name_changes` (`user_playerId`, `oldPlayerName`, `changeDate`) VALUES (?, ?, ?);");
        else
            preparedStatement = connection.prepareStatement("UPDATE `minecraft`.`player_name_changes` SET `oldPlayerName` = ?, `changeDate` = ? WHERE `user_playerId` = ?;");

        // Set variables
        int index = insertEntry ? 2 : 1;
        preparedStatement.setLong(insertEntry ? 1 : 3, nameChangeEntry.getPlayerId());
        preparedStatement.setString(index++, nameChangeEntry.getOldPlayerName());
        preparedStatement.setLong(index, nameChangeEntry.getChangeDate());

        // Execute and close statement
        preparedStatement.execute();
        preparedStatement.close();

        // Close connection
        connection.close();
    }
}
