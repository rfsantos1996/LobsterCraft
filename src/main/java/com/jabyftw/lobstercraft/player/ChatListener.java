package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.ConfigurationValues;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.BlockLocation;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.util.NumberConversions;

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
class ChatListener implements Listener {

    private static final String CHAT_FORMAT = "%1$s§r: %2$s"; // %1 is the player, %2 is the message

    private final boolean USE_DISTANCE_CHAT = LobsterCraft.configuration.getBoolean(ConfigurationValues.PLAYER_CHAT_USE_DISTANCE_BASED_CHAT.toString());

    // Distance chat configuration
    private final double
            MAX_DISTANCE = LobsterCraft.configuration.getDouble(ConfigurationValues.PLAYER_CHAT_DISTANCE_TO_BE_HEARD_VALUE.toString()),
            MAX_DISTANCE_SQUARED = NumberConversions.square(MAX_DISTANCE);
    private final boolean
            DISTANCE_CHECK_Y = LobsterCraft.configuration.getBoolean(ConfigurationValues.PLAYER_CHAT_DISTANCE_TO_BE_HEARD_CHECK_Y.toString());

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onChatHighest(AsyncPlayerChatEvent event) {
        // Cancel all events
        event.setCancelled(true);

        // Block vanished players
        if (LobsterCraft.vanishManager.isVanished(event.getPlayer()))
            return;

        OnlinePlayer sender = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer(event.getPlayer(), OnlinePlayer.OnlineState.LOGGED_IN);
        // Check if player is online
        if (sender == null) return;

        // Check player's mute entries
        for (PlayerHandlerService.MutePlayerEntry entry : LobsterCraft.servicesManager.playerHandlerService.getPlayerMutedEntries(sender.getOfflinePlayer().getPlayerId())) {
            // Check if player is muted
            if (entry.isMuted()) {
                StringBuilder stringBuilder = new StringBuilder("§cVocê foi silenciado");
                if (entry.hasModerator()) stringBuilder.append(" por ").append(entry.getModerator().getPlayerName());
                stringBuilder.append("§c até ").append(Util.formatDate(entry.getUnmuteDate())).append(" pela razão: §4\"").append(entry.getReason()).append('\"');
                sender.getPlayer().sendMessage(stringBuilder.toString());
                return;
            }
        }

        // Useless, but set format for event in case someone uses it
        event.setFormat(CHAT_FORMAT);

        // Format chat
        String message = String.format(CHAT_FORMAT, event.getPlayer().getDisplayName(), event.getMessage());
        boolean heard = false;

        // Iterate through LOGGED IN players
        BlockLocation senderLocation = new BlockLocation(sender.getPlayer().getLocation());
        for (OnlinePlayer receiver : LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayers(OnlinePlayer.OnlineState.LOGGED_IN)) {
            // Ignore himself
            if (receiver.equals(sender))
                continue;

            // Check if player muted the talking one
            if (receiver.getProfile(ChatProfile.class).isPlayerMuted(sender.getOfflinePlayer().getPlayerId()))
                continue;

            // Check for distance based chat
            if (USE_DISTANCE_CHAT &&
                    // Check if the distance without Y-axis is greater than the maximum OR
                    ((DISTANCE_CHECK_Y && new BlockLocation(receiver.getPlayer().getLocation()).distanceXZSquared(senderLocation) > MAX_DISTANCE_SQUARED) ||
                            // Check if the distance WITH Y-axis is greater than the maximum
                            (!DISTANCE_CHECK_Y && new BlockLocation(receiver.getPlayer().getLocation()).distanceSquared(senderLocation) > MAX_DISTANCE_SQUARED)))
                continue;

            // Send message to online player
            receiver.getPlayer().sendMessage(message);

            // Check as heard if other player is visible
            if (!receiver.isInvisible())
                heard = true;
        }

        // Send player message if his message was heard
        if (heard) {
            sender.getPlayer().sendMessage(message);
            Bukkit.getScheduler().runTask(LobsterCraft.plugin, () -> event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10f, 1f));
        }
    }
}
