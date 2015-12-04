package com.jabyftw.pacocacraft.login;

import com.jabyftw.Util;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.location.TeleportProfile;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.player.PlayerMoment;
import com.jabyftw.pacocacraft.player.BasePlayerProfile;
import com.jabyftw.pacocacraft.player.ProfileType;
import com.jabyftw.pacocacraft.player.invisibility.InvisibilityService;
import com.jabyftw.pacocacraft.util.Permissions;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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
public class UserProfile extends BasePlayerProfile {

    // Login/profile information
    private long playerId = -1;
    private String playerName;
    private String password = null;
    private long lastTimeOnline = -1;

    // Variables (not database related)
    private volatile boolean loggedIn = false;
    private PlayerMoment preLoginMoment = null;

    /**
     * Create user profile given player's name for first login identification
     * NOTE: player name can be changed ingame
     *
     * @param playerName player's name
     */
    public UserProfile(@NotNull String playerName) {
        super(ProfileType.USER_PROFILE);
        this.playerName = playerName;
    }

    /**
     * Create user profile using database information
     *
     * @param playerName     player's name
     * @param playerId       player's database identification number
     * @param password       player's encrypted password
     * @param lastTimeOnline player's last time online (using System.currentTimeInMillis)
     *
     * @see System#currentTimeMillis()
     */
    public UserProfile(@NotNull String playerName, long playerId, @NotNull String password, long lastTimeOnline) {
        super(ProfileType.USER_PROFILE);
        this.playerId = playerId;
        this.playerName = playerName;
        this.password = password;
        this.lastTimeOnline = lastTimeOnline;
    }

    @Override
    public void onPlayerHandleApply(@NotNull PlayerHandler playerHandler) {
        // TODO hide player for everybody and hide everybody from player (show upon login)
        Player player = playerHandler.getPlayer();

        // Update display name and send greetings
        player.setDisplayName(ChatColor.translateAlternateColorCodes('&', PacocaCraft.chat.getPlayerPrefix(player) + player.getName() + PacocaCraft.chat.getPlayerSuffix(player)));
        player.sendMessage(
                (password == null ? "§6Bem vindo, " : "§6Bem vindo novamente, ") +
                        player.getDisplayName() + "§6!" +
                        (password == null ? "" : "\n§6Você entrou pela ultima vez em §c" + Util.parseTimeInMillis(lastTimeOnline, "dd/MM/yyyy HH:mm"))
        );

        // Store before login information
        preLoginMoment = new PlayerMoment(player);

        // Set login 'ideal status' (no potion effects, no pending damage etc)
        PlayerMoment.setIdealStatus(player);

        // Hide player from everyone and vice-versa
        InvisibilityService.hideEveryoneFromPlayer(player);
        InvisibilityService.hidePlayerFromEveryone(player);

        // Teleport to spawn (without saving player's last location but saving server's before-login location)
        TeleportProfile.teleportInstantaneously(player, player.getWorld().getSpawnLocation(), false);
    }

    @Override
    public void onPlayerHandleDestruction() {
        // Update last time online if logged in
        if(isLoggedIn()) {
            lastTimeOnline = System.currentTimeMillis();
            modified = true;
        }

        // Restore login moment if player isn't logged in still
        if(preLoginMoment != null) preLoginMoment.restorePlayerMoment();

        // Set logged in to false in the end (because it may be checked before)
        loggedIn = false;
    }

    @Override
    public boolean shouldBeSaved() {
        return super.shouldBeSaved() && password != null;
    }

    /**
     * Set player as logged in given password typed by player
     * NOTE: player will already "be welcomed"/warned about its state
     * NOTE: this should be ran asynchronously as it'll retrieve every profile on database
     * <p>
     * Conclusion: password, preLoginMoment, (not player, sendMessage is thread-safe)
     *
     * @param encryptedPassword encrypted user given password (caller must encrypt the password)
     *
     * @return true if successful log in
     */
    public boolean attemptLogin(@NotNull String encryptedPassword) {
        Player player = getPlayerHandler().getPlayer();

        // Check if password is the same or if the password even exists
        if(this.password != null && !this.password.equals(encryptedPassword)) {
            player.sendMessage("§4Senha incorreta! §cTente novamente...");
            return false;
        } else if(this.password == null) {
            player.sendMessage("§4Você não está registrado! §cUse o comando §6/register");
            return false;
        }

        // else, everything went fine, log player in and restore everything
        this.loggedIn = true;
        preLoginMoment.restorePlayerMoment();
        preLoginMoment = null;

        // TODO retrieve and initiate all other profiles using retrieved playerId from register or database lookup

        // Show player to everyone and vice-versa
        InvisibilityService.showEveryoneToPlayer(player);
        InvisibilityService.showPlayerToEveryone(player);

        // Broadcast messages
        player.sendMessage("§6Login bem sucedido!");
        if(!PacocaCraft.permission.playerHas(player, Permissions.JOIN_VANISHED))
            Bukkit.getServer().broadcastMessage("§b+ §3" + player.getName());

        return true;
    }

    /**
     * Set the player's password
     * NOTE: the player will be logged on and will "be welcomed"/warned about its state
     *
     * @param encryptedPassword password of the player (encrypted by caller; caller must be sure to check both passwords on command)
     *
     * @return true if player was successfully registered
     *
     * @see UserProfile#attemptLogin(String): this should be ran asynchronously as it'll retrieve every profile on database
     */
    public boolean registerPlayer(@NotNull String encryptedPassword) {
        // Check if player is already registered
        if(this.password != null) {
            getPlayerHandler().getPlayer().sendMessage("§cVocê já foi registrado!");
            return false;
        }

        // Register player password (it'll be saved)
        this.password = encryptedPassword;
        // TODO register and retrieve playerId
        attemptLogin(encryptedPassword);
        return true;
    }

    public long getPlayerId() {
        return playerId;
    }

    /**
     * Check if player is logged on (used to deny movement, most commands, events etc)
     *
     * @return true if player is logged in
     *
     * @see com.jabyftw.pacocacraft.login.LoginListener#onChat(AsyncPlayerChatEvent) should be a volatile now
     */
    public synchronized boolean isLoggedIn() {
        return loggedIn;
    }

    public String getPlayerName() {
        return playerName;
    }


    public long getLastTimeOnlineMillis() {
        return lastTimeOnline;
    }

    public String getEncryptedPassword() {
        return password;
    }
}
