package com.jabyftw.easiercommands;

import com.jabyftw.pacocacraft.player.UserProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

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
public class CommandExecutor implements org.bukkit.command.CommandExecutor {

    private static final boolean DEBUG = false;
    private static final Class<?> PLAYER_CLASS = UserProfile.class;
    private static String TITLE = "";

    private static final Logger logger = Bukkit.getLogger();

    static {
        for(int i = 0; i < 7; i++) {
            TITLE += '=';
        }
    }

    protected CommandExecutor(final JavaPlugin plugin, final String name, final String description, final String usageMessage) {
        final org.bukkit.command.CommandExecutor executor = this;
        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> ((PluginCommand) plugin.getCommand(name)
                .setDescription(description)
                .setUsage(usageMessage)
                .setPermissionMessage("§cVocê não tem permissão para isto!"))
                .setExecutor(executor), 2);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] stringArguments) {
        if(command.getPermission() != null && command.getPermission().length() > 0 && !commandSender.hasPermission(command.getPermission())) {
            commandSender.sendMessage(getColoredMessage(command.getPermissionMessage()));
            return true;
        }

        if(DEBUG) logger.info(TITLE);
        if(DEBUG)
            logger.info("Command used: " + stringArguments.length + " arguments -> /" + label + " " + Arrays.toString(stringArguments));
        Argument[] objectArguments = new Argument[stringArguments.length];
        {
            for(int i = 0; i < stringArguments.length; i++) {
                objectArguments[i] = ArgumentType.handleArgument(commandSender, stringArguments[i]);
                if(DEBUG)
                    logger.info("Handled argument: " + (i + 1) + "/" + stringArguments.length + ": " + objectArguments[i].toString());
            }
        }

        Method mostNear = null;
        int nearValue = Integer.MAX_VALUE;
        LinkedList<Object> objects = new LinkedList<>();

        for(Method currentMethod : getClass().getDeclaredMethods()) {
            if(DEBUG) logger.info(TITLE.replaceAll("=", "*"));
            if(currentMethod.isAnnotationPresent(CommandHandler.class)) {

                CommandHandler declaredAnnotation = currentMethod.getDeclaredAnnotation(CommandHandler.class);
                String additionalPermission = declaredAnnotation.additionalPermission();

                if(declaredAnnotation.senderType().canHandleCommandSender(commandSender) && (additionalPermission.length() == 0 || commandSender.hasPermission(additionalPermission))) {
                    Class<?>[] requiredArguments = currentMethod.getParameterTypes();

                    boolean isPlayerNeeded;

                    if(requiredArguments.length > 0 && currentMethod.getReturnType().isAssignableFrom(HandleResponse.class) &&
                            ((isPlayerNeeded = requiredArguments[0].isAssignableFrom(PLAYER_CLASS)) || requiredArguments[0].isAssignableFrom(CommandSender.class))) {

                        {
                            LinkedList<Object> objectList = new LinkedList<>();

                            if(objectArguments.length < (requiredArguments.length - 1)) {
                                if(DEBUG) logger.info(currentMethod.getName() + " -> not enough arguments");
                                continue; // Not enough arguments
                            }

                            Object player = null;

                            if(isPlayerNeeded && !(commandSender instanceof Player)) {
                                if(DEBUG) logger.info(currentMethod.getName() + " -> player is incompatible again");
                                continue; // Can't handle this command on console
                            } else if(isPlayerNeeded) {
                                player = getPlayerClass(commandSender); // Change here the method to get the player class
                            }

                            {
                                objectList.add(isPlayerNeeded ? player : commandSender);
                            }

                            Integer integer = doLoop(objectList, requiredArguments, objectArguments);

                            if(DEBUG)
                                logger.info(currentMethod.getName() + " -> ** RESULT: " + integer + "/" + nearValue + " **");
                            if(integer != null && integer <= nearValue) {
                                mostNear = currentMethod;
                                nearValue = integer;

                                objects.clear();
                                objects.addAll(objectList);

                                if(DEBUG) logger.info(currentMethod.getName() + " -> ** most near method **");
                                if(integer == 0) {
                                    if(DEBUG)
                                        logger.info(currentMethod.getName() + " -> ** perfect method: " + getMethodArguments(currentMethod.getParameterTypes()) + " **");
                                    break; // Break method-iterator
                                }
                            } else {
                                objectList.clear();
                            }
                        }
                    } else if(DEBUG)
                        logger.info(currentMethod.getName() + " -> player is incompatible with first argument or method is invalid (" + PLAYER_CLASS.getName() + ")");
                } else if(DEBUG)
                    logger.info(currentMethod.getName() + " -> Player is incompatible or don't have permission");
            }
        }
        if(DEBUG) logger.info(TITLE.replaceAll("=", "*"));

        if(mostNear == null) {
            commandSender.sendMessage(getColoredMessage(command.getUsage()));
            if(DEBUG) logger.info("Processing: -> no method available");
        } else {
            try {
                if(DEBUG)
                    logger.info("Processing: -> invoking method: " + mostNear.getName() + " (" + getMethodArguments(mostNear.getParameterTypes()) + " -> " + getMethodArguments(getClassesFromList(objects)) + ") " +
                            "with " + objects.size() + "/" + mostNear.getParameterCount() + " arguments");
                HandleResponse handleResponse = (HandleResponse) mostNear.invoke(this, objects.toArray());

                switch(handleResponse) {
                    case RETURN_HELP:
                        commandSender.sendMessage(getColoredMessage(command.getUsage()));
                        break;
                    case RETURN_NO_PERMISSION:
                        commandSender.sendMessage(getColoredMessage(command.getPermissionMessage()));
                        break;
                }

            } catch(IllegalAccessException | InvocationTargetException | ClassCastException e) {
                e.printStackTrace();
            }
        }

        if(DEBUG) logger.info(TITLE);
        return true;
    }

    private UserProfile getPlayerClass(CommandSender commandSender) {
        return (UserProfile) commandSender;
    }

    private String getMethodArguments(Class<?>... objects) {
        String[] parametersName = new String[objects.length];

        for(int i = 0; i < objects.length; i++) {
            parametersName[i] = objects[i].getSimpleName();
        }

        return Arrays.toString(parametersName);
    }

    private Class<?>[] getClassesFromList(Collection<Object> objects) {
        Class<?>[] classes = new Class<?>[objects.size()];

        Iterator<Object> iterator = objects.iterator();
        int size = objects.size();
        int index = 0;

        while(iterator.hasNext() && size >= 0) {
            classes[index] = iterator.next().getClass();
            index++;
            size--;
        }

        return classes;
    }

    private Integer doLoop(LinkedList<Object> objectList, Class<?>[] requiredArguments, Argument[] objectArguments) {
        int objectIndex = 0;
        int argumentIndex = 1;

        for(; argumentIndex < requiredArguments.length; argumentIndex++) {
            Class<?> currentArgument = requiredArguments[argumentIndex];

            if(currentArgument.isArray()) { // If is array
                LinkedList<Object> objectArray = new LinkedList<>();

                while(objectIndex < objectArguments.length) { // While you have unprocessed objects
                    Object currentObject;

                    if((currentObject = objectArguments[objectIndex].getArgument(currentArgument)) != null) { // Add them until its over, or they're not compatible
                        objectArray.add(currentObject);
                        objectIndex++;
                    } else { // Not compatible, break
                        break;
                    }
                }

                objectList.add(objectArray.toArray());

                if(objectIndex >= objectArguments.length && argumentIndex < requiredArguments.length) // don't have more arguments
                    return null; // Return not enough arguments
            } else {
                Object currentObject;

                if(objectIndex < objectArguments.length) {
                    if((currentObject = objectArguments[objectIndex].getArgument(currentArgument)) != null) {
                        objectList.add(currentObject);
                        objectIndex++;
                    } else {
                        return null; // Incompatible
                    }
                } else {
                    return null; // Not enough arguments
                }
            }
        }

        return objectArguments.length - objectIndex; // 0 when all arguments were used, > 0 when there are remaining arguments
    }

    /* Some Util imported from my projects */

    public static String getColoredMessage(String original) {
        return ChatColor.translateAlternateColorCodes('&', original);
    }
}
