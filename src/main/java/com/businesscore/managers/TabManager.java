package com.businesscore.managers;

import com.businesscore.BusinessCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.businesscore.BusinessCore.color;

public class TabManager {

    private final BusinessCore plugin;

    // per-player team name cache
    private final Map<UUID, String> playerTeamNames = new ConcurrentHashMap<>();

    public TabManager(BusinessCore plugin) {
        this.plugin = plugin;
    }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean("tab.enabled", true)) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayer(p);
        }
    }

    public void updatePlayer(Player player) {
        if (!plugin.getConfig().getBoolean("tab.enabled", true)) return;

        // ── (опционально) header/footer ──
        String header = plugin.getConfig().getString("tab.header", "");
        String footer = plugin.getConfig().getString("tab.footer", "");

        header = plugin.replacePlaceholders(player, header);
        footer = plugin.replacePlaceholders(player, footer);

        try {
            player.setPlayerListHeaderFooter(color(header), color(footer));
        } catch (Throwable ignored) {}

        // ── строка игрока: [Ранг] Имя | Очки⭐ | Баланс💰 ──
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        String teamName = playerTeamNames.computeIfAbsent(player.getUniqueId(), u -> makeTeamName(player));
        Team team = sb.getTeam(teamName);
        if (team == null) team = sb.registerNewTeam(teamName);

        // ранги
        String rankDisplay = getRankDisplay(player);

        // очки (из DataManager)
        int pts = plugin.getDataManager().getPoints(player.getUniqueId().toString());

        // деньги (из EconomyManager)
        String bal = plugin.formatMoney(plugin.getEconomyManager().getBalance(player)) + plugin.getCurrencySymbol();

        // формат (можешь менять цвета в config.yml)
        String prefix = plugin.getConfig().getString("tab.prefix", "&7[" + rankDisplay + "&7] &f");
        String suffix = plugin.getConfig().getString("tab.suffix", " &7| &e%points%⭐ &7| &6%balance%💰");

        // поддержка плейсхолдеров в конфиге
        prefix = prefix.replace("%rank_name%", rankDisplay);
        suffix = suffix.replace("%rank_name%", rankDisplay);

        suffix = suffix.replace("%points%", String.valueOf(pts));
        suffix = suffix.replace("%balance%", bal);

        prefix = plugin.replacePlaceholders(player, prefix);
        suffix = plugin.replacePlaceholders(player, suffix);

        team.setPrefix(color(cut(prefix, 64)));
        team.setSuffix(color(cut(suffix, 64)));

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }

        // --- ВАЖНО: Устанавливаем прямо в TAB, чтобы избежать конфликтов и обрезаний ---
        String tabName = prefix + player.getName() + suffix;
        player.setPlayerListName(color(tabName));
    }

    private String makeTeamName(Player player) {
        String base = "bc" + Integer.toHexString(player.getUniqueId().hashCode());
        if (base.length() > 16) base = base.substring(0, 16);
        return base;
    }

    private String getRankDisplay(Player p) {
        String uuid = p.getUniqueId().toString();
        String rankId = plugin.getDataManager().getRank(uuid);
        return plugin.getConfig().getString("ranks." + rankId + ".display", rankId);
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    public void shutdown() {
        // nothing
    }
}
