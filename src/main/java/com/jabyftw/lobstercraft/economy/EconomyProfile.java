package com.jabyftw.lobstercraft.economy;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.Profile;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.sun.istack.internal.NotNull;

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
public class EconomyProfile extends Profile {

    private final EconomyStructure economyStructure;

    public EconomyProfile(long playerId, long economyId, double moneyAmount) {
        super(playerId);
        this.economyStructure = new EconomyStructure(economyId, EconomyStructure.StructureType.PLAYER_STRUCTURE, playerId, moneyAmount);
    }

    public EconomyProfile(long playerId, @NotNull final EconomyStructure economyStructure) {
        super(playerId);
        this.economyStructure = economyStructure;
    }

    @Override
    protected void onProfileApplication(@NotNull PlayerHandler playerHandler) {
    }

    @Override
    protected void onProfileDestruction() {
    }

    public EconomyStructure getEconomyStructure() {
        return economyStructure;
    }

    public double getMoneyAmount() {
        return economyStructure.getMoneyAmount();
    }

    public EconomyStructure.MoneyReceiveResponse receiveAmount(double amount, @NotNull final String reason) {
        return economyStructure.receiveAmount(amount, reason);
    }

    public EconomyStructure.SpentMoneyResponse spendAmount(double amount, @NotNull final String reason, boolean allowNegative) {
        return economyStructure.spendAmount(amount, reason, allowNegative);
    }

    @Override
    public DatabaseState getDatabaseState() {
        return economyStructure.getDatabaseState();
    }

    @Override
    protected void setAsModified() {
        economyStructure.setAsModified();
    }

    // TODO save and retrieve through economy structure
}
