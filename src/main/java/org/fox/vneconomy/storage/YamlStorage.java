package org.fox.vneconomy.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.api.EconomyProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class YamlStorage extends Storage {

    private final VNEconomy plugin;
    private FileConfiguration data;

    public YamlStorage(VNEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        data = plugin.getDataConfig();
        if (!data.isConfigurationSection("players")) {
            data.createSection("players");
            plugin.saveDataYaml();
        }
    }

    @Override
    public void close() {
        plugin.saveDataYaml();
    }

    private String path(UUID uuid) {
        return "players." + uuid.toString();
    }

    @Override
    public double getBalance(UUID uuid) {
        return data.getDouble(path(uuid) + ".balance", 0D);
    }

    @Override
    public void set(UUID uuid, double amount) {
        data.set(path(uuid) + ".balance", Math.max(0D, amount));
        plugin.saveDataYaml();
    }

    @Override
    public void give(UUID uuid, double amount) {
        set(uuid, getBalance(uuid) + amount);
    }

    @Override
    public boolean take(UUID uuid, double amount) {
        double bal = getBalance(uuid);
        if (bal < amount) return false;
        set(uuid, bal - amount);
        return true;
    }

    @Override
    public CompletableFuture<Double> getBalanceAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getBalance(uuid));
    }

    @Override
    public CompletableFuture<Void> setAsync(UUID uuid, double amount) {
        return CompletableFuture.runAsync(() -> set(uuid, amount));
    }

    @Override
    public CompletableFuture<Void> giveAsync(UUID uuid, double amount) {
        return CompletableFuture.runAsync(() -> give(uuid, amount));
    }

    @Override
    public CompletableFuture<Boolean> takeAsync(UUID uuid, double amount) {
        return CompletableFuture.supplyAsync(() -> take(uuid, amount));
    }

    @Override
    public List<EconomyProvider.TopEntry> getTop(int size) {
        Map<String, Object> players = data.getConfigurationSection("players").getValues(false);
        List<EconomyProvider.TopEntry> list = new ArrayList<>();
        for (String key : players.keySet()) {
            double bal = data.getDouble("players." + key + ".balance", 0D);
            UUID uuid = UUID.fromString(key);
            list.add(new EconomyProvider.TopEntry(uuid, nameOf(uuid), bal));
        }
        list.sort((a, b) -> Double.compare(b.amount(), a.amount()));
        if (list.size() > size) {
            return list.subList(0, size);
        }
        return list;
    }
}
