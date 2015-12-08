package com.jabyftw.easiercommands;

import com.jabyftw.pacocacraft.PacocaCraft;
import com.jabyftw.pacocacraft.player.PlayerHandler;
import com.jabyftw.pacocacraft.util.BukkitScheduler;
import com.sun.istack.internal.NotNull;
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

    // TODO: change to your 'Player' class (can be Bukkit's Player)
    public static final Class<?> PLAYER_CLASS = PlayerHandler.class;

    // Debug stuff
    private static final boolean debug = false;
    private static final Logger logger = PacocaCraft.logger;

    /**
     * Create a command executor given attributes
     * NOTE: the command is still required at plugin.yml!
     *
     * @param plugin       plugin that registered the command
     * @param name         command's name so the executor can be set
     * @param description  command's description
     * @param usageMessage command's usage message
     */
    public CommandExecutor(final @NotNull JavaPlugin plugin, final @NotNull String name, final String permission, final String description, final String usageMessage) {
        final org.bukkit.command.CommandExecutor executor = this;
        PluginCommand command = plugin.getCommand(name);
        // Check command's existence
        if(command == null || name.length() == 0)
            throw new IllegalArgumentException("You must register the command on plugin.yml!");

        ((PluginCommand) command
                .setDescription(description)
                .setUsage("§6Uso: " + usageMessage)
                .setPermissionMessage("§cVocê não tem permissão para isto!"))
                .setExecutor(executor);
        command.setPermission(permission);
    }

    /**
     * Get a player class instance from command sender
     *
     * @param player command sender given from command (it is a player, previously checked for you)
     *
     * @return the player class you support
     */
    private Object getPlayerClass(@NotNull Player player) {
        // TODO change here for your 'Player' class
        return PacocaCraft.playerMap.get(player);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, Command command, String label, String[] stringArguments) {
        // If player don't have permission to the command (base one, not variables), return true with a message
        if(command.getPermission() != null && command.getPermission().length() > 0 && !commandSender.hasPermission(command.getPermission())) {
            commandSender.sendMessage(getColoredMessage(command.getPermissionMessage()));
            return true;
        }

        if(debug)
            logger.info("§4[DEBUG]§r Command used: " + stringArguments.length + " arguments: §c/" + label + " " + Arrays.toString(stringArguments));

        // Parse string arguments as object arguments (our object)
        Argument[] availableArguments = new Argument[stringArguments.length];
        for(int i = 0; i < stringArguments.length; i++) {
            availableArguments[i] = ArgumentType.handleArgument(commandSender, stringArguments[i]);
            if(debug)
                logger.info("§4[DEBUG]§r Handled argument " + (i + 1) + "/" + stringArguments.length + ": " + availableArguments[i].toString());
        }

        // Set up variables for look up the better method
        Method mostNearMethod = null;
        int nearValue = Integer.MAX_VALUE;

        // Arguments that will be sent to the method
        LinkedList<Object> objects = new LinkedList<>();

        if(debug) logger.info("§4[DEBUG]§r Started looping methods:");
        // Loop through declared methods from this class
        for(Method currentMethod : getClass().getDeclaredMethods()) {

            // If method has @CommandHandler annotation
            if(currentMethod.isAnnotationPresent(CommandHandler.class)) {

                // Check for additional information
                CommandHandler declaredAnnotation = currentMethod.getDeclaredAnnotation(CommandHandler.class);
                String additionalPermission = declaredAnnotation.additionalPermission();

                // Check if command sender can handle this command and has permission to do so
                if(declaredAnnotation.senderType().canHandleCommandSender(commandSender) && (additionalPermission.length() == 0 || commandSender.hasPermission(additionalPermission))) {
                    Class<?>[] requiredArguments = currentMethod.getParameterTypes();
                    boolean isPlayerNeeded;

                    /*
                     * Is a valid command?
                     * - Has more than one argument
                     * - Return a HandleResponse
                     * - Is player or command sender required at first argument
                     */
                    if(requiredArguments.length > 0 && currentMethod.getReturnType().isAssignableFrom(HandleResponse.class) &&
                            ((isPlayerNeeded = requiredArguments[0].isAssignableFrom(PLAYER_CLASS)) || requiredArguments[0].isAssignableFrom(CommandSender.class))) {

                        // If player didn't sent enough arguments to this command, continue searching
                        if(availableArguments.length < (requiredArguments.length - 1)) {
                            if(debug)
                                logger.info("§4[DEBUG]§r §cMethod " + currentMethod.getName() + ":§r doesn't have enough arguments (" + availableArguments.length + "/" + requiredArguments.length + "), next");
                            continue; // Not enough arguments
                        }

                        // List of arguments to be delivered to method
                        LinkedList<Object> objectList = new LinkedList<>();

                        // Check if player is needed and if command sender wasn't a player
                        Object player = null;
                        if(isPlayerNeeded) {
                            // If player is required but sender isn't a player, continue searching
                            if(!(commandSender instanceof Player)) {
                                if(debug)
                                    logger.info("§4[DEBUG]§r §cMethod " + currentMethod.getName() + ":§r sender isn't a Player");
                                continue;
                            }
                            player = getPlayerClass((Player) commandSender);
                        }
                        // Add command sender/player as first argument (must be)
                        objectList.add(isPlayerNeeded ? player : commandSender);

                        // Check arguments (null - failed, not enough or incompatible, 0 - exactly filled, > 0 - there are remaining arguments, but the command would work)
                        Integer integer = checkArgumentsToMethod(objectList, requiredArguments, availableArguments);

                        if(debug)
                            logger.info("§4[DEBUG]§r §6Method " + currentMethod.getName() + ":§r " +
                                    "§" + (integer != null ? (integer.equals(0) ? "a" + integer.toString() : "2" + integer.toString()) : "4null") + "/" + nearValue);

                        // Check if is a possible method to command execution and if it is the better so far
                        if(integer != null && integer <= nearValue) {
                            // Set as better so far
                            mostNearMethod = currentMethod;
                            nearValue = integer;

                            // Update object list to the new method
                            objects.clear();
                            objects.addAll(objectList);


                            // If the arguments fit exactly, execute the command (as the computer wouldn't know the better method between 2 'perfect fit' methods)
                            if(integer == 0) {
                                if(debug)
                                    logger.info("§4[DEBUG]§r §cMethod " + currentMethod.getName() + ":§r set as perfect method");
                                // Break method-iterator
                                break;
                            } else if(debug) {
                                logger.info("§4[DEBUG]§r §cMethod " + currentMethod.getName() + ":§r set as most near method");
                            }
                        } else {
                            // Clear object list to next method (this isn't even necessary, but...)
                            objectList.clear();
                        }

                    } else if(debug) {
                        // Invalid method requirements (HandleResponse, Player/CommandSender)
                        logger.info("§4[DEBUG]§r §cMethod " + currentMethod.getName() + ":§r player isn't the first argument (" + PLAYER_CLASS.getSimpleName() + ")");
                    }
                } else if(debug) {
                    // Invalid sender or missing permissions
                    logger.info("§4[DEBUG]§r §cMethod " + currentMethod.getName() + ":§r player is incompatible or doesn't have permission");
                }
            } // Invalid method requirements (annotation)
            if(debug) logger.info("§4[DEBUG]§r Next method!");
        }

        if(debug) logger.info("§4[DEBUG]§r Processing command now");

        // After loop: if there isn't a matchable method, return command usage
        if(mostNearMethod == null) {
            // Return command usage
            commandSender.sendMessage(getColoredMessage(command.getUsage()));
            if(debug) logger.info("§4[DEBUG]§r §cNo method is available");
            return true;
        } else {
            try {
                if(debug) {
                    logger.info("§4[DEBUG]§r Invoking method " + mostNearMethod.getName() + ": " + objects.size() + "/" + mostNearMethod.getParameterCount() + " \n" +
                            "(" + getMethodArgumentNames(mostNearMethod.getParameterTypes()) + ") -> (" + getMethodArgumentNames(getClassesFromCollectionOfObjects(objects)) + ")");
                }

                // Invoke method given arguments
                HandleResponse handleResponse = (HandleResponse) mostNearMethod.invoke(this, objects.toArray());

                // Send player response based on command's HandleResponse
                switch(handleResponse) {
                    case RETURN_HELP:
                        commandSender.sendMessage(getColoredMessage(command.getUsage()));
                        return true;
                    case RETURN_NO_PERMISSION:
                        commandSender.sendMessage(getColoredMessage(command.getPermissionMessage()));
                        return true;
                    case RETURN_TRUE:
                        return true;
                }
            } catch(IllegalAccessException | InvocationTargetException | ClassCastException e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    /**
     * Loop through object arguments (sent by player) trying to fit the required arguments (by method) filling the object list
     *
     * @param objectList        object list to give method after successful completion
     * @param requiredArguments required arguments by method
     * @param objectArguments   player sent and processed arguments
     *
     * @return null if doesn't fit
     */
    private Integer checkArgumentsToMethod(LinkedList<Object> objectList, Class<?>[] requiredArguments, Argument[] objectArguments) {
        int objectIndex = 0, argumentIndex = 1;

        // Loop through required arguments
        for(; argumentIndex < requiredArguments.length; argumentIndex++) {
            Class<?> currentArgument = requiredArguments[argumentIndex];

            // Check if current argument is an array
            if(currentArgument.isArray()) {
                // Create an array of objects that can be fit into it
                LinkedList<Object> objectArray = new LinkedList<>();

                // While exist remaining arguments
                while(objectIndex < objectArguments.length) {
                    // Get current object based on required argument
                    Object currentObject = objectArguments[objectIndex].getArgument(currentArgument);

                    // If required argument fit
                    if(currentObject != null) {
                        // Add it to the array
                        objectArray.add(currentObject);
                        objectIndex++;
                    } else {
                        // Required argument doesn't fit, break while loop
                        break;
                    }
                }

                // Add array as argument
                objectList.add(objectArray.toArray());

                // If there's no remaining object to fit into required arguments, return null (failed)
                if(objectIndex >= objectArguments.length && argumentIndex < requiredArguments.length)
                    return null;
            } else { // If it isn't an array
                Object currentObject;

                // If there is enough arguments left
                if(objectIndex < objectArguments.length) {
                    // Check compatibility with the required argument
                    if((currentObject = objectArguments[objectIndex].getArgument(currentArgument)) != null) {
                        objectList.add(currentObject);
                        objectIndex++;
                    } else {
                        // Incompatible object to the type required by method
                        return null;
                    }
                } else {
                    // Not enough arguments
                    return null;
                }
            }
        }

        /*
         * Return the difference of remaining arguments and arguments used:
         * null means there wasn't a compatible argument list to fill the method
         * = 0 means that the method was exactly filled
         * > 0 means there are remaining arguments
         */
        return objectArguments.length - objectIndex;
    }

    /**
     * Get method's argument names as a list of String
     *
     * @param objects method's parameter types
     *
     * @return a list-type string of required parameters' name
     */
    private String getMethodArgumentNames(Class<?>... objects) {
        String[] parametersName = new String[objects.length];

        for(int i = 0; i < objects.length; i++) {
            parametersName[i] = objects[i].getSimpleName();
        }

        return Arrays.toString(parametersName);
    }

    /**
     * Get an array of classes from the given collection
     *
     * @param collection collection of objects
     *
     * @return an array with object's types
     */
    private Class<?>[] getClassesFromCollectionOfObjects(Collection<Object> collection) {
        Class<?>[] classes = new Class<?>[collection.size()];

        Iterator<Object> iterator = collection.iterator();
        int size = collection.size();
        int index = 0;

        while(iterator.hasNext() && size >= 0) {
            classes[index] = iterator.next().getClass();
            index++;
            size--;
        }

        return classes;
    }

    /**
     * Parse the string as a colored message supporting the '&' character often used on configuration files
     *
     * @param original the original string
     *
     * @return colorful message considering the '&' character
     */
    public static String getColoredMessage(String original) {
        return ChatColor.translateAlternateColorCodes('&', original);
    }
}
