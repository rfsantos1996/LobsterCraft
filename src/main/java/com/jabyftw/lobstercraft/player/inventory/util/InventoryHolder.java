package com.jabyftw.lobstercraft.player.inventory.util;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.jabyftw.lobstercraft.util.BukkitScheduler;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Copyright (C) 2016  Rafael Sartori for PacocaCraft Plugin
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
 * <p>
 * This is code from <a href=https://bukkit.org/threads/icon-menu.108342/>Bukkit's Forum</a>
 */
public class InventoryHolder implements Listener {

    private final String inventoryTitle;
    private final int size;
    private final ItemStack[] optionIcons;
    private final ClickEventHandler handler;

    private Inventory inventory;

    /**
     * Creates an InventoryHolder with given properties
     *
     * @param inventoryTitle inventory's title
     * @param numberOfLines  number of lines (numberOfLines * 9 will give the size of the inventory)
     * @param handler        options' handler
     */
    public InventoryHolder(@NotNull final String inventoryTitle, int numberOfLines, @NotNull final ClickEventHandler handler) {
        if (numberOfLines <= 0) throw new IllegalArgumentException("Invalid inventory size.");
        this.inventoryTitle = inventoryTitle;
        this.size = numberOfLines * 9;
        this.handler = handler;
        this.optionIcons = new ItemStack[size];
    }

    public InventoryHolder setOption(int position, @Nullable final String itemName, @Nullable final String... itemLore) {
        return setOption(position, optionIcons[position], itemName, itemLore);
    }

    public InventoryHolder setOption(int position, @Nullable final ItemStack itemStack) {
        return setOption(position, itemStack, null);
    }

    public InventoryHolder setOption(int position, @Nullable final ItemStack itemStack, @Nullable final String itemName, @Nullable final String... itemLore) {
        ItemStack correctedItemStack = setItemNameAndLore(itemStack, itemName, itemLore);

        if (inventory != null) {
            // Update item
            inventory.setItem(position, correctedItemStack);

            // Iterate through viewers
            for (HumanEntity humanEntity : inventory.getViewers())
                // Update player's inventory view
                if (humanEntity instanceof Player) ((Player) humanEntity).updateInventory();
        }

        // Update our items array
        optionIcons[position] = correctedItemStack;
        return this;
    }

    public String getInventoryTitle() {
        return inventoryTitle;
    }

    public int getSize() {
        return size;
    }

    private ItemStack setItemNameAndLore(@NotNull final ItemStack itemStack, @Nullable final String itemName, @Nullable final String[] lore) {
        // Check if item stack needs to be updated
        if (itemStack != null && (itemName != null || lore != null)) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            {
                // Set display name, if needed
                if (itemName != null) itemMeta.setDisplayName(itemName);
                // Set lore, if needed
                if (lore != null) itemMeta.setLore(Arrays.asList(lore));
            }
            // Update item's properties
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public void open(@NotNull final Player player) {
        if (inventory == null) {
            // Create inventory
            inventory = Bukkit.createInventory(null, size, inventoryTitle);

            // Set items
            for (int i = 0; i < optionIcons.length; i++)
                if (optionIcons[i] != null)
                    inventory.setItem(i, optionIcons[i]);

            // Register events
            Bukkit.getServer().getPluginManager().registerEvents(this, LobsterCraft.lobsterCraft);
        }

        // Open inventory for player
        player.openInventory(inventory);
    }

    public void destroy() {
        // Close inventory
        if (inventory != null) {
            // Iterate through all viewers
            for (HumanEntity humanEntity : inventory.getViewers())
                // Close inventory
                humanEntity.closeInventory();

            // Clear and delete inventory
            inventory.clear();
            inventory = null;

            // This will remove the only reference to the inventory (if built blindly) and destroy the instance
            // Unregister events
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof InventoryHolder && ((InventoryHolder) obj).inventoryTitle.equals(inventoryTitle)) ||
                (obj instanceof Inventory && ((Inventory) obj).getTitle().equals(inventoryTitle));
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 19).append(inventoryTitle).toHashCode();
    }

    @EventHandler(priority = EventPriority.HIGHEST) // We're cancelling the event, can't be monitor
    public void onInventoryClick(InventoryClickEvent event) {
        // Just listen for this inventory
        if (equals(event.getInventory())) {
            int slot = event.getRawSlot();

            if (slot >= 0 && slot < size) {
                OptionClickEvent optionClickEvent = new OptionClickEvent((Player) event.getWhoClicked(), inventory.getItem(slot), slot);

                // "call" the event
                handler.onOptionClick(optionClickEvent);

                // Check for input for cancel (default true), close and destroy
                if (optionClickEvent.isCancelled())
                    event.setCancelled(true);

                if (optionClickEvent.willClose())
                    BukkitScheduler.runTaskLater(event.getWhoClicked()::closeInventory, 1);

                if (optionClickEvent.willDestroy())
                    BukkitScheduler.runTaskLater(this::destroy, 1);
            } else {
                // Clicked on empty item, cancel anyway
                event.setCancelled(true);
            }
        }
    }

    public interface ClickEventHandler {
        void onOptionClick(OptionClickEvent event);
    }

    public class OptionClickEvent implements Cancellable {

        private Player player;
        private ItemStack itemStack;
        private int position;

        private boolean cancelled = true, close = false, destroy = false;

        public OptionClickEvent(@NotNull Player player, @NotNull final ItemStack itemStack, int position) {
            this.player = player;
            this.itemStack = itemStack;
            this.position = position;
        }

        public Player getPlayer() {
            return player;
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        public Inventory getInventory() {
            // Will return null after inventory is destroyed
            return inventory;
        }

        public int getPosition() {
            return position;
        }

        public boolean willClose() {
            return close;
        }

        public void setWillClose(boolean close) {
            this.close = close;
        }

        public boolean willDestroy() {
            return destroy;
        }

        public void setWillDestroy(boolean destroy) {
            this.destroy = destroy;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
}
