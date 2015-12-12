package com.jabyftw.pacocacraft.player;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.pacocacraft.player.commands.*;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.jabyftw.pacocacraft.util.ServerService;
import com.jabyftw.profile_util.PlayerProfile;
import com.jabyftw.profile_util.ProfileType;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
public class PlayerService implements ServerService {

    public static final long TIME_BETWEEN_PROFILE_SAVES_TICKS = PacocaCraft.config.getLong(ConfigValue.LOGIN_TIME_BETWEEN_PROFILE_SAVES.getPath()) * 20L; // seconds * 20 = number of ticks

    private static final Object storedProfileLock = new Object();
    private static final HashBasedTable<Long, ProfileType, PlayerProfile> storedProfiles = HashBasedTable.create();

    private StoredProfilesSavingTask profilesSavingTask;
    private BukkitTask storedProfilesTask;

    @Override
    public void onEnable() {
        InventorySpyCommand inventorySpyCommand;

        Bukkit.getServer().getPluginCommand("godmode").setExecutor(new GodModeCommand());
        Bukkit.getServer().getPluginCommand("gamemode").setExecutor(new GameModeCommand());
        Bukkit.getServer().getPluginCommand("fly").setExecutor(new FlyCommand());
        Bukkit.getServer().getPluginCommand("item").setExecutor(new PendingItemsCommand());
        Bukkit.getServer().getPluginCommand("give").setExecutor(new GiveCommand());
        Bukkit.getServer().getPluginCommand("heal").setExecutor(new HealCommand());
        Bukkit.getServer().getPluginCommand("enchant").setExecutor(new EnchantCommand());
        Bukkit.getServer().getPluginCommand("clearenchantment").setExecutor(new ClearEnchantmentCommand());
        Bukkit.getServer().getPluginCommand("workbench").setExecutor(new WorkbenchCommand());
        Bukkit.getServer().getPluginCommand("list").setExecutor(new ListCommand());
        //Bukkit.getServer().getPluginCommand("whisper").setExecutor(new WhisperCommand()); // TODO after my chat idea
        //Bukkit.getServer().getPluginCommand("r").setExecutor(new ReplyCommand());
        //Bukkit.getServer().getPluginCommand("mute").setExecutor(new MuteCommand());
        Bukkit.getServer().getPluginCommand("clear").setExecutor(new ClearInventoryCommand());
        Bukkit.getServer().getPluginCommand("suicide").setExecutor(new SuicideCommand());
        Bukkit.getServer().getPluginCommand("kill").setExecutor(new KillPlayersCommand());
        Bukkit.getServer().getPluginCommand("killall").setExecutor(new KillEntitiesCommand());
        Bukkit.getServer().getPluginCommand("spawnmob").setExecutor(new SpawnEntitiesCommand());
        Bukkit.getServer().getPluginCommand("pweather").setExecutor(new PlayerWeatherCommand());
        Bukkit.getServer().getPluginCommand("ptime").setExecutor(new PlayerTimeCommand());
        Bukkit.getServer().getPluginCommand("speed").setExecutor(new SpeedCommand());
        Bukkit.getServer().getPluginCommand("repair").setExecutor(new RepairCommand());
        Bukkit.getServer().getPluginCommand("spyinv").setExecutor((inventorySpyCommand = new InventorySpyCommand()));
        Bukkit.getServer().getPluginCommand("exp").setExecutor(new ExpCommand());
        Bukkit.getServer().getPluginCommand("level").setExecutor(new LevelCommand());
        Bukkit.getServer().getPluginCommand("feed").setExecutor(new FeedEventCommand());
        Bukkit.getServer().getPluginCommand("hat").setExecutor(new HatCommand());

        Bukkit.getServer().getPluginManager().registerEvents(new PlayerListener(), PacocaCraft.pacocaCraft);
        Bukkit.getServer().getPluginManager().registerEvents(inventorySpyCommand, PacocaCraft.pacocaCraft);

        storedProfilesTask = BukkitScheduler.runTaskTimerAsynchronously(
                PacocaCraft.pacocaCraft,
                (profilesSavingTask = new StoredProfilesSavingTask()),
                TIME_BETWEEN_PROFILE_SAVES_TICKS,
                TIME_BETWEEN_PROFILE_SAVES_TICKS
        );
        PacocaCraft.logger.info("Enabled " + getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
        storedProfilesTask.cancel();
        // Force run if server is closing
        while(!storedProfiles.isEmpty()) {
            PacocaCraft.logger.info("Trying to save all player profiles; If stuck, tell developer");
            profilesSavingTask.run();
        }
    }

    /**
     * Store profile instance in case player rejoins in a few minutes
     * <b>NOTE:</b> profiles are retrieved on successful login
     *
     * @param playerProfile given profile of a logged off player
     *
     * @see StoredProfilesSavingTask the saving task that will take care of the profile after its lifetime
     */
    public <T extends PlayerProfile> void storeProfile(@NotNull T playerProfile) {
        // Make sure profile is correctly removed
        if(playerProfile.getPlayerHandler() != null)
            throw new IllegalArgumentException("Delivered a profile that wasn't correctly removed from the player!");

        // Set the date when the profile was moved
        playerProfile.setStoredSince(System.currentTimeMillis());

        // Insert it on the table synchronously
        synchronized(storedProfileLock) {
            storedProfiles.put(playerProfile.getPlayerId(), playerProfile.getProfileType(), playerProfile);
        }
    }

    public Map<ProfileType, PlayerProfile> getProfiles(long playerId) {
        Map<ProfileType, PlayerProfile> row = new EnumMap<>(ProfileType.class);

        synchronized(storedProfileLock) {
            // Check if map is not empty
            Map<ProfileType, PlayerProfile> storedProfilesRow = storedProfiles.row(playerId);
            if(storedProfilesRow.isEmpty())
                return row;

            // put map if it isn't empty
            row.putAll(storedProfilesRow);

            // Remove profiles from storage
            for(ProfileType profileType : row.keySet())
                storedProfiles.remove(playerId, profileType);
        }

        // Set as not stored
        for(PlayerProfile playerProfile : row.values())
            playerProfile.setStoredSince(-1);

        return row;
    }

    protected class StoredProfilesSavingTask implements Runnable {

        private final EnumMap<ProfileType, ArrayList<PlayerProfile>> pendingSaveProfiles = new EnumMap<>(ProfileType.class);
        private final long PROFILE_LIFETIME_MILLIS = TimeUnit.SECONDS.toMillis(PacocaCraft.config.getLong(ConfigValue.LOGIN_PROFILE_WAITING_TIME.getPath()));

        protected StoredProfilesSavingTask() {
            for(ProfileType profileType : ProfileType.values())
                if(!pendingSaveProfiles.containsKey(profileType))
                    pendingSaveProfiles.put(profileType, new ArrayList<>());
        }

        @Override
        public void run() {
            synchronized(storedProfileLock) {
                Iterator<Table.Cell<Long, ProfileType, PlayerProfile>> iterator = storedProfiles.cellSet().iterator();

                // Iterate through all values
                while(iterator.hasNext()) {
                    Table.Cell<Long, ProfileType, PlayerProfile> cell = iterator.next();

                    // Check if profile waited its lifetime or if server is closing (forcing)
                    if((System.currentTimeMillis() - cell.getValue().getStoredSince()) >= PROFILE_LIFETIME_MILLIS || PacocaCraft.isServerClosing()) {

                        // Save profile if needed
                        if(cell.getValue().shouldBeSaved()) // if it shouldn't, it'll be removed without updating database
                            pendingSaveProfiles.get(cell.getColumnKey()).add(cell.getValue());

                        // Remove profile, at last
                        iterator.remove();
                    }
                }
            }

            // Iterate between profile types
            for(ArrayList<PlayerProfile> playerProfiles : pendingSaveProfiles.values()) {
                Iterator<PlayerProfile> iterator = playerProfiles.iterator();

                // Iterate between profiles
                while(iterator.hasNext()) {
                    PlayerProfile next = iterator.next();

                    // Save profile
                    try {
                        next.getProfileType().saveProfile(next);
                    } catch(SQLException e) {
                        e.printStackTrace();
                    }

                    // Remove profile from storage
                    iterator.remove();
                }
            }
        }
    }
}
