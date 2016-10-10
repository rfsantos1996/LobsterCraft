package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.util.Util;
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

    private final static float MAXIMUM_SPEED = 10.0f, MINIMUM_SPEED = 0.01f;

    public SpeedCommand() {
        super("speed", Permissions.PLAYER_SPEED.toString(), "Permite ao jogador mudar a sua velocidade", "/speed (fly/walk) (velocidade)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onSpeedChange(OnlinePlayer playerHandler, String type) {
        return onSpeedChange(playerHandler, type, 1f);
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onSpeedChange(OnlinePlayer playerHandler, String type, float speed) {
        Player player = playerHandler.getPlayer();
        boolean isFlySpeed = type.toLowerCase().startsWith("f") || type.toLowerCase().startsWith("v"); // "fly" "voar"

        // Check speed limits (it'll already warn the player
        if (!checkSpeedLimits(player, speed))
            return true;

        // Set player speed
        setPlayerSpeed(player, isFlySpeed, getBukkitMoveSpeed(speed, isFlySpeed));

        // Send player message
        playerHandler.getPlayer().sendMessage("§6Sua velocidade de §c" + (isFlySpeed ? "vôo" : "andar") + "§6 foi alterada para §c" + (speed == 1f ? "o padrão" : speed));
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_SPEED_OTHERS)
    private boolean onSpeedChangeOthers(CommandSender commandSender, OnlinePlayer target, String type, float speed) {
        Player targetPlayer = target.getPlayer();
        boolean isFlySpeed = type.toLowerCase().startsWith("f") || type.toLowerCase().startsWith("v"); // "fly" "voar"

        // Check speed limits (it'll already warn the targetPlayer
        if (!checkSpeedLimits(commandSender, speed))
            return true;

        // Set targetPlayer speed
        setPlayerSpeed(targetPlayer, isFlySpeed, getBukkitMoveSpeed(speed, isFlySpeed));

        // Send targetPlayer message
        target.getPlayer().sendMessage(Util.appendStrings("§6Sua velocidade de §c", isFlySpeed ? "vôo" : "andar", "§6 foi alterada para §c",
                (speed == 1f ? "o padrão" : speed), "§6 por §c", commandSender.getName()));
        commandSender.sendMessage(Util.appendStrings("§6Velocidade de ", targetPlayer.getDisplayName(), "§6 de §c", isFlySpeed ? "vôo" : "andar", "§6 foi alterada para §c",
                (speed == 1f ? "o padrão" : speed)));
        return true;
    }

    /**
     * 1 is the default shown speed (both fly and walking speed) but the default flySpeed and walkSpeed are, respectively, 0.2f and 0.1f
     *
     * @param shownSpeed "our" speed / the multiplier
     * @param isFlySpeed consider as fly speed
     * @return the Bukkit's (Minecraft's) speed
     */
    private float getBukkitMoveSpeed(float shownSpeed, boolean isFlySpeed) {
        return (isFlySpeed ? 0.2f : 0.1f) * (shownSpeed / (isFlySpeed ? 2.0f : 1.0f));
    }

    private boolean checkSpeedLimits(CommandSender commandSender, float speed) {
        if (Math.abs(speed) < MINIMUM_SPEED) {
            commandSender.sendMessage(Util.appendStrings("§4Velocidade muito baixa! §6Mínimo: §c", Util.formatDecimal(MINIMUM_SPEED)));
            return false;
        } else if (Math.abs(speed) > MAXIMUM_SPEED) {
            commandSender.sendMessage(Util.appendStrings("§4Velocidade muito alta! §6Máximo: §c", Util.formatDecimal(MAXIMUM_SPEED)));
            return false;
        }
        return true;
    }

    private void setPlayerSpeed(Player player, boolean isFlySpeed, float moveSpeed) {
        if (isFlySpeed)
            player.setFlySpeed(moveSpeed);
        else
            player.setWalkSpeed(moveSpeed);
    }
}
