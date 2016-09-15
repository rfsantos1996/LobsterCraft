package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.CityOccupation;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang.builder.HashCodeBuilder;

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
public class OfflinePlayer {

    private static final double DEFAULT_MONEY_AMOUNT = LobsterCraft.configuration.getDouble(ConfigurationValues.PLAYER_DEFAULT_MONEY_AMOUNT.toString());

    // Player database information
    Integer playerId = null;
    protected String playerName;
    protected String encryptedPassword = null;
    protected double moneyAmount = DEFAULT_MONEY_AMOUNT;
    protected Short cityId = null;
    protected CityOccupation cityOccupation = null;
    protected Long lastTimeOnline = null;
    protected long timePlayed = 0;
    protected String lastIp = null;

    // Profile variables
    DatabaseState databaseState = DatabaseState.NOT_ON_DATABASE;

    /**
     * Build a generic OfflinePlayer, checking if name is valid
     *
     * @param playerName player's name
     * @throws IllegalArgumentException if name isn't valid
     */
    OfflinePlayer(@NotNull final String playerName) throws IllegalArgumentException {
        if (!Util.checkStringCharactersAndLength(playerName, 3, 16))
            throw new IllegalArgumentException("Player name isn't valid!");
        this.playerName = playerName.toLowerCase();
    }

    /**
     * Build a registered OfflinePlayer from database information
     *
     * @param playerId          player's Id (can't be null, register will define it)
     * @param playerName        player's name - will be lower cased everywhere
     * @param encryptedPassword player's encrypted password (SHA-256 from Util)
     * @param moneyAmount       player's money amount
     * @param cityId            player's city Id
     * @param cityOccupation    player's position in city
     * @param lastTimeOnline    player's last time online (quit date in milliseconds using currentTimeMillis from System) (can't be null, register will record it)
     * @param timePlayed        player's time played
     * @param lastIp            player's last IP (can't be null, register will record it)
     * @see com.jabyftw.lobstercraft.util.Util#encryptString(String)
     * @see System#currentTimeMillis()
     */
    OfflinePlayer(int playerId, @NotNull final String playerName, @NotNull final String encryptedPassword, double moneyAmount, @Nullable Short cityId,
                  @Nullable CityOccupation cityOccupation, @NotNull Long lastTimeOnline, long timePlayed, @NotNull String lastIp) {
        this.playerId = playerId;
        this.playerName = playerName.toLowerCase();
        this.encryptedPassword = encryptedPassword;
        this.moneyAmount = moneyAmount;
        this.cityId = cityId;
        this.cityOccupation = cityOccupation;
        this.lastTimeOnline = lastTimeOnline;
        this.timePlayed = timePlayed;
        this.lastIp = lastIp;
        this.databaseState = DatabaseState.ON_DATABASE;
    }

    public boolean isRegistered() {
        return playerId != null;
    }

    /**
     * <b>CAREFUL:</b> name changes might create a bug on PlayerHandlerService's online players map
     *
     * @param onlineState filter online player if necessary (can be null)
     * @return null if player is offline
     * @see PlayerHandlerService#getOnlinePlayer(OfflinePlayer, OnlinePlayer.OnlineState)
     */
    public OnlinePlayer getOnlinePlayer(@Nullable OnlinePlayer.OnlineState onlineState) {
        return LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(this, onlineState);
    }

    public org.bukkit.OfflinePlayer getBukkitOfflinePlayer() {
        //noinspection deprecation
        return LobsterCraft.plugin.getServer().getOfflinePlayer(playerName);
    }

    /*
     * Getters
     */

    /**
     * @return null if player isn't registered
     */
    public Integer getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return null if player isn't registered
     */
    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    /**
     * @return null if player isn't registered
     */
    public double getMoneyAmount() {
        return moneyAmount;
    }

    /**
     * @return null if player isn't on a city
     */
    public Short getCityId() {
        return cityId;
    }

    /**
     * @return null if player isn't on a city
     */
    public CityOccupation getCityOccupation() {
        return cityOccupation;
    }

    /**
     * @return null if player isn't registered
     */
    public Long getLastTimeOnline() {
        return lastTimeOnline;
    }

    public long getTimePlayed() {
        return timePlayed;
    }

    /**
     * @return null if player isn't registered
     */
    public String getLastIp() {
        return lastIp;
    }

    public DatabaseState getDatabaseState() {
        return databaseState;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null &&
                (obj instanceof OfflinePlayer && ((OfflinePlayer) obj).playerName.equalsIgnoreCase(playerName) ||
                        obj instanceof OnlinePlayer && ((OnlinePlayer) obj).getOfflinePlayer().equals(this));
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3, 7)
                .append(playerName)
                .toHashCode();
    }
}
