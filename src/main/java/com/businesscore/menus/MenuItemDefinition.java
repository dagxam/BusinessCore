package com.businesscore.menus;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class MenuItemDefinition {

    private final String id;
    private final Material material;
    private final int amount;
    private final String displayName;
    private final List<String> lore;
    private final Integer slot; // single slot
    private final List<Integer> slots; // multi slots
    private final List<String> clickCommands;
    private final List<String> leftClickCommands;
    private final List<String> rightClickCommands;

    public MenuItemDefinition(String id,
                              Material material,
                              int amount,
                              String displayName,
                              List<String> lore,
                              Integer slot,
                              List<Integer> slots,
                              List<String> clickCommands,
                              List<String> leftClickCommands,
                              List<String> rightClickCommands) {
        this.id = id;
        this.material = material;
        this.amount = amount <= 0 ? 1 : amount;
        this.displayName = displayName;
        this.lore = lore == null ? List.of() : List.copyOf(lore);
        this.slot = slot;
        this.slots = slots == null ? List.of() : List.copyOf(slots);
        this.clickCommands = clickCommands == null ? List.of() : List.copyOf(clickCommands);
        this.leftClickCommands = leftClickCommands == null ? List.of() : List.copyOf(leftClickCommands);
        this.rightClickCommands = rightClickCommands == null ? List.of() : List.copyOf(rightClickCommands);
    }

    public String id() { return id; }
    public Material material() { return material; }
    public int amount() { return amount; }
    public String displayName() { return displayName; }
    public List<String> lore() { return lore; }
    public Integer slot() { return slot; }
    public List<Integer> slots() { return slots; }
    public List<String> clickCommands() { return clickCommands; }
    public List<String> leftClickCommands() { return leftClickCommands; }
    public List<String> rightClickCommands() { return rightClickCommands; }

    public List<Integer> allSlots() {
        List<Integer> out = new ArrayList<>();
        if (slot != null) out.add(slot);
        out.addAll(slots);
        return out;
    }
}
