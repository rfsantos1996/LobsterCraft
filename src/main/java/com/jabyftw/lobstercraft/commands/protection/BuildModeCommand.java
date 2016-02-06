package com.jabyftw.lobstercraft.commands.protection;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.util.AdministratorBuildMode;
import com.jabyftw.lobstercraft.player.util.BuildMode;
import com.jabyftw.lobstercraft.player.util.ConditionController;
import com.jabyftw.lobstercraft.player.util.Permissions;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.jabyftw.lobstercraft.util.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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
public class BuildModeCommand extends CommandExecutor {

    private final List<String> listAliases = Arrays.asList("list", "listar", "nomes", "lista"),
            changeAliases = Arrays.asList("modify", "build", "construir", "alterar", "modificar", "change", "edit", "editar"),
            createAliases = Arrays.asList("criar", "create", "new", "novo", "nova"),
            deleteAliases = Arrays.asList("delete", "deletar", "remover", "excluir", "exclude", "del"),
            exitAliases = Arrays.asList("leave", "sair", "exit", "quit");

    public BuildModeCommand() {
        super(
                "buildmode",
                Permissions.PROTECTION_ADMINISTRATOR_BUILD_MODE,
                "Permite ao jogador construir no modo administrador",
                "/blockmode (criar/construir/sair/deletar/listar) (nome identificador para construção)"
        );
    }

    @CommandHandler(senderType = SenderType.BOTH)
    public HandleResponse onBuildMode(CommandSender sender, String[] commands) {
        if (commands.length < 1) {
            if (sender instanceof Player)
                sender.sendMessage("§6Seu modo de construção é §c" + LobsterCraft.playerHandlerService.getPlayerHandler((Player) sender).getProtectionType().name());
            return HandleResponse.RETURN_HELP;
        }

        String firstArgument = commands[0].toLowerCase();

        // Check for each argument
        if (listAliases.contains(firstArgument))
            return onList(sender);
        else if (exitAliases.contains(firstArgument) && sender instanceof Player)
            return onExit(LobsterCraft.playerHandlerService.getPlayerHandler((Player) sender));
        else if (createAliases.contains(firstArgument) && commands.length > 1)
            return onCreate(sender, Util.removeIndexFromString(0, commands));
        else if (deleteAliases.contains(firstArgument) && commands.length > 1)
            return onDelete(sender, Util.removeIndexFromString(0, commands));
        else if (changeAliases.contains(firstArgument) && commands.length > 1 && sender instanceof Player)
            return onModify(LobsterCraft.playerHandlerService.getPlayerHandler((Player) sender), Util.removeIndexFromString(0, commands));
        else
            return HandleResponse.RETURN_HELP;
    }

    private HandleResponse onDelete(CommandSender sender, String[] arguments) {
        String constructionName = arguments[0].toLowerCase();
        Long constructionId = LobsterCraft.constructionsService.getConstructionId(constructionName);

        if (constructionId == null) {
            sender.sendMessage("§cConstrução não encontrada.");
            return HandleResponse.RETURN_TRUE;
        }

        if (sender instanceof Player) {
            PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler((Player) sender);

            if (playerHandler.getConditionController().sendMessageIfConditionReady(
                    ConditionController.Condition.DELETE_CONSTRUCTION_CHECK,
                    "§cTem certeza de que quer deletar a construção §6" + constructionName + "§c? §6Se sim, repita o comando. §4TODOS OS BLOCOS SERÃO DESPROTEGIDOS."
            ))
                return HandleResponse.RETURN_TRUE;
        }

        BukkitScheduler.runTaskAsynchronously(() -> {
            try {
                LobsterCraft.constructionsService.deleteConstruction(constructionName, constructionId);
                sender.sendMessage("§6Todos os blocos foram desprotegidos, a construção §c" + constructionName + "§6 foi deletada.");
            } catch (SQLException e) {
                e.printStackTrace();
                sender.sendMessage("§cOcorreu um erro no banco de dados.");
            }
        });
        return HandleResponse.RETURN_TRUE;
    }

    private HandleResponse onExit(PlayerHandler playerHandler) {
        if (playerHandler.getBuildMode() != BuildMode.DEFAULT) {
            playerHandler.setBuildMode(null);
            playerHandler.sendMessage("§6Você agora está construindo como jogador.");
        } else {
            playerHandler.sendMessage("§cVocê já está no modo de construção padrão.");
        }
        return HandleResponse.RETURN_TRUE;
    }

    private HandleResponse onModify(PlayerHandler playerHandler, String[] arguments) {
        // Search construction Id
        Long constructionId = LobsterCraft.constructionsService.getConstructionId(arguments[0]);

        // Check if is a valid construction
        if (constructionId == null) {
            // Warn player
            playerHandler.sendMessage("§4Não foi possível encontrar a construção.§c Use §6/buildmode list");
        } else {
            // Set up administrator mode with desired constructionId
            playerHandler.setBuildMode(new AdministratorBuildMode(constructionId));
            playerHandler.sendMessage("§6Você está construindo para §c" + arguments[0].toLowerCase() + "§6. §cTodos seus blocos serão protegidos em nome da construção.");
        }
        return HandleResponse.RETURN_TRUE;
    }

    private HandleResponse onCreate(CommandSender sender, String[] arguments) {
        if (LobsterCraft.permission.has(sender, Permissions.PROTECTION_CREATE_ADMINISTRATOR_BUILDINGS)) {
            String constructionName = arguments[0].toLowerCase();

            // Check if it is a valid name
            if (!Util.checkStringCharactersAndLength(constructionName, 3, 45)) {
                sender.sendMessage("§cNome inválido! Deve conter entre 3 e 45 letras, sem espaços.");
                return HandleResponse.RETURN_TRUE;
            }

            // Search construction Id
            Long searchId = LobsterCraft.constructionsService.getConstructionId(constructionName);

            if (searchId != null) {
                // Warn player that construction exists
                sender.sendMessage("§cJá existe uma construção com este nome. §6Use §c/buildmode alterar " + constructionName + "§6 para construir lá.");
                return HandleResponse.RETURN_TRUE;
            }

            // Register construction, initialize player (if sender is a Player)
            BukkitScheduler.runTaskAsynchronously(() -> {
                FutureTask<Long> registerTask = LobsterCraft.constructionsService.registerConstruction(constructionName);
                // Note: constructionId will be inserted on a ScheduledTask, so we need to use it on a scheduled task too
                registerTask.run();
                try {
                    Long constructionId = registerTask.get();

                    // Check if its a valid constructionId
                    if (constructionId == null) throw new NullPointerException("constructionId is null");

                    // Run synchronously as it is required by constructionId database
                    BukkitScheduler.runTask(() -> {
                        // If sender is a player and have permission to construct
                        if (sender instanceof Player && LobsterCraft.permission.has(sender, Permissions.PROTECTION_ADMINISTRATOR_BUILD_MODE)) {
                            PlayerHandler playerHandler = LobsterCraft.playerHandlerService.getPlayerHandler((Player) sender);

                            // Set as constructing if has permission
                            playerHandler.setBuildMode(new AdministratorBuildMode(constructionId));
                            playerHandler.sendMessage("§6Você agora está construindo para §c" + constructionName + "§6. §cTodos os seus blocos serão protegidos em nome da construção.");
                        } else {
                            sender.sendMessage("§6Construção criada! Avise construtores que agora são permitidos a construir.");
                        }
                    });
                } catch (InterruptedException | ExecutionException | NullPointerException e) {
                    e.printStackTrace();
                    sender.sendMessage("§4Ocorreu um erro! §cNão foi possível criar a construção.");
                }
            });
            return HandleResponse.RETURN_TRUE;
        } else {
            return HandleResponse.RETURN_NO_PERMISSION;
        }
    }

    private HandleResponse onList(CommandSender sender) {
        Set<String> constructionSet = LobsterCraft.constructionsService.getConstructionSet();

        // Check if there are constructions
        if (constructionSet.isEmpty()) {
            sender.sendMessage("§cAinda não há nenhuma construção disponível!");
            return HandleResponse.RETURN_TRUE;
        }

        StringBuilder stringBuilder = new StringBuilder("§6Construções criadas: ");

        // Iterate through all items
        Iterator<String> iterator = constructionSet.iterator();

        while (iterator.hasNext()) {
            // Append colored name and a comma
            stringBuilder.append("§c").append(iterator.next());
            if (iterator.hasNext()) stringBuilder.append("§6, ");
        }

        // Send message to player
        sender.sendMessage(stringBuilder.toString());
        return HandleResponse.RETURN_TRUE;
    }
}
