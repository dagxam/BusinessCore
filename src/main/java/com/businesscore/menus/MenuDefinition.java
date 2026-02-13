package com.businesscore.menus;

import org.bukkit.Material;

import java.util.*;

public class MenuDefinition {

    private final String id;
    private final String title;
    private final int size;
    private final int updateIntervalSeconds;
    private final List<String> openCommands;
    private final boolean registerCommand;
    private final Map<String, MenuItemDefinition> items;

    public MenuDefinition(String id, String title, int size, int updateIntervalSeconds,
                          List<String> openCommands, boolean registerCommand,
                          Map<String, MenuItemDefinition> items) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.updateIntervalSeconds = updateIntervalSeconds;
        this.openCommands = openCommands == null ? List.of() : List.copyOf(openCommands);
        this.registerCommand = registerCommand;
        this.items = items == null ? Map.of() : new LinkedHashMap<>(items);
    }

    public String id() { return id; }
    public String title() { return title; }
    public int size() { return size; }
    public int updateIntervalSeconds() { return updateIntervalSeconds; }
    public List<String> openCommands() { return openCommands; }
    public boolean registerCommand() { return registerCommand; }
    public Map<String, MenuItemDefinition> items() { return items; }
}
