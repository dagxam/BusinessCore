package com.businesscore;

import com.businesscore.commands.*;
import com.businesscore.hooks.PlaceholderHook;
import com.businesscore.listeners.*;
import com.businesscore.managers.*;
import com.businesscore.menus.MenuManager;
import com.businesscore.tasks.*;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class BusinessCore extends JavaPlugin {

    private static BusinessCore instance;

    private DataManager dataManager;
    private EconomyManager economyManager;
    private RankManager rankManager;
    private GenderManager genderManager;
    private MenuManager menuManager;
    private TabManager tabManager;

    private boolean placeholderApiAvailable;
    private boolean skinsRestorerAvailable;

    private final DecimalFormat moneyFormat = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Managers
        dataManager = new DataManager(this);
        dataManager.load();
        economyManager = new EconomyManager(this);
        rankManager = new RankManager(this);
        genderManager = new GenderManager(this);
        menuManager = new MenuManager(this);
        tabManager = new TabManager(this);

        // Optional hooks
        placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        skinsRestorerAvailable = Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer");

        // Register commands (static commands from plugin.yml)
        registerCommands();

        // Load menus from YAML and register menu commands (DeluxeMenus-like)
        menuManager.loadAllMenus();

        // Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        // Tasks
        long genderCheckTicks = getConfig().getInt("authme-gender-check-seconds", 2) * 20L;
        long skinUpdateTicks = getConfig().getInt("skin-update-interval-seconds", 30) * 20L;
        long tabUpdateTicks = getConfig().getInt("tab-update-interval-seconds", 2) * 20L;

        new GenderCheckTask(this).runTaskTimer(this, 100L, genderCheckTicks);
        new SkinUpdateTask(this).runTaskTimer(this, skinUpdateTicks, skinUpdateTicks);
        new TabUpdateTask(this).runTaskTimer(this, 40L, tabUpdateTicks);
        new OpStateFixTask(this).runTaskTimer(this, 100L, 40L);

        // PlaceholderAPI expansion (optional)
        if (placeholderApiAvailable) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }

        // Auto-save every 5 min
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> dataManager.save(), 6000L, 6000L);

        // Initial TAB render
        Bukkit.getScheduler().runTaskLater(this, () -> tabManager.updateAll(), 20L);

        getLogger().info("BusinessCore enabled! (PlaceholderAPI=" + placeholderApiAvailable + ", SkinsRestorer=" + skinsRestorerAvailable + ")");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.save();
        }
        if (tabManager != null) {
            tabManager.shutdown();
        }
        getLogger().info("BusinessCore disabled!");
    }

    private void registerCommands() {
        EconomyCommands eco = new EconomyCommands(this);
        getCommand("setmoney").setExecutor(eco);
        getCommand("addmoney").setExecutor(eco);
        getCommand("takemoney").setExecutor(eco);
        getCommand("balance").setExecutor(eco);
        getCommand("pay").setExecutor(eco);
        getCommand("baltop").setExecutor(eco);

        ShopCommands shop = new ShopCommands(this);
        getCommand("shop_vip").setExecutor(shop);
        getCommand("shop_lum").setExecutor(shop);
        getCommand("shop_mine").setExecutor(shop);
        getCommand("shop_farm").setExecutor(shop);
        getCommand("sell").setExecutor(shop);
        getCommand("skript-buy").setExecutor(shop);
        getCommand("skript-sell").setExecutor(shop);
        getCommand("skript-sellall").setExecutor(shop);
        getCommand("skript-buyhealth").setExecutor(shop);
        getCommand("skript-buyexp").setExecutor(shop);
        getCommand("skript-buylevel").setExecutor(shop);
        getCommand("skript-buypoints").setExecutor(shop);
        getCommand("skript-buy-upgradebook").setExecutor(shop);
        getCommand("skript-sell-upgradebook").setExecutor(shop);

        RankCommands rank = new RankCommands(this);
        getCommand("mypoints").setExecutor(rank);
        getCommand("checkpoints").setExecutor(rank);
        getCommand("addpoints").setExecutor(rank);
        getCommand("setpoints").setExecutor(rank);
        getCommand("resetpoints").setExecutor(rank);
        getCommand("ranklist").setExecutor(rank);
        getCommand("migratepoints").setExecutor(rank);

        GenderCommands gender = new GenderCommands(this);
        getCommand("selectgender").setExecutor(gender);
        getCommand("gendermenu").setExecutor(gender);
        getCommand("mygender").setExecutor(gender);
        getCommand("resetmygender").setExecutor(gender);
        getCommand("resetgender").setExecutor(gender);
        getCommand("updateskin").setExecutor(gender);
        getCommand("genderskin").setExecutor(gender);
        getCommand("genderconfig").setExecutor(gender);
    }

    // ── Accessors ──
    public static BusinessCore getInstance() { return instance; }
    public DataManager getDataManager() { return dataManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public RankManager getRankManager() { return rankManager; }
    public GenderManager getGenderManager() { return genderManager; }
    public MenuManager getMenuManager() { return menuManager; }
    public TabManager getTabManager() { return tabManager; }

    public boolean isPlaceholderApiAvailable() { return placeholderApiAvailable; }
    public boolean isSkinsRestorerAvailable() { return skinsRestorerAvailable; }

    public String getPrefix() {
        return color(getConfig().getString("prefix", "&6&l[Server]&r"));
    }

    public String getCurrencySymbol() {
        return getConfig().getString("currency-symbol", "₽");
    }

    public String formatMoney(double amount) {
        return moneyFormat.format(amount);
    }

    /**
     * Replaces BusinessCore placeholders (works without PlaceholderAPI).
     * If PlaceholderAPI is installed, it will be applied afterwards.
     */
    public String replacePlaceholders(Player player, String input) {
        if (input == null) return "";

        String out = input;

        // Currency symbol
        out = out.replace("%currency%", getCurrencySymbol());

        // Common
        if (player != null) {
            out = out.replace("%player%", player.getName());
            out = out.replace("%player_name%", player.getName());
            out = out.replace("%player_uuid%", player.getUniqueId().toString());
        }

        // Economy placeholders
        if (player != null) {
            double bal = economyManager.getBalance(player);
            String sym = getCurrencySymbol();
            out = out.replace("%skript_balance%", formatMoney(bal));
            out = out.replace("%businesscore_money%", formatMoney(bal));
            out = out.replace("%businesscore_skript_money%", formatMoney(bal));
            out = out.replace("%businesscore_balance%", formatMoney(bal) + sym);
            out = out.replace("%businesscore_skript_balance%", formatMoney(bal) + sym);
        }

        // Rank / points placeholders
        if (player != null) {
            String uuid = player.getUniqueId().toString();
            out = out.replace("%businesscore_points%", String.valueOf(dataManager.getPoints(uuid)));
            out = out.replace("%businesscore_rs_points%", String.valueOf(dataManager.getPoints(uuid)));
            out = out.replace("%businesscore_rank%", dataManager.getRank(uuid));
        }

        // Gender placeholders
        if (player != null) {
            String g;
            if (player.hasPermission("gender.male")) g = "§b§l♂";
            else if (player.hasPermission("gender.female")) g = "§d§l♀";
            else g = "§7?";
            out = out.replace("%businesscore_gender%", g);
            out = out.replace("%businesscore_rs_gender%", g);
        }

        // Apply PlaceholderAPI last (optional)
        if (placeholderApiAvailable && player != null && out.contains("%")) {
            try {
                out = PlaceholderAPI.setPlaceholders(player, out);
            } catch (Throwable ignored) {
                // If PlaceholderAPI changes, we don't want to crash.
            }
        }

        return out;
    }

    public static String color(String msg) {
        if (msg == null) return "";
        return msg.replace("&", "§");
    }
}
