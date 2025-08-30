package org.fox.vneconomy.storage;

import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.api.EconomyProvider;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLiteStorage extends Storage {
    private final VNEconomy plugin;
    private Connection conn;

    public SQLiteStorage(VNEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        try {
            String url = "jdbc:sqlite:" + plugin.getDataFolder().toPath().resolve(plugin.getConfig().getString("sqlite.file", "economy.db"));
            conn = DriverManager.getConnection(url);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS economy (uuid TEXT PRIMARY KEY, balance REAL NOT NULL DEFAULT 0)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }

    @Override
    public double getBalance(UUID uuid) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM economy WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0D;
    }

    @Override
    public void set(UUID uuid, double amount) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO economy(uuid,balance) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, Math.max(0D, amount));
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
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
        List<EconomyProvider.TopEntry> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid,balance FROM economy ORDER BY balance DESC LIMIT ?")) {
            ps.setInt(1, size);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString(1));
                out.add(new EconomyProvider.TopEntry(uuid, nameOf(uuid), rs.getDouble(2)));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }
}
