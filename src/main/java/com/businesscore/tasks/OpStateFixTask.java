package com.businesscore.tasks;

import com.businesscore.BusinessCore;
import com.businesscore.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tracks OP state changes and refreshes our own TAB rendering.
 * (Old TAB-plugin workaround removed.)
 */
public class OpStateFixTask extends BukkitRunnable {

    private final BusinessCore plugin;

    public OpStateFixTask(BusinessCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        DataManager dm = plugin.getDataManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String uuid = player.getUniqueId().toString();
            int now = player.isOp() ? 1 : 0;
            int stored = dm.getOpState(uuid);

            if (stored != now) {
                dm.setOpState(uuid, now);
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getTabManager().updateAll());
                return;
            }
        }
    }
}
