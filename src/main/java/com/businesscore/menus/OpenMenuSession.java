package com.businesscore.menus;

import org.bukkit.inventory.Inventory;

import java.util.Map;

/**
 * Represents currently opened menu for a player.
 * slotToItemId maps inventory slots to item IDs.
 */
public record OpenMenuSession(
        String menuId,
        Inventory inventory,
        Map<Integer, String> slotToItemId
) {
}
