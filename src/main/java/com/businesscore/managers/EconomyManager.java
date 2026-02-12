package com.businesscore.managers;

import com.businesscore.BusinessCore;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

public class EconomyManager {

    private final BusinessCore plugin;

    public EconomyManager(BusinessCore plugin) {
        this.plugin = plugin;
    }

    private String key(OfflinePlayer p) {
        return p.getUniqueId().toString();
    }

    public double getBalance(OfflinePlayer p) {
        return plugin.getDataManager().getMoney(key(p));
    }

    public void setBalance(OfflinePlayer p, double amount) {
        String k = key(p);
        plugin.getDataManager().setMoney(k, amount);
        if (p.getName() != null) {
            plugin.getDataManager().setMoneyName(k, p.getName());
        }
    }

    public void addBalance(OfflinePlayer p, double amount) {
        setBalance(p, getBalance(p) + amount);
    }

    /**
     * @return true if successful, false if insufficient funds
     */
    public boolean takeBalance(OfflinePlayer p, double amount) {
        double bal = getBalance(p);
        if (bal < amount) return false;
        setBalance(p, bal - amount);
        return true;
    }

    /**
     * Returns top balances sorted descending.
     */
    public List<Map.Entry<String, Double>> getTopBalances(int limit) {
        return plugin.getDataManager().getAllMoney().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public String getNameForUUID(String uuid) {
        return plugin.getDataManager().getMoneyName(uuid);
    }
}
