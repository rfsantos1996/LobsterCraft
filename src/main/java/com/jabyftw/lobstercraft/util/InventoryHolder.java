package com.jabyftw.lobstercraft.util;

import com.jabyftw.lobstercraft.LobsterCraft;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

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
    private final Integer maximumSize;
    private final LinkedList<ItemStack> optionIcons = new LinkedList<>();
    private final LinkedList<Inventory> inventoryPages = new LinkedList<>();
    private final ItemStack
            previousPage = setItemNameAndLore(new ItemStack(Material.PAPER, 1), "§6Página anterior", null),
            nextPage = setItemNameAndLore(new ItemStack(Material.PAPER, 1), "§6Próxima página", null);

    private ClickEventHandler handler;

    /**
     * Creates an InventoryHolder with given properties.<br>
     * Note: <i>%page%</i> will be replaced by the page number on inventory title
     *
     * @param inventoryTitle inventory's title (its what makes the inventory unique)
     * @param handler        options handler
     */
    public InventoryHolder(@NotNull final String inventoryTitle, @NotNull final ClickEventHandler handler, @Nullable Integer maximumSize) {
        this.inventoryTitle = inventoryTitle;
        this.handler = handler;
        this.maximumSize = maximumSize;
    }

    /**
     * This will customize the inventory, will accept colors. This DOES NOT update existing inventory!
     *
     * @param position  item position - must be [0, size[
     * @param itemStack ItemStack of the item
     * @param itemName  item's name
     * @param itemLore  item's lore (description-like)
     * @return this instance
     * @see org.bukkit.ChatColor#translateAlternateColorCodes(char, String)
     */
    public InventoryHolder setOption(int position, @Nullable final ItemStack itemStack, @Nullable final String itemName, @Nullable final String... itemLore) {
        // Check for valid position
        if (position < 0 || (maximumSize != null && position > maximumSize)) throw new ArrayIndexOutOfBoundsException("Position is invalid!");
        ItemStack correctedItemStack = setItemNameAndLore(itemStack, itemName, itemLore);

        // Update our instance
        optionIcons.set(position, correctedItemStack);
        return this;
    }

    public InventoryHolder setOption(int position, @Nullable final String itemName, @Nullable final String... itemLore) {
        return setOption(position, optionIcons.get(position), itemName, itemLore);
    }

    public InventoryHolder setOption(int position, @Nullable final ItemStack itemStack) {
        return setOption(position, itemStack, null);
    }

    /**
     * This method will create every page for this inventory. If the number of options is greater than 54, the last 3 options of the first page will be moved to the next
     * page so we can add "Next page" and "Previous page" buttons with an empty space that will be automatically handled.
     *
     * @return this instance
     */
    public InventoryHolder buildInventory() {
        int[] sizeOfEachPage = getSizeOfEachPage(optionIcons.size());
        int optionIndex = 0;
        boolean wasEmpty = inventoryPages.isEmpty();

        // Create inventory: size must be {9, 18, 27, 36, 45, or 54}
        for (int pageIndex = 0; pageIndex < sizeOfEachPage.length; pageIndex++) {
            Inventory inventory = Bukkit.createInventory(null, sizeOfEachPage[pageIndex], inventoryTitle.replaceAll("%page%", String.valueOf(pageIndex + 1)));
            boolean isNotTheLastPage = pageIndex < (sizeOfEachPage.length - 1);

            // Setup inventory
            for (int itemIndex = 0; itemIndex < sizeOfEachPage[pageIndex] + (isNotTheLastPage ? -3 : 0); itemIndex++) {
                // Break if we finished adding all items
                if (optionIndex >= optionIcons.size()) break;
                inventory.setItem(itemIndex, optionIcons.get(optionIndex++));
            }

            // Add next/previous page buttons if current page isn't the last one
            if (isNotTheLastPage) {
                // Set next page item
                ItemStack nextPage_clone = nextPage.clone();
                nextPage_clone.setAmount(pageIndex + 1);
                inventory.setItem(54, nextPage_clone);

                // Set previous page item
                if (pageIndex > 0) {
                    ItemStack previousPage_clone = previousPage.clone();
                    previousPage_clone.setAmount(pageIndex);
                    inventory.setItem(53, previousPage_clone);
                }
            }

            // Add page
            inventoryPages.set(pageIndex, inventory);
        }

        // Check if inventory has multiple pages
        if (inventoryPages.size() > 1)
            handler = new MultiplePageEventHandler(handler);

        // Register events for this Inventory
        if (wasEmpty)
            Bukkit.getServer().getPluginManager().registerEvents(this, LobsterCraft.plugin);
        return this;
    }

    /**
     * Opens this inventory for given player. If the inventory wasn't opened before, it'll be built now.
     *
     * @param player the player you want the inventory to open for
     * @return this instance
     */
    public InventoryHolder open(@NotNull final Player player) {
        if (inventoryPages.isEmpty())
            buildInventory();

        // Open inventory for player
        player.openInventory(inventoryPages.getFirst());
        return this;
    }

    /**
     * This will destroy inventories and un-register the Listener
     */
    public void destroy() {
        // Close inventories
        if (!inventoryPages.isEmpty()) {
            for (Inventory inventory : inventoryPages) {
                // Iterate through all viewers and close inventory
                for (HumanEntity humanEntity : inventory.getViewers())
                    humanEntity.closeInventory();

                // Clear and delete inventory
                // This will remove the only reference to the inventory (if not stored)
                inventory.clear();
            }
            inventoryPages.clear();

            // Un-register events
            HandlerList.unregisterAll(this);
        }
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

    public String getInventoryTitle() {
        return inventoryTitle;
    }

    public int getItemSize() {
        return optionIcons.size();
    }

    public int getNumberOfPages() {
        return inventoryPages.size();
    }

    /*
     * Overridden methods
     */

    @Override
    public boolean equals(Object obj) {
        if (obj != null)
            if (obj instanceof InventoryHolder)
                return ((InventoryHolder) obj).inventoryTitle.equalsIgnoreCase(inventoryTitle);
            else if (obj instanceof Inventory)
                return inventoryPages.contains(obj);
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 19).append(inventoryTitle).toHashCode();
    }

    /*
     * Listener
     */

    @EventHandler(priority = EventPriority.HIGHEST) // We're cancelling the event, can't be monitor
    private void onInventoryClick(InventoryClickEvent event) {
        // Just listen for this inventory
        if (equals(event.getView().getTopInventory())) {
            OptionClickEvent optionClickEvent = new OptionClickEvent(event, this);

            LobsterCraft.logger.info(Util.appendStrings("Click event:\n",
                    "isShiftClick: ", event.isShiftClick(), '\n',
                    "isLeftClick", event.isLeftClick(), '\n',
                    "isRightClick", event.isRightClick(), '\n',
                    "Action: ", event.getAction().name(), '\n',
                    "RawSlot: ", event.getRawSlot(), '\n',
                    "Slot: ", event.getSlot(), '\n',
                    "SlotType: ", event.getSlotType(), '\n',
                    "clickType: ", event.getClick(), '\n',
                    "HotbarButton: ", event.getHotbarButton(), '\n',
                    "InventoryName: ", event.getInventory().getName(), '\n',
                    "TopInventoryName: ", event.getView().getTopInventory().getName(), '\n',
                    "BottomInventoryName: ", event.getView().getBottomInventory().getName(), '\n',
                    "(View)Cursor: ", event.getView().getCursor().toString(), '\n',
                    "CurrentItem: ", event.getCurrentItem()
            ));

            // "Call" the event
            handler.onOptionClick(optionClickEvent);

            // Check for input for cancel (default true), close and destroy
            if (optionClickEvent.isCancelled())
                event.setCancelled(true);

            // Note: I used to run these 1 tick after calling the event and I don't know why - needs testing
            // Check if we should close the Inventory
            if (optionClickEvent.willClose())
                event.getWhoClicked().closeInventory();
            // Check if we should destroy the Inventory
            if (optionClickEvent.willDestroy())
                destroy();
        }
    }

    /*
     * Static methods - Utilities
     */

    /**
     * This method will mix all items in one Map(ItemStack, amount of items)
     *
     * @param itemStacks ItemStack array
     * @return a map with all ItemStacks together
     */
    public static HashMap<ItemStack, Integer> mergeItems(@NotNull final ItemStack[] itemStacks) {
        HashMap<ItemStack, Integer> items = new HashMap<>();

        for (ItemStack currentItem : itemStacks) {
            ItemStack similarItem = null;
            // Iterate through inserted items
            for (ItemStack insertedItem : items.keySet())
                // If currentItem is similar to a inserted item, merge them
                // Note: this doesn't consider amount, so we're safe!
                if (insertedItem.isSimilar(currentItem)) {
                    similarItem = insertedItem;
                    break;
                }

            if (similarItem != null)
                items.put(similarItem, items.get(similarItem) + currentItem.getAmount());
            else
                items.put(currentItem, currentItem.getAmount());
        }

        return items;
    }

    private static int[] getSizeOfEachPage(int numberOfItems) {
        int numberOfPages = 0;

        while (numberOfItems > 0) {
            numberOfPages += 1;
            numberOfItems -= 54;
            // If we filled the page and there are items, we will need to add 3 buttons (3 items will be moved to the next page with the remaining ones)
            if (numberOfItems > 0)
                numberOfItems += 3;
        }

        int[] pageSize = new int[numberOfPages];
        for (int i = 0; i < pageSize.length; i++) {
            pageSize[i] = i < (pageSize.length - 1) ? 54 : numberOfItems % 54;
        }
        return pageSize;
    }

    /*
     * Some classes
     */

    public interface ClickEventHandler {
        void onOptionClick(@NotNull final OptionClickEvent event);
    }

    private class MultiplePageEventHandler implements ClickEventHandler {

        private final ClickEventHandler originalHandler;

        public MultiplePageEventHandler(@NotNull final ClickEventHandler originalHandler) {
            this.originalHandler = originalHandler;
        }

        @Override
        public void onOptionClick(@NotNull OptionClickEvent event) {
            int pageIndex;
            // Check if isn't the last page
            if (event.clickedOnCustomInventory() &&
                    (pageIndex = event.getCustomInventories().indexOf(event.getBukkitClickEvent().getInventory())) < (event.getCustomInventories().size() - 1)) {
                // Check if was the "next page" button
                if (event.getBukkitClickEvent().getRawSlot() == 54 || event.getBukkitClickEvent().getRawSlot() == 53) {
                    // Close manually
                    event.setWillClose(false);
                    event.getBukkitClickEvent().getView().getPlayer().closeInventory();
                    // Open next page
                    event.getCustomInventories().get(pageIndex + (event.getBukkitClickEvent().getRawSlot() == 54 ? 1 : -1));
                } else {
                    originalHandler.onOptionClick(event);
                }
            }
        }
    }

    public class OptionClickEvent implements Cancellable {

        private final InventoryClickEvent event;
        private final InventoryHolder inventoryHolder;

        private boolean cancelled = true, close = false, destroy = false;

        private OptionClickEvent(@NotNull final InventoryClickEvent event, @NotNull final InventoryHolder inventoryHolder) {
            this.event = event;
            this.inventoryHolder = inventoryHolder;
        }

        /*
         * Simple getters and setters
         */

        /**
         * @return the InventoryClickEvent that involves this InventoryHolder
         */
        public InventoryClickEvent getBukkitClickEvent() {
            return event;
        }

        public InventoryHolder getInventoryHolder() {
            return inventoryHolder;
        }

        public LinkedList<Inventory> getCustomInventories() {
            return inventoryPages;
        }

        public int getPage() {
            return inventoryPages.indexOf(event.getInventory());
        }

        public boolean clickedOnCustomInventory() {
            return inventoryPages.contains(event.getInventory());
        }

        public boolean willClose() {
            return close;
        }

        /**
         * @param close true if you want the inventory to close after the player click
         */
        public void setWillClose(boolean close) {
            this.close = close;
        }

        public boolean willDestroy() {
            return destroy;
        }

        /**
         * @param destroy true if you want to destroy the entire inventory after the player click
         */
        public void setWillDestroy(boolean destroy) {
            this.destroy = destroy;
        }

        /*
         * Overridden methods
         */

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
