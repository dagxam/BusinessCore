package com.businesscore.commands;

import com.businesscore.BusinessCore;
import com.businesscore.managers.EconomyManager;
import com.businesscore.managers.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.List;

import static com.businesscore.BusinessCore.color;

public class ShopCommands implements CommandExecutor {

    private final BusinessCore plugin;
    private final EconomyManager eco;
    private final String sym;

    public ShopCommands(BusinessCore plugin) {
        this.plugin = plugin;
        this.eco = plugin.getEconomyManager();
        this.sym = plugin.getCurrencySymbol();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players!");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "shop_vip" -> cmdShopVip(player);
            case "shop_lum" -> openDM(player, "shop_lum_menu");
            case "shop_mine" -> openDM(player, "shop_mine_menu");
            case "shop_farm" -> openDM(player, "shop_farm_menu");
            case "sell" -> cmdSell(player, args);
            case "skript-buy" -> cmdBuy(player, args);
            case "skript-sell" -> cmdSellItem(player, args);
            case "skript-sellall" -> cmdSellAll(player, args);
            case "skript-buyhealth" -> cmdBuyHealth(player, args);
            case "skript-buyexp" -> cmdBuyExp(player, args);
            case "skript-buylevel" -> cmdBuyLevel(player, args);
            case "skript-buypoints" -> cmdBuyPoints(player, args);
            case "skript-buy-upgradebook" -> cmdBuyUpgradeBook(player, args);
            case "skript-sell-upgradebook" -> cmdSellUpgradeBook(player, args);
        }
        return true;
    }

    private void openDM(Player p, String menu) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open " + menu + " " + p.getName());
    }

    private void cmdShopVip(Player player) {
        List<String> vipGroups = plugin.getConfig().getStringList("vip-shop-groups");
        boolean hasAccess = false;
        for (String g : vipGroups) {
            if (player.hasPermission("group." + g)) {
                hasAccess = true;
                break;
            }
        }
        if (hasAccess) {
            openDM(player, "shop_vip_menu");
        } else {
            player.sendMessage(color("&c✗ VIP-магазин доступен с ранга &eOfficial&c и выше."));
        }
    }

    private void cmdSell(Player player, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage("");
            player.sendMessage(color("&c&l✗ &cВозьмите предмет в руку!"));
            player.sendMessage(color("&7Держите предмет и введите &e/sell"));
            player.sendMessage("");
            return;
        }

        int inHand = item.getAmount();
        int amount;

        if (args.length > 0) {
            try { amount = Integer.parseInt(args[0]); } catch (NumberFormatException e) {
                player.sendMessage(color("&c✗ Неверное количество!")); return;
            }
            if (amount <= 0) { player.sendMessage(color("&c✗ Количество должно быть больше 0!")); return; }
            if (amount > inHand) { player.sendMessage(color("&c✗ У вас в руке только x" + inHand + "!")); return; }
        } else {
            amount = inHand;
        }

        double money = amount * 0.1;

        if (amount >= inHand) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(inHand - amount);
        }

        eco.addBalance(player, money);

        player.sendMessage("");
        player.sendMessage(color("&a&l✓ &aПродано!"));
        player.sendMessage(color("&7Предмет: &f" + item.getType().name()));
        player.sendMessage(color("&7Количество: &ex" + amount));
        player.sendMessage(color("&7Получено: &6+" + money + sym));
        player.sendMessage(color("&7Баланс: &6" + eco.getBalance(player) + sym));
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    private void cmdBuy(Player player, String[] args) {
        if (args.length < 3) { player.sendMessage(color("&c✗ Неверные аргументы.")); return; }

        Material mat = Material.matchMaterial(args[0]);
        if (mat == null) { player.sendMessage(color("&c✗ Неверный предмет.")); return; }

        int amount, price;
        try {
            amount = Integer.parseInt(args[1]);
            price = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) { player.sendMessage(color("&c✗ Неверные числа.")); return; }

        if (amount <= 0 || price <= 0) { player.sendMessage(color("&c✗ Неверные значения.")); return; }

        if (!eco.takeBalance(player, price)) {
            player.sendMessage(color("&c✗ Недостаточно денег! Нужно: &6" + price + sym
                    + "&c, у вас: &6" + eco.getBalance(player) + sym));
            return;
        }

        player.getInventory().addItem(new ItemStack(mat, amount));
        player.sendMessage(color("&a✓ Куплено: x" + amount + " &f" + mat.name() + " &a(-&6" + price + sym + "&a)"));
        player.sendMessage(color("&7Остаток: &6" + eco.getBalance(player) + sym));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    private void cmdSellItem(Player player, String[] args) {
        if (args.length < 3) { player.sendMessage(color("&c✗ Неверные аргументы.")); return; }

        Material mat = Material.matchMaterial(args[0]);
        if (mat == null) { player.sendMessage(color("&c✗ Неверный предмет.")); return; }

        int amount, price;
        try {
            amount = Integer.parseInt(args[1]);
            price = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) { player.sendMessage(color("&c✗ Неверные числа.")); return; }

        if (amount <= 0 || price <= 0) { player.sendMessage(color("&c✗ Неверные значения.")); return; }

        if (player.getInventory().containsAtLeast(new ItemStack(mat), amount)) {
            player.getInventory().removeItem(new ItemStack(mat, amount));
            eco.addBalance(player, price);
            player.sendMessage(color("&a✓ Продано: x" + amount + " &f" + mat.name()
                    + " &a(+&6" + price + sym + "&a)"));
            player.sendMessage(color("&7Баланс: &6" + eco.getBalance(player) + sym));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            player.sendMessage(color("&c✗ У вас нет нужного количества предметов."));
        }
    }

    private void cmdSellAll(Player player, String[] args) {
        if (args.length < 3) { player.sendMessage(color("&c✗ Неверные аргументы.")); return; }

        Material mat = Material.matchMaterial(args[0]);
        if (mat == null) { player.sendMessage(color("&c✗ Неверный предмет.")); return; }

        int packSize, pricePerPack;
        try {
            packSize = Integer.parseInt(args[1]);
            pricePerPack = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) { player.sendMessage(color("&c✗ Неверные числа.")); return; }

        // Count total in inventory
        int total = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == mat) total += is.getAmount();
        }

        if (total <= 0) { player.sendMessage(color("&c✗ У вас нет этого предмета.")); return; }

        int packs = total / packSize;
        if (packs <= 0) {
            player.sendMessage(color("&c✗ Недостаточно для продажи (нужно минимум x" + packSize + ")."));
            return;
        }

        int sellAmount = packs * packSize;
        int earn = packs * pricePerPack;

        player.getInventory().removeItem(new ItemStack(mat, sellAmount));
        eco.addBalance(player, earn);

        player.sendMessage(color("&a✓ Продано: x" + sellAmount + " &f" + mat.name()
                + " &a(+&6" + earn + sym + "&a)"));
        player.sendMessage(color("&7Баланс: &6" + eco.getBalance(player) + sym));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    private void cmdBuyHealth(Player player, String[] args) {
        if (args.length < 2) return;
        int hp, price;
        try { hp = Integer.parseInt(args[0]); price = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { return; }
        if (hp <= 0 || price <= 0) return;

        if (!eco.takeBalance(player, price)) {
            player.sendMessage(color("&c✗ Недостаточно денег!")); return;
        }

        double newHealth = Math.min(player.getHealth() + hp, player.getMaxHealth());
        player.setHealth(newHealth);
        player.sendMessage(color("&a✓ Куплено здоровье: &c+" + hp + " &a(-&6" + price + sym + "&a)"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    private void cmdBuyExp(Player player, String[] args) {
        if (args.length < 2) return;
        int exp, price;
        try { exp = Integer.parseInt(args[0]); price = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { return; }
        if (exp <= 0 || price <= 0) return;

        if (!eco.takeBalance(player, price)) {
            player.sendMessage(color("&c✗ Недостаточно денег!")); return;
        }

        player.giveExp(exp);
        player.sendMessage(color("&a✓ Куплено опыта: &e+" + exp + " &a(-&6" + price + sym + "&a)"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    private void cmdBuyLevel(Player player, String[] args) {
        if (args.length < 2) return;
        int lvl, price;
        try { lvl = Integer.parseInt(args[0]); price = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { return; }
        if (lvl <= 0 || price <= 0) return;

        if (!eco.takeBalance(player, price)) {
            player.sendMessage(color("&c✗ Недостаточно денег!")); return;
        }

        player.setLevel(player.getLevel() + lvl);
        player.sendMessage(color("&a✓ Куплено уровней: &b+" + lvl + " &a(-&6" + price + sym + "&a)"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    private void cmdBuyPoints(Player player, String[] args) {
        if (args.length < 2) return;
        int pts, price;
        try { pts = Integer.parseInt(args[0]); price = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { return; }
        if (pts <= 0 || price <= 0) return;

        if (!eco.takeBalance(player, price)) {
            player.sendMessage(color("&c✗ Недостаточно денег! Нужно: &6" + price + sym)); return;
        }

        RankManager rm = plugin.getRankManager();
        rm.addPoints(player, pts);

        String uuid = player.getUniqueId().toString();
        player.sendMessage(color("&a✓ Куплено: &e+" + pts + " очков ранга &a(-&6" + price + sym + "&a)"));
        player.sendMessage(color("&7Всего очков: &e" + plugin.getDataManager().getPoints(uuid)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        rm.checkRankUp(player);
    }

    private void cmdBuyUpgradeBook(Player player, String[] args) {
        if (args.length < 2) return;
        int lvl, price;
        try { lvl = Integer.parseInt(args[0]); price = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { return; }
        if (lvl <= 0 || price <= 0) return;

        if (!eco.takeBalance(player, price)) {
            player.sendMessage(color("&c✗ Недостаточно денег!")); return;
        }

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(Enchantment.SHARPNESS, lvl, true);
        book.setItemMeta(meta);
        player.getInventory().addItem(book);

        player.sendMessage(color("&a✓ Куплена книга улучшений &7(Sharpness " + lvl + ") &a(-&6" + price + sym + "&a)"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    private void cmdSellUpgradeBook(Player player, String[] args) {
        if (args.length < 2) return;
        int price;
        try { price = Integer.parseInt(args[1]); } catch (NumberFormatException e) { return; }
        if (price <= 0) return;

        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.getType() == Material.ENCHANTED_BOOK) {
                player.getInventory().removeItem(is);
                eco.addBalance(player, price);
                player.sendMessage(color("&a✓ Книга продана &a(+&6" + price + sym + "&a)"));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                return;
            }
        }
        player.sendMessage(color("&c✗ У вас нет книги для продажи."));
    }
}
