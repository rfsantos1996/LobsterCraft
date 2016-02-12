package com.jabyftw.lobstercraft.economy;

import com.jabyftw.lobstercraft.util.Util;
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
public class EconomyHistoryEntry {

    private final String reason;
    private final long entryDate;
    private final double beforeAmount, deltaAmount;

    public EconomyHistoryEntry(@NotNull final String reason, long entryDate, double beforeAmount, double deltaAmount) {
        this.reason = reason;
        this.entryDate = entryDate;
        this.beforeAmount = beforeAmount;
        this.deltaAmount = deltaAmount;
    }

    public double getDeltaAmount() {
        return deltaAmount;
    }

    public double getBeforeAmount() {
        return beforeAmount;
    }

    public String getEntryDate() {
        return Util.formatDate(entryDate);
    }

    public String getReason() {
        return reason;
    }
}
