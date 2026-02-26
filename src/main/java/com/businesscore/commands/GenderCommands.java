package com.businesscore.commands;

import com.businesscore.BusinessCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.businesscore.BusinessCore.color;

public class GenderCommands implements CommandExecutor {

    private final BusinessCore plugin;

    public GenderCommands(BusinessCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        String name = cmd.getName().toLowerCase();

        switch (name) {

            case "gendermenu": {
                if (!(sender instanceof Player player)) return true;
                plugin.getMenuManager().openMenu(player, "gender_select");
                return true;
            }

            case "selectgender": {
                if (!(sender instanceof Player player)) return true;
                if (args.length < 1) {
                    player.sendMessage(color("&cИспользование: /selectgender <male/female>"));
                    return true;
                }
                plugin.getGenderManager().selectGender(player, args[0]);
                return true;
            }

            case "mygender": {
                if (!(sender instanceof Player player)) return true;
                String g = plugin.getGenderManager().getPlayerGender(player);
                if (g.equals("male")) {
                    player.sendMessage(color("&eВаш пол: &b&l♂ Мужской"));
                } else if (g.equals("female")) {
                    player.sendMessage(color("&eВаш пол: &d&l♀ Женский"));
                } else {
                    player.sendMessage(color("&eВаш пол: &7не выбран"));
                }
                return true;
            }

            case "resetmygender": {
                if (!(sender instanceof Player player)) return true;
                if (!player.hasPermission("gender.reset.self")) {
                    player.sendMessage(color("&cНет прав."));
                    return true;
                }
                plugin.getGenderManager().resetGender(player);
                player.sendMessage(color("&a✓ Твой пол сброшен. Сделай шаг, чтобы выбрать заново."));
                return true;
            }

            case "resetgender": {
                if (!sender.hasPermission("gender.reset.other")) {
                    sender.sendMessage(color("&cНет прав."));
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage(color("&cИспользование: /resetgender <player>"));
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(color("&cИгрок не найден (должен быть онлайн)."));
                    return true;
                }

                plugin.getGenderManager().resetGender(target);
                target.sendMessage(color("&e&l⚠ &eТвой пол был сброшен. Сделай шаг, чтобы выбрать заново."));
                sender.sendMessage(color("&a✓ Пол игрока " + target.getName() + " сброшен."));
                return true;
            }

            case "updateskin": {
                if (!sender.hasPermission("gender.updateskin")) {
                    sender.sendMessage(color("&cНет прав."));
                    return true;
                }

                Player target;
                if (args.length >= 1) {
                    target = Bukkit.getPlayerExact(args[0]);
                    if (target == null) {
                        sender.sendMessage(color("&cИгрок не найден (должен быть онлайн)."));
                        return true;
                    }
                } else {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage(color("&cИспользование: /updateskin <player>"));
                        return true;
                    }
                    target = p;
                }

                plugin.getGenderManager().setSkinByGroup(target);
                sender.sendMessage(color("&a✓ Скин обновлён для " + target.getName()));
                return true;
            }

            case "genderskin": {
                if (!sender.hasPermission("gender.admin")) {
                    sender.sendMessage(color("&cНет прав."));
                    return true;
                }
                sender.sendMessage(color("&eЭта команда зависит от реализации GenderManager (skins config)."));
                sender.sendMessage(color("&7Если нужно — скажи, я добавлю хранение в config.yml."));
                return true;
            }

            case "genderconfig": {
                if (!sender.hasPermission("gender.admin")) {
                    sender.sendMessage(color("&cНет прав."));
                    return true;
                }
                sender.sendMessage(color("&eSkinsRestorer: " + (plugin.isSkinsRestorerAvailable() ? "&aесть" : "&cнет")));
                sender.sendMessage(color("&ePlaceholderAPI: " + (plugin.isPlaceholderApiAvailable() ? "&aесть" : "&cнет")));
                return true;
            }
        }

        return true;
    }
}
