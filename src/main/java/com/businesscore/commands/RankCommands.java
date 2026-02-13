package com.businesscore.commands;

import com.businesscore.BusinessCore;
import com.businesscore.managers.DataManager;
import com.businesscore.managers.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import static com.businesscore.BusinessCore.color;

public class RankCommands implements CommandExecutor {

    private final BusinessCore plugin;

    public RankCommands(BusinessCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "mypoints" -> cmdMyPoints(sender);
            case "checkpoints" -> cmdCheckPoints(sender, args);
            case "addpoints" -> cmdAddPoints(sender, args);
            case "setpoints" -> cmdSetPoints(sender, args);
            case "resetpoints" -> cmdResetPoints(sender, args);
            case "ranklist" -> cmdRankList(sender);
            case "migratepoints" -> cmdMigratePoints(sender);
        }
        return true;
    }

    private void cmdMyPoints(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Only players!"); return; }

        DataManager dm = plugin.getDataManager();
        RankManager rm = plugin.getRankManager();
        String uuid = player.getUniqueId().toString();
        int pts = dm.getPoints(uuid);
        String rank = dm.getRank(uuid);

        player.sendMessage("");
        player.sendMessage(color("&e&l⭐ &eТвои очки: &6&l" + pts));
        player.sendMessage(color("&e&l⭐ &eТвой ранг: &6&l" + rank));
        player.sendMessage(color("&7Следующий ранг: " + rm.getNextRankInfo(player)));
        player.sendMessage("");
    }

    private void cmdCheckPoints(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(color("&c/checkpoints <игрок>")); return; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { sender.sendMessage(color("&cИгрок не найден!")); return; }

        DataManager dm = plugin.getDataManager();
        String uuid = target.getUniqueId().toString();

        sender.sendMessage(color("&eОчки игрока &6" + target.getName() + "&e: &6&l" + dm.getPoints(uuid)));
        sender.sendMessage(color("&eРанг: &6" + dm.getRank(uuid)));
    }

    private void cmdAddPoints(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(color("&c/addpoints <игрок> <количество>")); return; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { sender.sendMessage(color("&cИгрок не найден!")); return; }

        int amount;
        try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
            sender.sendMessage(color("&cНеверное число!")); return;
        }

        DataManager dm = plugin.getDataManager();
        String uuid = target.getUniqueId().toString();
        dm.addPoints(uuid, amount);

        sender.sendMessage(color("&aДобавлено &6" + amount + " &aочков игроку &6" + target.getName()
                + "&a. Всего: &6" + dm.getPoints(uuid)));

        plugin.getRankManager().checkRankUp(target);
    }

    private void cmdSetPoints(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(color("&c/setpoints <игрок> <количество>")); return; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { sender.sendMessage(color("&cИгрок не найден!")); return; }

        int amount;
        try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
            sender.sendMessage(color("&cНеверное число!")); return;
        }

        DataManager dm = plugin.getDataManager();
        dm.setPoints(target.getUniqueId().toString(), amount);

        sender.sendMessage(color("&aУстановлено &6" + amount + " &aочков игроку &6" + target.getName()));
        plugin.getRankManager().checkRankUp(target);
    }

    private void cmdResetPoints(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(color("&c/resetpoints <игрок>")); return; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { sender.sendMessage(color("&cИгрок не найден!")); return; }

        DataManager dm = plugin.getDataManager();
        String uuid = target.getUniqueId().toString();

        dm.setPoints(uuid, 0);
        dm.setRank(uuid, "default");
        dm.setZombieKills(uuid, 0);
        dm.setSpiderKills(uuid, 0);
        dm.setDiamondMined(uuid, 0);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "lp user " + target.getName() + " parent set default");

        sender.sendMessage(color("&aОчки и ранг игрока &6" + target.getName() + " &aсброшены!"));
    }

    private void cmdRankList(CommandSender sender) {
        var ranksSec = plugin.getConfig().getConfigurationSection("ranks");

        sender.sendMessage("");
        sender.sendMessage(color("&6&l═══ СИСТЕМА РАНГОВ ═══"));

        if (ranksSec == null || ranksSec.getKeys(false).isEmpty()) {
            sender.sendMessage(color("&cВ конфиге нет секции &eranks&c."));
            sender.sendMessage("");
            return;
        }

        // Sort by required points ascending for readability
        var list = new java.util.ArrayList<String>();
        for (String id : ranksSec.getKeys(false)) {
            int pts = ranksSec.getInt(id + ".points", 0);
            String display = ranksSec.getString(id + ".display", id);
            list.add(pts + "|" + display);
        }
        list.sort(java.util.Comparator.comparingInt(s -> Integer.parseInt(s.split("\\|", 2)[0])));

        for (String line : list) {
            String[] parts = line.split("\\|", 2);
            int pts = Integer.parseInt(parts[0]);
            String display = parts[1];
            sender.sendMessage(color(display + " &8- &e" + pts + " очков"));
        }

        sender.sendMessage(color("&dПремьер-министр &8- &eназначается"));
        sender.sendMessage(color("&cПрезидент &8- &eназначается"));
        sender.sendMessage("");
    }

    private void cmdMigratePoints(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Only players!"); return; }
        // This was for Skript migration — in Java plugin, data is already UUID-based
        player.sendMessage(color("&aМиграция не требуется — данные уже хранятся по UUID."));
    }
}
