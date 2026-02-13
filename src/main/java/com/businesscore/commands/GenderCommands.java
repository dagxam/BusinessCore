package com.businesscore.commands;

import com.businesscore.BusinessCore;
import com.businesscore.managers.GenderManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

import static com.businesscore.BusinessCore.color;

public class GenderCommands implements CommandExecutor {

    private final BusinessCore plugin;
    private final GenderManager gm;

    public GenderCommands(BusinessCore plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGenderManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "selectgender" -> cmdSelectGender(sender, args);
            case "gendermenu" -> cmdGenderMenu(sender);
            case "mygender" -> cmdMyGender(sender);
            case "resetmygender" -> cmdResetMyGender(sender);
            case "resetgender" -> cmdResetGender(sender, args);
            case "updateskin" -> cmdUpdateSkin(sender, args);
            case "genderskin" -> cmdGenderSkin(sender, args);
            case "genderconfig" -> cmdGenderConfig(sender);
        }
        return true;
    }

    private void cmdSelectGender(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Only players!"); return; }
        if (args.length < 1) {
            player.sendMessage(color("&c&l✖ &cИспользуй: /selectgender male или /selectgender female"));
            return;
        }
        gm.selectGender(player, args[0]);
    }

    private void cmdGenderMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
		plugin.getMenuManager().openMenu(player, "gender_select");
    }

    private void cmdMyGender(CommandSender sender) {
        if (!(sender instanceof Player player)) return;

        if (player.hasPermission("gender.male")) {
            player.sendMessage(color("&b&l♂ &bТвой пол: Мужской"));
            player.sendMessage(color("&7Группа: &e" + gm.getPlayerGroup(player)));
        } else if (player.hasPermission("gender.female")) {
            player.sendMessage(color("&d&l♀ &dТвой пол: Женский"));
            player.sendMessage(color("&7Группа: &e" + gm.getPlayerGroup(player)));
        } else {
            player.sendMessage(color("&7Пол не выбран"));
        }
    }

    private void cmdResetMyGender(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        gm.resetGender(player);
        player.sendMessage(color("&e&l⚠ &eТвой пол сброшен! Начни двигаться для открытия меню"));
    }

    private void cmdResetGender(CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(color("&c/resetgender <игрок>")); return; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { sender.sendMessage(color("&cИгрок не найден!")); return; }

        gm.resetGender(target);
        sender.sendMessage(color("&aПол игрока " + target.getName() + " сброшен!"));
    }

    private void cmdUpdateSkin(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(color("&cИгрок не в сети!")); return; }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("Specify a player!"); return;
        }

        if (target.hasPermission("gender.selected")) {
            gm.setSkinByGroup(target);
            sender.sendMessage(color("&aСкин обновлён для " + target.getName() + "!"));
        } else {
            sender.sendMessage(color("&cУ игрока не выбран пол!"));
        }
    }

    private void cmdGenderSkin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color("&cИспользуй: /genderskin <группа> <male/female> <название_скина>"));
            return;
        }

        String group = args[0];
        String gender = args[1].toLowerCase();
        String skinName = args[2];

        if (!gender.equals("male") && !gender.equals("female")) {
            sender.sendMessage(color("&cИспользуй: /genderskin <группа> <male/female> <название_скина>"));
            return;
        }

        plugin.getConfig().set("skins." + group + "." + gender, skinName);
        plugin.saveConfig();

        sender.sendMessage(color("&aСкин для группы &e" + group + " &a(пол: &e" + gender + "&a) установлен: &e" + skinName));
    }

    private void cmdGenderConfig(CommandSender sender) {
        sender.sendMessage(color("&6&l═══════════════════════════════"));
        sender.sendMessage(color("&e&lКонфигурация скинов:"));
        sender.sendMessage("");

        List<String> groups = plugin.getConfig().getStringList("group-priority");
        for (String group : groups) {
            sender.sendMessage(color("&6" + group + ":"));
            String male = plugin.getConfig().getString("skins." + group + ".male", "не установлен");
            String female = plugin.getConfig().getString("skins." + group + ".female", "не установлен");
            sender.sendMessage(color("  &bМужской: &f" + male));
            sender.sendMessage(color("  &dЖенский: &f" + female));
        }

        sender.sendMessage("");
        sender.sendMessage(color("&6default:"));
        sender.sendMessage(color("  &bМужской: &f" + plugin.getConfig().getString("skins.default.male", "Steve")));
        sender.sendMessage(color("  &dЖенский: &f" + plugin.getConfig().getString("skins.default.female", "Alex")));
        sender.sendMessage(color("&6&l═══════════════════════════════"));
    }
}
