package org.fox.vneconomy.leaderboard;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.api.EconomyProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TopManager {
    private final VNEconomy plugin;
    private final org.fox.vneconomy.storage.Storage storage;
    private BukkitTask task;
    private List<EconomyProvider.TopEntry> cached = new ArrayList<>();

    public TopManager(VNEconomy plugin, org.fox.vneconomy.storage.Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void start() {
        int seconds = plugin.getConfig().getInt("leaderboard.cache-seconds", 60);
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refresh, 1L, seconds * 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    public void refresh() {
        int size = plugin.getConfig().getInt("leaderboard.size", 10);
        cached = storage.getTop(size);
    }

    public List<EconomyProvider.TopEntry> getCachedTop(int size) {
        if (cached.size() > size) {
            return Collections.unmodifiableList(cached.subList(0, size));
        }
        return Collections.unmodifiableList(cached);
    }
}
