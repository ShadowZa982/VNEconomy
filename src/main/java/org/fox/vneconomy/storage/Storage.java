package org.fox.vneconomy.storage;

import org.bukkit.OfflinePlayer;
import org.fox.vneconomy.api.EconomyProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class Storage implements EconomyProvider {
    public abstract void init();
    public abstract void close();
    public abstract Connection getConnection() throws SQLException;

    public String nameOf(UUID uuid) {
        OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
        return op != null && op.getName() != null ? op.getName() : uuid.toString();
    }

    public abstract double getBalance(UUID uuid);
    public abstract double getTotalRecharged(UUID uuid);

    // sửa kiểu trả về thành void
    @Override
    public abstract CompletableFuture<Void> giveAsync(UUID uuid, double amount);

}

