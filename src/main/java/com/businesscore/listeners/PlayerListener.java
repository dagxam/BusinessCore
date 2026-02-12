package com.businesscore.listeners;

import com.businesscore.BusinessCore;
import com.businesscore.managers.DataManager;
import com.businesscore.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.businesscore.BusinessCore.color;

public class PlayerListener implements Listener {

    private final BusinessCore plugin;
    private final Map<UUID, Long> genderCooldown = new HashMap<>();

    public PlayerListener(BusinessCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DataManager dm = plugin.getDataManager();
        EconomyManager eco = plugin.getEconomyManager();
        String uuid = player.getUniqueId().toString();

        // Initialize money if not set
        if (dm.getMoney(uuid) == 0.0 && dm.getMoneyName(uuid).equals(uuid)) {
            dm.setMoney(uuid, 0);
        }
        dm.setMoneyName(uuid, player.getName());

        // First join: start money
        if (dm.isFirstJoin(uuid)) {
            dm.markJoined(uuid);
            int startMoney = plugin.getConfig().getInt("start-money", 100);
            String sym = plugin.getCurrencySymbol();

            if (eco.getBalance(player) <= 0) {
                eco.setBalance(player, startMoney);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("");
                player.sendMessage(color("&a&l✓ Добро пожаловать на сервер!"));
                player.sendMessage(color("&eВам начислено: &6" + startMoney + sym));
                player.sendMessage("");
            }, 20L);
        }

        // OP state for TAB fix
        dm.setOpState(uuid, player.isOp() ? 1 : 0);

        // Check rank after 1 second
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getRankManager().checkRankUp(player);
            }
        }, 20L);

        // Apply skin after 3 seconds if gender selected
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.hasPermission("gender.selected")) {
                plugin.getGenderManager().setSkinByGroup(player);
            }
        }, 60L);

        // Render TAB shortly after join (needs rank/gender loaded)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getTabManager().updatePlayer(player);
            }
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        plugin.getDataManager().removeOpState(uuid);
        genderCooldown.remove(event.getPlayer().getUniqueId());

        // Clean up open menu session
        plugin.getMenuManager().clearOpenSession(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only trigger on actual position changes, not just looking around
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        if (!player.hasPermission("gender.selected")) {
            UUID uid = player.getUniqueId();
            long now = System.currentTimeMillis();

            if (!genderCooldown.containsKey(uid) || now - genderCooldown.get(uid) > 3000) {
                genderCooldown.put(uid, now);
                Bukkit.dispatchCommand(player, "gendermenu");
                player.sendMessage(color("&e&l⚠ &eВыбери свой пол для продолжения игры!"));
            }
        }
    }
}
