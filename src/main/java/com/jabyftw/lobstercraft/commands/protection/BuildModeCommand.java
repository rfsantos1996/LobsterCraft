package com.jabyftw.lobstercraft.commands.protection;

import com.jabyftw.easiercommands.CommandExecutor;
import com.jabyftw.easiercommands.CommandHandler;
import com.jabyftw.easiercommands.HandleResponse;
import com.jabyftw.easiercommands.SenderType;
import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.player.OnlinePlayer;
import com.jabyftw.lobstercraft.player.TriggerController;
import com.jabyftw.lobstercraft.Permissions;
import com.jabyftw.lobstercraft.services.services_event.PlayerChangesBuildingModeEvent;
import com.jabyftw.lobstercraft.util.Util;
import com.jabyftw.lobstercraft.world.BlockProtectionType;
import com.jabyftw.lobstercraft.world.BuildingMode;
import com.jabyftw.lobstercraft.world.CityStructure;
import com.jabyftw.lobstercraft.world.WorldService;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;

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

    // General command aliases
    private final List<String>
            administratorAliases = Arrays.asList("admin", "adm", "administrator", "construction", "construct", "build", "construcao");

    // Player command aliases
    private final List<String>
            cityAliases = Arrays.asList("cidade", "city"),
            houseAliases = Arrays.asList("casa", "house"),
            playerAliases = Arrays.asList("player", "individual", "individ");

    // Administrator command aliases
    private final List<String>
            listConstructionsAliases = Arrays.asList("list", "listar", "nomes", "lista"),
            createConstructionsAliases = Arrays.asList("criar", "create", "new", "novo", "nova"),
            changeConstructionsAliases = Arrays.asList("modify", "build", "construir", "alterar", "modificar", "change", "edit", "editar"),
            removeConstructionsAliases = Arrays.asList("delete", "deletar", "remover", "remove", "excluir", "exclude", "del");

    public BuildModeCommand() {
        super(
                "buildmode",
                Permissions.WORLD_PROTECTION_CHANGE_BUILD_MODE.toString(),
                "Permite ao jogador construir no modo administrador",
                "/blockmode (cidade/casa/individual/administrador)"
        );
    }

    @CommandHandler(senderType = SenderType.BOTH)
    private HandleResponse onBaseCommand(CommandSender sender, String[] commands) {
        if (commands.length < 1) {
            if (sender instanceof Player) {
                BuildingMode buildingMode = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) sender, OnlinePlayer.OnlineState.LOGGED_IN).getBuildingMode();
                sender.sendMessage(buildingMode != null ?
                        Util.appendStrings("§6Seu modo de proteção é de §c", buildingMode.getBlockProtectionType().getDisplayName()) :
                        "§cModo de construção não definido!"
                );
            }
            return HandleResponse.RETURN_HELP;
        }

        String firstArgument = commands[0].toLowerCase();

        // Check for administrator commands
        if (administratorAliases.contains(firstArgument)) {
            if (LobsterCraft.permission.has(sender, Permissions.WORLD_PROTECTION_CHANGE_BUILD_MODE_ADMINISTRATOR.toString())) {
                if (commands.length >= 2) {
                    String secondArgument = commands[1].toLowerCase();
                    if (listConstructionsAliases.contains(secondArgument)) {
                        return onListConstructions(sender);
                    } else if (commands.length >= 3) {
                        String thirdArgument = commands[2].toLowerCase();
                        if (createConstructionsAliases.contains(secondArgument))
                            return onCreateConstructions(sender, thirdArgument);
                        else if (removeConstructionsAliases.contains(secondArgument))
                            return onRemoveConstructions(sender, thirdArgument);
                        else if (changeConstructionsAliases.contains(secondArgument) && sender instanceof Player)
                            return onModifyConstruction(LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) sender, OnlinePlayer.OnlineState.LOGGED_IN),
                                    thirdArgument);
                    }
                }
                // Help for administrator
                sender.sendMessage("§6Uso: §c/blockmode admin (criar/modificar/remover/listar) (identificador para construção - nome)");
                return HandleResponse.RETURN_TRUE;
            } else {
                return HandleResponse.RETURN_NO_PERMISSION;
            }
        }

        // Check for player commands
        if (sender instanceof Player) {
            OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) sender, OnlinePlayer.OnlineState.LOGGED_IN);
            if (houseAliases.contains(firstArgument))
                return onBuildMode(onlinePlayer, BlockProtectionType.CITY_HOUSES);
            else if (cityAliases.contains(firstArgument))
                return onBuildMode(onlinePlayer, BlockProtectionType.CITY_BLOCKS);
            else if (playerAliases.contains(firstArgument))
                return onBuildMode(onlinePlayer, BlockProtectionType.PLAYER_BLOCKS);
            else
                return HandleResponse.RETURN_HELP;
        } else {
            return HandleResponse.RETURN_HELP;
        }
    }

    private HandleResponse onBuildMode(OnlinePlayer onlinePlayer, BlockProtectionType protectionType) {
        if (protectionType == BlockProtectionType.CITY_HOUSES || protectionType == BlockProtectionType.CITY_BLOCKS) {
            CityStructure cityStructure = onlinePlayer.getOfflinePlayer().getCity();

            // Check if player has a city
            if (cityStructure != null) {
                if (protectionType == BlockProtectionType.CITY_HOUSES) {
                    CityStructure.CityHouse cityHouse = cityStructure.getHouseFromPlayer(onlinePlayer.getOfflinePlayer().getPlayerId());

                    // Check if player has a house
                    if (cityHouse != null)
                        setBuildMode(onlinePlayer, BlockProtectionType.CITY_HOUSES, cityHouse.getHouseId());
                    else
                        onlinePlayer.getPlayer().sendMessage("§cVocê não possui uma casa nesta cidade!");

                    return HandleResponse.RETURN_TRUE;
                } else
                    // protectionType is CITY_BLOCKS
                    if (onlinePlayer.getOfflinePlayer().getCityOccupation().canChangeCity()) {
                        setBuildMode(onlinePlayer, BlockProtectionType.CITY_BLOCKS, cityStructure.getCityId());
                        return HandleResponse.RETURN_TRUE;
                    } else {
                        onlinePlayer.getPlayer().sendMessage("§cSua ocupação na cidade não permite edita-la!");
                        return HandleResponse.RETURN_TRUE;
                    }
            } else {
                onlinePlayer.getPlayer().sendMessage("§cVocê não está em uma cidade!");
                return HandleResponse.RETURN_TRUE;
            }
        } else if (protectionType == BlockProtectionType.PLAYER_BLOCKS) {
            setBuildMode(onlinePlayer, BlockProtectionType.PLAYER_BLOCKS, onlinePlayer.getOfflinePlayer().getPlayerId());
            return HandleResponse.RETURN_TRUE;
        } else {
            return HandleResponse.RETURN_HELP;
        }
    }

    private HandleResponse onModifyConstruction(OnlinePlayer onlinePlayer, String identifier) {
        Integer constructionId = LobsterCraft.servicesManager.worldService.getConstructionId(identifier);

        // Set block construction mode
        if (constructionId != null)
            setBuildMode(onlinePlayer, BlockProtectionType.ADMINISTRATOR_BLOCKS, constructionId);
        else
            onlinePlayer.getPlayer().sendMessage(Util.appendStrings("§4Construção §C\"", identifier, "\"§4 não encontrada! §cVeja a lista com /bm adm list"));

        return HandleResponse.RETURN_TRUE;
    }

    private void setBuildMode(@NotNull OnlinePlayer onlinePlayer, @NotNull BlockProtectionType blockProtectionType, @Nullable Integer protectionId) {
        // Build future building mode
        BuildingMode futureBuildingMode = new BuildingMode(blockProtectionType, protectionId, null);

        // Call event
        PlayerChangesBuildingModeEvent changesBuildingModeEvent = new PlayerChangesBuildingModeEvent(onlinePlayer, futureBuildingMode);
        Bukkit.getPluginManager().callEvent(changesBuildingModeEvent);
    }

    private HandleResponse onListConstructions(CommandSender sender) {
        // Check if player has permission
        if (!LobsterCraft.permission.has(sender, Permissions.WORLD_PROTECTION_CHANGE_BUILD_MODE_ADMINISTRATOR.toString()))
            return HandleResponse.RETURN_NO_PERMISSION;

        Set<Map.Entry<String, Integer>> constructions = LobsterCraft.servicesManager.worldService.getConstructions();

        // Check if there are constructions
        if (constructions.isEmpty()) {
            sender.sendMessage("§cAinda não há nenhuma construção disponível!");
            return HandleResponse.RETURN_TRUE;
        }

        StringBuilder stringBuilder = new StringBuilder("§6Construções criadas: ");

        // Iterate through all items
        boolean first = true;
        for (Map.Entry<String, Integer> entry : constructions) {
            // Append comma if isn't the first one
            if (!first) stringBuilder.append("§6, ");
            first = false;
            // Append colored name and id
            stringBuilder.append("§c").append(entry.getValue()).append(" (").append(entry.getValue()).append(")");
        }

        // Send message to player
        sender.sendMessage(stringBuilder.toString());
        return HandleResponse.RETURN_TRUE;
    }

    private HandleResponse onCreateConstructions(CommandSender sender, final String constructionName) {
        // Check if player has permission
        if (!LobsterCraft.permission.has(sender, Permissions.WORLD_PROTECTION_CREATE_BUILD_MODE_ADMINISTRATOR.toString()))
            return HandleResponse.RETURN_NO_PERMISSION;

        Bukkit.getScheduler().runTaskAsynchronously(
                LobsterCraft.plugin,
                () -> {
                    try {
                        WorldService.ConstructionCreationResponse response = LobsterCraft.servicesManager.worldService.createConstruction(constructionName);

                        if (response == WorldService.ConstructionCreationResponse.NAME_INVALID)
                            sender.sendMessage("§cNome inválido! Deve conter entre 3 e 45 letras, sem espaços.");
                        else if (response == WorldService.ConstructionCreationResponse.NAME_MATCHED_ANOTHER)
                            sender.sendMessage(Util.appendStrings("§cJá existe uma construção com este nome. §6Use §c/buildmode construir ", constructionName, "§6 para construí-la."));
                        else if (response == WorldService.ConstructionCreationResponse.SUCCESSFULLY_CREATED)
                            sender.sendMessage("§6Construção criada! Os construtores podem agora modificá-la.");
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                        sender.sendMessage("§cAlgo falhou no banco de dados!");
                    }
                }
        );
        return HandleResponse.RETURN_TRUE;
    }

    private HandleResponse onRemoveConstructions(CommandSender sender, final String constructionName) {
        // Check if player has permission
        if (!LobsterCraft.permission.has(sender, Permissions.WORLD_PROTECTION_REMOVE_BUILD_MODE_ADMINISTRATOR.toString()))
            return HandleResponse.RETURN_NO_PERMISSION;

        final Integer constructionId = LobsterCraft.servicesManager.worldService.getConstructionId(constructionName);

        // Find construction
        if (constructionId == null) {
            sender.sendMessage("§cConstrução não encontrada.");
            return HandleResponse.RETURN_TRUE;
        }

        // Check for player condition: use the command at least 2 times
        if (sender instanceof Player) {
            OnlinePlayer onlinePlayer = LobsterCraft.servicesManager.playerHandlerService.getOnlinePlayer((Player) sender, OnlinePlayer.OnlineState.LOGGED_IN);

            // Check for player second use
            if (onlinePlayer.getTriggerController().sendMessageIfNotTriggered(
                    TriggerController.TemporaryTrigger.DELETE_CONSTRUCTION_CHECK,
                    Util.appendStrings("§cTem certeza de que quer remover a construção §6", constructionName, "§c? §6Se sim, repita o comando. §4TODOS OS BLOCOS SERÃO DESPROTEGIDOS.")
            ))
                return HandleResponse.RETURN_TRUE;
        }

        // Remove asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(LobsterCraft.plugin, () -> {
            try {
                sender.sendMessage(LobsterCraft.servicesManager.worldService.removeConstruction(constructionId) ?
                        Util.appendStrings("§6Todos os blocos foram desprotegidos e a construção §c", constructionName, "§6 foi removida.")
                        : "§cOcorreu um erro ao remover construção.");
            } catch (SQLException exception) {
                exception.printStackTrace();
                sender.sendMessage("§cOcorreu um erro no banco de dados.");
            }
        });
        return HandleResponse.RETURN_TRUE;
    }
}
