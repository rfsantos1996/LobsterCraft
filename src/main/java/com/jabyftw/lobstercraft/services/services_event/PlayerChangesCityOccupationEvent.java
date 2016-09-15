package com.jabyftw.lobstercraft.services.services_event;

import com.jabyftw.lobstercraft.player.OfflinePlayer;
import com.jabyftw.lobstercraft.world.CityOccupation;
import com.jabyftw.lobstercraft.world.CityStructure;
import com.sun.istack.internal.NotNull;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
public class PlayerChangesCityOccupationEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final OfflinePlayer offlinePlayer;
    private final CityStructure cityStructure;
    private final CityOccupation cityOccupation;

    // This is called after a few checks, it is supposed to be successful
    private CityStructure.ChangeOccupationResponse result = CityStructure.ChangeOccupationResponse.SUCCESSFULLY_CHANGED;

    /**
     * This event is called when a OfflinePlayer joins a city
     *
     * @param offlinePlayer online player that is joining the city
     */
    public PlayerChangesCityOccupationEvent(@NotNull final OfflinePlayer offlinePlayer, @NotNull final CityStructure cityStructure,
                                            @NotNull final CityOccupation cityOccupation) {
        this.offlinePlayer = offlinePlayer;
        this.cityStructure = cityStructure;
        this.cityOccupation = cityOccupation;
    }

    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    public CityStructure getCityStructure() {
        return cityStructure;
    }

    public CityOccupation getCityOccupation() {
        return cityOccupation;
    }

    public CityStructure.ChangeOccupationResponse getResult() {
        return result;
    }

    public void setResult(@NotNull final CityStructure.ChangeOccupationResponse result) {
        this.result = result;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return result != CityStructure.ChangeOccupationResponse.SUCCESSFULLY_CHANGED;
    }

    @Override
    public void setCancelled(boolean cancel) {
        throw new NotImplementedException("You should use setResult(CityStructure.ChangeOccupationResponse)");
    }
}
