package com.jabyftw.lobstercraft.commands.player;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Iterator;

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
public class KillEntitiesCommand extends CommandExecutor {

    public KillEntitiesCommand() {
        super("killall", Permissions.PLAYER_KILL_ENTITIES.toString(), "Permite ao jogador matar entidades", "/killall (tipo de criatura)");
    }

    @CommandHandler(senderType = SenderType.BOTH)
    private boolean onKillAll(CommandSender commandSender, EntityType entityType) {
        if (entityType == EntityType.PLAYER || !entityType.isAlive()) {
            commandSender.sendMessage("§cVocê não pode matar este tipo de entidade.");
        }

        ArrayList<LivingEntity> killedEntities = new ArrayList<>();

        // Filter desired entities
        for (World world : Bukkit.getWorlds())
            //noinspection Convert2streamapi
            for (Entity entity : world.getEntitiesByClass(entityType.getEntityClass()))
                killedEntities.add((LivingEntity) entity); // Must be a living entity (based on the definition of isAlive())

        int entities = killedEntities.size();
        Iterator<LivingEntity> iterator = killedEntities.iterator();

        // Iterate through all entities
        while (iterator.hasNext()) {
            LivingEntity next = iterator.next();
            // Remove entity from the world and from the list
            next.remove();
            iterator.remove();
        }

        commandSender.sendMessage(Util.appendStrings("§cVocê matou §4", entities, " ", entityType.name().toLowerCase().replaceAll("_", " ")));
        return true;
    }
}
