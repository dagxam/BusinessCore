package com.businesscore.menus;

import com.businesscore.BusinessCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.businesscore.BusinessCore.color;

/**
 * DeluxeMenus-like menu system.
 *
 * - Loads menus from plugins/BusinessCore/menus/*.yml
 * - Supports open_command + register_command
 * - Supports item slots, slot ranges ("0-8") and click commands
 * - Supports internal placeholders and optional PlaceholderAPI
 */
public class MenuManager {

    private final BusinessCore plugin;

    private final Map<String, MenuDefinition> menus = new ConcurrentHashMap<>();
    private final Map<String, String> commandToMenuId = new ConcurrentHashMap<>();
    private final Map<UUID, OpenMenuSession> openSessions = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> updateTasks = new ConcurrentHashMap<>();

    public MenuManager(BusinessCore plugin) {
        this.plugin = plugin;
        ensureDefaultMenus();
    }

    public void loadAllMenus() {
        menus.clear();
        commandToMenuId.clear();

        File folder = new File(plugin.getDataFolder(), "menus");
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;

        for (File f : files) {
            try {
                String id = f.getName().substring(0, f.getName().length() - 4);
                MenuDefinition def = loadMenuFromFile(id, f);
                if (def == null) continue;
                menus.put(def.id(), def);

                for (String cmd : def.openCommands()) {
                    if (cmd == null || cmd.isBlank()) continue;
                    commandToMenuId.put(cmd.toLowerCase(Locale.ROOT), def.id());
                    if (def.registerCommand()) {
                        registerDynamicCommand(cmd, def.id());
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to load menu file: " + f.getName() + " -> " + t.getMessage());
            }
        }
    }

    public boolean openMenu(Player player, String menuId) {
        MenuDefinition def = menus.get(menuId);
        if (def == null) {
            player.sendMessage(color("&c✗ Меню &e" + menuId + " &cне найдено."));
            return false;
        }

        Inventory inv = Bukkit.createInventory(player, def.size(), color(plugin.replacePlaceholders(player, def.title())));
        Map<Integer, String> slotToItem = new HashMap<>();

        // Fill items
        for (MenuItemDefinition item : def.items().values()) {
            ItemStack stack = buildItem(player, item);
            for (int slot : item.allSlots()) {
                if (slot < 0 || slot >= inv.getSize()) continue;
                inv.setItem(slot, stack);
                slotToItem.put(slot, item.id());
            }
        }

        OpenMenuSession session = new OpenMenuSession(def.id(), inv, slotToItem);
        openSessions.put(player.getUniqueId(), session);

        // schedule auto-update
        cancelUpdateTask(player.getUniqueId());
        int interval = def.updateIntervalSeconds();
        if (interval > 0) {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) {
                    cancelUpdateTask(player.getUniqueId());
                    openSessions.remove(player.getUniqueId());
                    return;
                }
                OpenMenuSession s = openSessions.get(player.getUniqueId());
                if (s == null) { cancelUpdateTask(player.getUniqueId()); return; }
                refreshMenu(player, def, s);
            }, interval * 20L, interval * 20L);
            updateTasks.put(player.getUniqueId(), task);
        }

        player.openInventory(inv);
        return true;
    }

    public OpenMenuSession getOpenSession(UUID uuid) {
        return openSessions.get(uuid);
    }

    public void clearOpenSession(UUID uuid) {
        openSessions.remove(uuid);
        cancelUpdateTask(uuid);
    }

    private void cancelUpdateTask(UUID uuid) {
        BukkitTask t = updateTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    public void handleClick(Player player, OpenMenuSession session, int slot, boolean rightClick) {
        MenuDefinition def = menus.get(session.menuId());
        if (def == null) return;

        String itemId = session.slotToItemId().get(slot);
        if (itemId == null) return;

        MenuItemDefinition item = def.items().get(itemId);
        if (item == null) return;

        List<String> commands;
        if (rightClick && !item.rightClickCommands().isEmpty()) commands = item.rightClickCommands();
        else if (!rightClick && !item.leftClickCommands().isEmpty()) commands = item.leftClickCommands();
        else commands = item.clickCommands();

        if (commands == null || commands.isEmpty()) return;

        for (String raw : commands) {
            if (raw == null) continue;
            String cmd = plugin.replacePlaceholders(player, raw);

            if (cmd.equalsIgnoreCase("[close]")) {
                player.closeInventory();
                continue;
            }

            if (cmd.startsWith("[player]")) {
                String toRun = cmd.substring("[player]".length()).trim();
                if (!toRun.isEmpty()) Bukkit.dispatchCommand(player, toRun);
                continue;
            }

            if (cmd.startsWith("[console]")) {
                String toRun = cmd.substring("[console]".length()).trim();
                if (!toRun.isEmpty()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun.replace("%player%", player.getName()));
                continue;
            }

            // DeluxeMenus also allows plain commands; treat them as player commands
            Bukkit.dispatchCommand(player, cmd);
        }
    }

    private void refreshMenu(Player player, MenuDefinition def, OpenMenuSession session) {
        // Update title (Paper supports changing title by reopening)
        try {
            String newTitle = color(plugin.replacePlaceholders(player, def.title()));
            if (!player.getOpenInventory().getTitle().equals(newTitle)) {
                // Reopen inventory to update title safely
                openMenu(player, def.id());
                return;
            }
        } catch (Throwable ignored) {
        }

        // Update items that have placeholders
        for (MenuItemDefinition item : def.items().values()) {
            ItemStack updated = buildItem(player, item);
            for (int slot : item.allSlots()) {
                if (slot < 0 || slot >= session.inventory().getSize()) continue;
                session.inventory().setItem(slot, updated);
            }
        }
    }

    private ItemStack buildItem(Player player, MenuItemDefinition item) {
        Material mat = item.material() == null ? Material.STONE : item.material();
        ItemStack stack = new ItemStack(mat, item.amount());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = item.displayName();
            if (name != null) meta.setDisplayName(color(plugin.replacePlaceholders(player, name)));

            if (!item.lore().isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String l : item.lore()) {
                    lore.add(color(plugin.replacePlaceholders(player, l)));
                }
                meta.setLore(lore);
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private MenuDefinition loadMenuFromFile(String id, File file) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        String title = yml.getString("menu_title", "Menu");
        int size = yml.getInt("size", 54);
        int update = yml.getInt("update_interval", 0);

        boolean register = yml.getBoolean("register_command", false);

        List<String> openCommands = new ArrayList<>();
        Object oc = yml.get("open_command");
        if (oc instanceof String s) {
            if (!s.isBlank()) openCommands.add(s);
        } else if (oc instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                String s = String.valueOf(o);
                if (!s.isBlank()) openCommands.add(s);
            }
        }

        Map<String, MenuItemDefinition> items = new LinkedHashMap<>();
        ConfigurationSection itemsSec = yml.getConfigurationSection("items");
        if (itemsSec != null) {
            for (String key : itemsSec.getKeys(false)) {
                ConfigurationSection is = itemsSec.getConfigurationSection(key);
                if (is == null) continue;

                Material mat = Material.matchMaterial(String.valueOf(is.getString("material", "STONE")).toUpperCase(Locale.ROOT));
                if (mat == null) mat = Material.STONE;

                int amount = is.getInt("amount", 1);
                String displayName = is.getString("display_name", "");
                List<String> lore = is.getStringList("lore");
                Integer slot = is.contains("slot") ? is.getInt("slot") : null;
                List<Integer> slots = parseSlots(is.getList("slots"));

                List<String> click = is.getStringList("click_commands");
                List<String> left = is.getStringList("left_click_commands");
                List<String> right = is.getStringList("right_click_commands");

                MenuItemDefinition item = new MenuItemDefinition(key, mat, amount, displayName, lore, slot, slots, click, left, right);
                items.put(key, item);
            }
        }

        return new MenuDefinition(id, title, size, update, openCommands, register, items);
    }

    private List<Integer> parseSlots(List<?> rawList) {
        if (rawList == null || rawList.isEmpty()) return List.of();
        List<Integer> out = new ArrayList<>();
        for (Object o : rawList) {
            if (o == null) continue;
            String s = String.valueOf(o).trim();
            if (s.isEmpty()) continue;
            if (s.contains("-")) {
                String[] parts = s.split("-", 2);
                try {
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    int start = Math.min(a, b);
                    int end = Math.max(a, b);
                    for (int i = start; i <= end; i++) out.add(i);
                } catch (NumberFormatException ignored) {
                }
            } else {
                try {
                    out.add(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    // ──────────────────────────
    // Dynamic command registration
    // ──────────────────────────
    private void registerDynamicCommand(String commandName, String menuId) {
        String cmd = commandName.toLowerCase(Locale.ROOT);
        if (Bukkit.getPluginCommand(cmd) != null) {
            // Already exists (from plugin.yml or another plugin). We'll still map it.
            return;
        }

        try {
            CommandMap map = getCommandMap();
            if (map == null) return;

            BukkitCommand dyn = new BukkitCommand(cmd) {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage("Only players!");
                        return true;
                    }
                    return openMenu(p, menuId);
                }
            };
            dyn.setDescription("Open menu: " + menuId);
            map.register(plugin.getName().toLowerCase(Locale.ROOT), dyn);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register menu command /" + cmd + ": " + t.getMessage());
        }
    }

    private CommandMap getCommandMap() {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            return (CommandMap) f.get(Bukkit.getServer());
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ──────────────────────────
    // Default menus
    // ──────────────────────────
    private void ensureDefaultMenus() {
        File folder = new File(plugin.getDataFolder(), "menus");
        if (!folder.exists()) folder.mkdirs();

        saveDefaultMenu("gender_select.yml");
        saveDefaultMenu("shop.yml");
        saveDefaultMenu("shop_farm_menu.yml");
        saveDefaultMenu("shop_lum_menu.yml");
        saveDefaultMenu("shop_mine_menu.yml");
        saveDefaultMenu("shop_vip_menu.yml");
    }

    private void saveDefaultMenu(String fileName) {
        File out = new File(plugin.getDataFolder(), "menus/" + fileName);
        if (out.exists()) return;

        String path = "menus/" + fileName;
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) return;
            plugin.saveResource(path, false);
        } catch (Exception ignored) {
        }
    }
}
