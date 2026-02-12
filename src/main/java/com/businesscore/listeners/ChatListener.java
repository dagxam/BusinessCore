package com.businesscore.listeners;

import com.businesscore.BusinessCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import static com.businesscore.BusinessCore.color;

public class ChatListener implements Listener {

    private final BusinessCore plugin;

    public ChatListener(BusinessCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) return;

        Player player = event.getPlayer();
        String fmt = plugin.getConfig().getString("chat.format",
                "%gender_prefix% %rank_prefix%&f%player_name%&7: &f%message%");

        String rankPrefix = plugin.getConfig().getString(
                "ranks." + plugin.getDataManager().getRank(player.getUniqueId().toString()) + ".display",
                plugin.getDataManager().getRank(player.getUniqueId().toString())
        ) + " ";

        String genderPrefix;
        if (player.hasPermission("gender.male")) genderPrefix = "&b&l♂";
        else if (player.hasPermission("gender.female")) genderPrefix = "&d&l♀";
        else genderPrefix = "&7?";

        fmt = fmt.replace("%rank_prefix%", rankPrefix).replace("%gender_prefix%", genderPrefix);
        fmt = plugin.replacePlaceholders(player, fmt);
        fmt = fmt.replace("%message%", event.getMessage());

        event.setFormat(color(fmt));
    }
}
