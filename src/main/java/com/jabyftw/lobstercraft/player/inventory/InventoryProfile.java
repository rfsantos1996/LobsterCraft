package com.jabyftw.lobstercraft.player.inventory;

import com.jabyftw.lobstercraft.player.PlayerHandler;
import com.jabyftw.lobstercraft.player.Profile;
import com.jabyftw.lobstercraft.util.DatabaseState;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Copyright (C) 2016  Rafael Sartori for LobsterCraft Plugin
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
public class InventoryProfile extends Profile {

    private final ArrayList<ItemStack> remainingContents = new ArrayList<>();
    private ItemStack[] contents, armorContents;

    public InventoryProfile(final long playerId) {
        super(playerId);
        databaseState = DatabaseState.NOT_ON_DATABASE;
    }

    public InventoryProfile(final long playerId, @NotNull final ItemStack[] contents, @NotNull final ItemStack[] armorContents, @Nullable final ItemStack[] remainingContents) {
        super(playerId);
        this.contents = contents;
        this.armorContents = armorContents;
        if (remainingContents != null)
            this.remainingContents.addAll(Arrays.asList(remainingContents));
        databaseState = DatabaseState.ON_DATABASE;
    }

    // We don't need Base64Coder as it'll be stored and not streamed
    public static byte[] itemStacksToByteArray(@NotNull final ItemStack[] contents) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream outputStream = new BukkitObjectOutputStream(byteArrayOutputStream);

        // Write the number of items
        outputStream.writeInt(contents.length);
        // Write item stacks
        for (ItemStack content : contents)
            outputStream.writeObject(content);

        // Close stuff
        outputStream.close();
        byteArrayOutputStream.close();

        // return encoded lines
        return byteArrayOutputStream.toByteArray();
    }

    public static ItemStack[] byteArrayToItemStacks(@NotNull final byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        BukkitObjectInputStream inputStream = new BukkitObjectInputStream(byteArrayInputStream);

        // initialize array with the number of items
        ItemStack[] itemStacks = new ItemStack[inputStream.readInt()];

        // Re-create item array
        for (int i = 0; i < itemStacks.length; i++) {
            itemStacks[i] = (ItemStack) inputStream.readObject();
        }

        // Close stuff
        inputStream.close();
        byteArrayInputStream.close();

        // Return the result
        return itemStacks;
    }

    public static InventoryProfile retrieveProfile(Connection connection, long playerId) throws Exception {
        // Create statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM minecraft.player_inventories WHERE user_playerId=?;");

        // Set variable
        preparedStatement.setLong(1, playerId);

        // Execute query
        ResultSet resultSet = preparedStatement.executeQuery();

        InventoryProfile inventoryProfile;

        // If there's a result
        if (resultSet.next()) {
            // Parse back to item stacks
            ItemStack[] contents = byteArrayToItemStacks(resultSet.getBytes("contents")),
                    armorContents = byteArrayToItemStacks(resultSet.getBytes("armor_contents")),
                    remainingContents = null;

            byte[] remainingContentsBytes = resultSet.getBytes("remaining_contents");
            if (!resultSet.wasNull())
                remainingContents = byteArrayToItemStacks(remainingContentsBytes);

            inventoryProfile = new InventoryProfile(playerId, contents, armorContents, remainingContents);
        } else {
            // Return default, as the result wasn't found
            inventoryProfile = new InventoryProfile(playerId);
        }

        // Close everything
        resultSet.close();
        preparedStatement.close();
        //connection.close(); // Do not close our connection since it'll be used in multiple steps

        // Return result
        return inventoryProfile;
    }

    public static boolean saveProfile(@NotNull final Connection connection, @NotNull final InventoryProfile profile) {
        try {
            // Check if it is being inserted (all profiles that come here are the ones who needs saving)
            boolean isInserting = profile.databaseState == DatabaseState.INSERT_TO_DATABASE;
            if (profile.databaseState != DatabaseState.UPDATE_DATABASE && !isInserting)
                return false; // Isn't being inserted nor updated (?) return false, something went wrong

            // Create prepared statement
            PreparedStatement preparedStatement = connection.prepareStatement(
                    isInserting ? "INSERT INTO `minecraft`.`player_inventories` (`user_playerId`, `contents`, `armor_contents`, `remaining_contents`) VALUES (?, ?, ?, ?);"
                            : "UPDATE `minecraft`.`player_inventories` SET `contents` = ?, `armor_contents` = ?, `remaining_contents` = ? WHERE `user_playerId` = ?;"
            );

            // Insert values
            preparedStatement.setLong(isInserting ? 1 : 4, profile.getPlayerId());
            int index = isInserting ? 2 : 1;
            preparedStatement.setBytes(index++, itemStacksToByteArray(profile.contents));
            preparedStatement.setBytes(index++, itemStacksToByteArray(profile.armorContents));

            // remainingContents can be null on database, lets check for that then
            byte[] byteArray; // Max length BLOB can hold is (2^16)-1 bytes
            if (!profile.remainingContents.isEmpty() &&
                    (byteArray = itemStacksToByteArray(profile.remainingContents.toArray(new ItemStack[profile.remainingContents.size()]))).length < 65535)
                preparedStatement.setBytes(index, byteArray);
            else
                preparedStatement.setNull(index, Types.BLOB);

            // Execute statement
            preparedStatement.execute();

            // Close everything
            preparedStatement.close();
            //connection.close(); // Do not close the connection since it is from outside

            // Update database state
            profile.databaseState = DatabaseState.ON_DATABASE;
            return true;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onProfileApplication(@NotNull PlayerHandler playerHandler) {
        PlayerInventory inventory = playerHandler.getPlayer().getInventory();

        // Clear current and apply all items
        inventory.clear();
        if (contents != null)
            inventory.setContents(contents);
        if (armorContents != null)
            inventory.setArmorContents(armorContents);
    }

    @Override
    protected void onProfileDestruction() {
        PlayerInventory inventory = getPlayerHandler().getPlayer().getInventory();

        contents = inventory.getContents();
        armorContents = inventory.getArmorContents();
        setAsModified();
    }

    public ArrayList<ItemStack> getRemainingContents() {
        return remainingContents;
    }

    /**
     * Give items to a player, storing them in case of full inventory
     *
     * @param warnPlayer true if you want the plugin to warn player about remaining items
     * @param itemStacks items to give the player
     * @return true if he received all items, false otherwise
     */
    public boolean addItems(boolean warnPlayer, @NotNull final ItemStack... itemStacks) {
        Collection<ItemStack> itemStackCollection = getPlayerHandler().getPlayer().getInventory().addItem(itemStacks).values();

        // If there are remaining items
        if (!itemStackCollection.isEmpty()) {
            // Store items that couldn't be given to the player
            remainingContents.addAll(itemStackCollection);

            // Warn player that he have pending items
            if (warnPlayer)
                getPlayerHandler().sendMessage("§cVocê tem itens pendentes: use §6/itens§c para recebê-los.");
        }

        return itemStackCollection.isEmpty();
    }
}
