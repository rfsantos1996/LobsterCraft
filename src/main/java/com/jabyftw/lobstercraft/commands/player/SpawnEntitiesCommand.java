package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.List;

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
public class SpawnEntitiesCommand extends CommandExecutor {

    private final static HashSet<Material> nonSolidMaterials = new HashSet<>();

    static {
        // Exclude non-solid materials
        for (Material material : Material.values()) {
            if (!material.isSolid()) nonSolidMaterials.add(material);
        }
    }

    public SpawnEntitiesCommand() {
        super("spawnmob", Permissions.PLAYER_SPAWN_MOBS.toString(), "Permite ao jogador obter animais e monstros", "/spawnmob (tipo de criatura) (quantidade)");
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onSpawnEntities(OnlinePlayer onlinePlayer, EntityType entityType) {
        return onSpawnEntities(onlinePlayer, entityType, 1);
    }

    @CommandHandler(senderType = SenderType.PLAYER)
    private boolean onSpawnEntities(OnlinePlayer onlinePlayer, EntityType entityType, int amount) {
        if (amount <= 0) {
            onlinePlayer.getPlayer().sendMessage("§cQuantidade inadequada!");
            return true;
        }

        List<Block> lineOfSight = onlinePlayer.getPlayer().getLineOfSight(nonSolidMaterials, 100);

        // Check for the closer
        Location playerLocation = onlinePlayer.getPlayer().getLocation();
        Block closerBlock = null;
        double closerDistance = Double.MAX_VALUE;

        for (Block block : lineOfSight) {
            double currentDistance = block.getLocation().distanceSquared(playerLocation);

            // Check if it is closer and update variables
            if (currentDistance < closerDistance) {
                closerBlock = block;
                closerDistance = currentDistance;
            }
        }

        // If closer block isn't null, spawn mob there; else warn player
        if (closerBlock != null) {
            Location spawnLocation = closerBlock.getLocation().add(0, 1, 0);

            // Spawn mobs and warn player
            if (spawnMob(spawnLocation, entityType, amount))
                onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§c", amount, "§6 de §c", entityType.name().toLowerCase().replaceAll("_", " "),
                        (amount > 1 ? "§6 foi criado." : "§6 foram criados")));
            else
                onlinePlayer.getPlayer().sendMessage("§cNão foi possível criar.");
        } else {
            onlinePlayer.getPlayer().sendMessage("§cNão foi encontrado nenhum bloco!");
        }
        return true;
    }

    @CommandHandler(senderType = SenderType.BOTH, additionalPermissions = Permissions.PLAYER_SPAWN_MOBS_OTHERS)
    private boolean onSpawnEntitiesOnOthers(CommandSender commandSender, OnlinePlayer onlinePlayer, EntityType entityType) {
        return onSpawnEntitiesOnOthers(commandSender, onlinePlayer, entityType, 1);
    }

    private boolean onSpawnEntitiesOnOthers(CommandSender commandSender, OnlinePlayer onlinePlayer, EntityType entityType, int amount) {
        if (amount <= 0) {
            commandSender.sendMessage("§cQuantidade inadequada!");
            return true;
        }

        if (spawnMob(onlinePlayer.getPlayer().getLocation().add(0, 1, 0), entityType, amount)) {

            onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§6", commandSender.getName(), " criou §c", amount, "§6 de §c",
                    entityType.name().toLowerCase().replaceAll("_", ""), "§6 na sua localização"));

            commandSender.sendMessage(Util.appendStrings("§6Foi criado §c", amount, "§6 de §c", entityType.name().toLowerCase().replaceAll("_", ""),
                    "§6 na localização de ", onlinePlayer.getPlayer().getDisplayName()));
        } else {
            commandSender.sendMessage(Util.appendStrings("§cNão foi possível criar ", entityType.name().toLowerCase().replaceAll("_", " "), "."));
        }
        return true;
    }

    private boolean spawnMob(Location spawnLocation, EntityType entityType, int amount) {
        for (int i = 0; i < amount; i++) {
            Entity entity = spawnLocation.getWorld().spawnEntity(spawnLocation, entityType);
            if (entity == null) return false;
        }
        return true;
    }
}
