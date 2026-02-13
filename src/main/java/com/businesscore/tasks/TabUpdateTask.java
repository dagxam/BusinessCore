package com.businesscore.tasks;

import com.businesscore.BusinessCore;
import org.bukkit.scheduler.BukkitRunnable;

public class TabUpdateTask extends BukkitRunnable {

    private final BusinessCore plugin;

    public TabUpdateTask(BusinessCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getTabManager().updateAll();
    }
}
