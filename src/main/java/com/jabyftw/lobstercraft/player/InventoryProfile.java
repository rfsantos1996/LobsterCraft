package com.jabyftw.lobstercraft.player;

import com.jabyftw.lobstercraft.util.DatabaseState;
import com.jabyftw.lobstercraft.util.Util;
import com.sun.istack.internal.NotNull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
public class InventoryProfile extends Profile {

    private final ArrayList<ItemStack> remainingItems = new ArrayList<>();
    private ItemStack[] contents, armorContents;

    protected InventoryProfile(@NotNull OnlinePlayer onlinePlayer) {
        super(ProfileType.INVENTORY_PROFILE, onlinePlayer);
    }

    /**
     * @return an ArrayList with all items player is pending
     */
    public ArrayList<ItemStack> getRemainingItems() {
        return remainingItems;
    }

    /**
     * Give items to a player, storing them in case of full inventory
     *
     * @param warnPlayer true if you want the plugin to warn player about remaining items
     * @param itemStacks items to give the player
     * @return true if he received all items, false if there are remaining items
     */
    public boolean addItems(boolean warnPlayer, @NotNull final ItemStack... itemStacks) {
        Collection<ItemStack> itemStackCollection = onlinePlayer.getPlayer().getInventory().addItem(itemStacks).values();

        // If there are remaining items
        if (!itemStackCollection.isEmpty()) {
            // Store items that couldn't be given to the player
            remainingItems.addAll(itemStackCollection);

            // Warn player that he have pending items
            if (warnPlayer) onlinePlayer.getPlayer().sendMessage("§cVocê tem itens pendentes: use §6/itens§c para recebê-los.");
        }

        return itemStackCollection.isEmpty();
    }

    /*
     * Profile overridden methods
     */

    @Override
    protected void onLoadingFromDatabase(@NotNull Connection connection, @NotNull OnlinePlayer onlinePlayer) throws SQLException {
        // Prepare statement and execute query
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `minecraft`.`player_inventories` WHERE `user_playerId` = ?;");
        preparedStatement.setInt(1, playerId);
        ResultSet resultSet = preparedStatement.executeQuery();

        // Check if there is a database entry
        if (resultSet.next()) {
            // Parse bytes back to ItemStacks
            try {
                contents = Util.byteArrayToItemStacks(resultSet.getBytes("contents"));
                armorContents = Util.byteArrayToItemStacks(resultSet.getBytes("armor_contents"));

                byte[] remainingContentsBytes = resultSet.getBytes("remaining_contents");
                if (!resultSet.wasNull())
                    remainingItems.addAll(Arrays.asList(Util.byteArrayToItemStacks(remainingContentsBytes)));
            } catch (IOException | ClassNotFoundException exception) {
                exception.printStackTrace();
            }
            this.databaseState = DatabaseState.ON_DATABASE;
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();
    }

    @Override
    protected void onProfileApplication() {
        PlayerInventory inventory = onlinePlayer.getPlayer().getInventory();

        // Clear current and apply all items
        inventory.clear();
        if (contents != null) inventory.setContents(contents);
        if (armorContents != null) inventory.setArmorContents(armorContents);
    }

    @Override
    protected void onProfileDestruction() {
        PlayerInventory inventory = onlinePlayer.getPlayer().getInventory();

        contents = inventory.getContents();
        armorContents = inventory.getArmorContents();
        setAsModified();
    }

    @Override
    protected boolean onSavingToDatabase(@NotNull Connection connection) throws SQLException {
        // Check if it is being inserted
        boolean isInserting = this.databaseState == DatabaseState.INSERT_TO_DATABASE;
        if (this.databaseState != DatabaseState.UPDATE_DATABASE && !isInserting)
            return false; // Isn't being inserted nor updated (?) return false, something went wrong

        // Insert values
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                isInserting ? "INSERT INTO `minecraft`.`player_inventories` (`user_playerId`, `contents`, `armor_contents`, `remaining_contents`) VALUES (?, ?, ?, ?);"
                        : "UPDATE `minecraft`.`player_inventories` SET `contents` = ?, `armor_contents` = ?, `remaining_contents` = ? WHERE `user_playerId` = ?;"
        )) {
            preparedStatement.setInt(isInserting ? 1 : 4, playerId);
            int index = isInserting ? 2 : 1;

            preparedStatement.setBytes(index++, Util.itemStacksToByteArray(contents));
            preparedStatement.setBytes(index++, Util.itemStacksToByteArray(armorContents));

            // remainingContents can be null on database, lets check for that then
            byte[] byteArray; // Max length BLOB can hold is (2^16)-1 bytes
            preparedStatement.setObject(
                    index,
                    (!remainingItems.isEmpty() && (byteArray = Util.itemStacksToByteArray(remainingItems.toArray(new ItemStack[remainingItems.size()]))).length < 65535) ?
                            byteArray : null,
                    Types.BLOB
            );

            // Execute and close stuff
            preparedStatement.execute();
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        // Update database state
        this.databaseState = DatabaseState.ON_DATABASE;
        return true;
    }
}
