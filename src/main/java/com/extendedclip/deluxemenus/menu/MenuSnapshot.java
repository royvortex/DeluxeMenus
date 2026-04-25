package com.extendedclip.deluxemenus.menu;

import java.util.Collection;
import org.bukkit.event.inventory.InventoryType;

/**
 * A snapshot of a menu's state for a specific player, containing all precomputed
 * items and properties needed for rendering.
 */
public class MenuSnapshot {
    private final String title;
    private final int size;
    private final InventoryType type;
    private final Collection<MenuItemData> items;

    public MenuSnapshot(String title, int size, InventoryType type, Collection<MenuItemData> items) {
        this.title = title;
        this.size = size;
        this.type = type;
        this.items = items;
    }

    public String getTitle() { return title; }
    public int getSize() { return size; }
    public InventoryType getType() { return type; }
    public Collection<MenuItemData> getItems() { return items; }
}
