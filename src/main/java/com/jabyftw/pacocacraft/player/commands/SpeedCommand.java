package com.jabyftw.pacocacraft.player.commands;

import com.jabyftw.Util;
import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.configuration.ConfigValue;
import com.jabyftw.profile_util.PlayerHandler;
import com.jabyftw.pacocacraft.util.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
public class SpeedCommand extends CommandExecutor {

    private final static double
            MINIMUM_SPEED_MULTIPLIER = PacocaCraft.config.getDouble(ConfigValue.PLAYER_SPEED_MINIMUM_MULTIPLIER.getPath()),
            MAXIMUM_SPEED_MULTIPLIER = PacocaCraft.config.getDouble(ConfigValue.PLAYER_SPEED_MAXIMUM_MULTIPLIER.getPath());

    public SpeedCommand() {
        super(PacocaCraft.pacocaCraft, "speed", Permissions.PLAYER_SPEED, "§6Permite ao jogador mudar a sua velocidade", "§c/speed (§4fly/walk§c) (§4velocidade§c)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onSpeedChange(PlayerHandler playerHandler, String type) {
        return onSpeedChange(playerHandler, type, 1f);
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    public boolean onSpeedChange(PlayerHandler playerHandler, String type, float speed) {
        Player player = playerHandler.getPlayer();
        boolean isFlySpeed = type.toLowerCase().startsWith("f") || type.toLowerCase().startsWith("v"); // "fly" "voar"

        // Check speed limits (it'll already warn the player
        if(!checkSpeedLimits(player, speed))
            return true;

        // Set player speed
        setPlayerSpeed(player, isFlySpeed, getBukkitMoveSpeed(speed, isFlySpeed));

        // Send player message
        Util.sendPlayerMessage(playerHandler, "§6Sua velocidade de §c" + (isFlySpeed ? "vôo" : "andar") + "§6 foi alterada para §c" + (speed == 1f ? "o padrão" : speed));
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_SPEED_OTHERS)
    public boolean onSpeedChangeOthers(CommandSender commandSender, PlayerHandler target, String type, float speed) {
        Player targetPlayer = target.getPlayer();
        boolean isFlySpeed = type.toLowerCase().startsWith("f") || type.toLowerCase().startsWith("v"); // "fly" "voar"

        // Check speed limits (it'll already warn the targetPlayer
        if(!checkSpeedLimits(commandSender, speed))
            return true;

        // Set targetPlayer speed
        setPlayerSpeed(targetPlayer, isFlySpeed, getBukkitMoveSpeed(speed, isFlySpeed));

        // Send targetPlayer message
        Util.sendPlayerMessage(target, "§6Sua velocidade de §c" + (isFlySpeed ? "vôo" : "andar") + "§6 foi alterada para §c" + (speed == 1f ? "o padrão" : speed) + "§6 por §c" + commandSender.getName());
        Util.sendCommandSenderMessage(
                commandSender,
                "§6Velocidade de " + targetPlayer.getDisplayName() + "§6 de §c" + (isFlySpeed ? "vôo" : "andar") + "§6 foi alterada para §c" + (speed == 1f ? "o padrão" : speed)
        );
        return true;
    }

    /**
     * 1 is the default shown speed (both fly and walking speed) but the default flySpeed and walkSpeed are, respectively, 0.2f and 0.1f
     *
     * @param shownSpeed "our" speed / the multiplier
     * @param isFlySpeed consider as fly speed
     *
     * @return the Bukkit's (Minecraft's) speed
     */
    private float getBukkitMoveSpeed(float shownSpeed, boolean isFlySpeed) {
        return (isFlySpeed ? 0.2f : 0.1f) * shownSpeed;
    }

    private boolean checkSpeedLimits(CommandSender commandSender, float speed) {
        if(speed < MINIMUM_SPEED_MULTIPLIER) {
            Util.sendCommandSenderMessage(commandSender, "§4Velocidade muito baixa! §6Mínimo: §c" + MINIMUM_SPEED_MULTIPLIER);
            return false;
        } else if(speed > MAXIMUM_SPEED_MULTIPLIER) {
            Util.sendCommandSenderMessage(commandSender, "§4Velocidade muito alta! §6Máximo: §c" + MAXIMUM_SPEED_MULTIPLIER);
            return false;
        }
        return true;
    }

    private void setPlayerSpeed(Player player, boolean isFlySpeed, float bukkitMoveSpeed) {
        if(isFlySpeed)
            player.setFlySpeed(bukkitMoveSpeed);
        else
            player.setWalkSpeed(bukkitMoveSpeed);
    }
}
