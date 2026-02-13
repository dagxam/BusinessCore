package com.businesscore.tasks;

import com.businesscore.BusinessCore;
import com.businesscore.managers.DataManager;
import com.businesscore.managers.GenderManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static com.businesscore.BusinessCore.color;

public class SkinUpdateTask extends BukkitRunnable {

    private final BusinessCore plugin;

    public SkinUpdateTask(BusinessCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        DataManager dm = plugin.getDataManager();
        GenderManager gm = plugin.getGenderManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("gender.selected")) {
                String currentGroup = gm.getPlayerGroup(player);
                String uuid = player.getUniqueId().toString();
                String stored = dm.getGenderGroup(uuid);

                if (!currentGroup.equals(stored)) {
                    dm.setGenderGroup(uuid, currentGroup);
                    gm.setSkinByGroup(player);
                    player.sendMessage(color("&e&l⚠ &eТвой скин обновлён по новой должности!"));
                }
            }
        }
    }
}
