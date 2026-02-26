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

        dm.setMoneyName(uuid, player.getName());

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
                player.sendMessage(color("&7Сделайте шаг, чтобы выбрать пол."));
                player.sendMessage("");
            }, 20L);
        }

        dm.setOpState(uuid, player.isOp() ? 1 : 0);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) plugin.getRankManager().checkRankUp(player);
        }, 20L);

        // apply skin if already selected using DataManager instead of permissions
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !plugin.getDataManager().getPlayerGender(uuid).equals("none")) {
                plugin.getGenderManager().setSkinByGroup(player);
            }
        }, 60L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) plugin.getTabManager().updatePlayer(player);
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        plugin.getDataManager().removeOpState(uuid);
        genderCooldown.remove(event.getPlayer().getUniqueId());
        plugin.getMenuManager().clearOpenSession(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        // Надежная проверка через DataManager (игнорирует OP права)
        if (!plugin.getDataManager().getPlayerGender(player.getUniqueId().toString()).equals("none")) return;

        if (plugin.getMenuManager().getOpenSession(player.getUniqueId()) != null) return;

        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();

        if (genderCooldown.containsKey(uid) && now - genderCooldown.get(uid) < 3000) return;
        genderCooldown.put(uid, now);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            plugin.getMenuManager().openMenu(player, "gender_select");
            player.sendMessage(color("&e&l⚠ &eВыбери свой пол для продолжения игры!"));
        });
    }
}
