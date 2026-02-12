package com.businesscore.tasks;

import com.businesscore.BusinessCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class GenderCheckTask extends BukkitRunnable {

    private final BusinessCore plugin;

    public GenderCheckTask(BusinessCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("authme.login")
                    && !player.hasPermission("gender.selected")
                    && !player.hasPermission("gender.menu.opened")) {

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp user " + player.getName() + " permission set gender.menu.opened true");

                int delay = plugin.getConfig().getInt("authme-gender-menu-delay-seconds", 2) * 20;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                "dm open gender_select " + player.getName());
                    }
                }, delay);
            }
        }
    }
}
