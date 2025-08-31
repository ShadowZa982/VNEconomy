package org.fox.vneconomy.storage;

import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.api.EconomyProvider;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SQLiteStorage extends Storage {

    private final VNEconomy plugin;

    public SQLiteStorage(VNEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            // Table economy
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS economy (
                        uuid TEXT PRIMARY KEY,
                        balance REAL NOT NULL DEFAULT 0
                    )
                    """);

            // Table tb_orders
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tb_orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        total REAL NOT NULL,
                        payment_status TEXT NOT NULL DEFAULT 'Unpaid',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            // Table tb_accounts: lưu tổng đã nạp và balance
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tb_accounts (
                        player_uuid TEXT PRIMARY KEY,
                        balance REAL DEFAULT 0,
                        total_recharged REAL DEFAULT 0
                    )
                    """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        // Không cần làm gì với SQLite driver
    }

    @Override
    public Connection getConnection() throws SQLException {
        String file = plugin.getConfig().getString("sqlite.file", "economy.db");
        String url = "jdbc:sqlite:" + plugin.getDataFolder().toPath().resolve(file);
        return DriverManager.getConnection(url);
    }

    @Override
    public double getBalance(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT balance FROM economy WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0D;
    }

    @Override
    public void set(UUID uuid, double amount) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO economy(uuid,balance) VALUES(?,?) " +
                             "ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, Math.max(0D, amount));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    public CompletableFuture<Boolean> takeAsync(UUID uuid, double amount) {
        return CompletableFuture.supplyAsync(() -> take(uuid, amount));
    }

    @Override
    public List<EconomyProvider.TopEntry> getTop(int size) {
        List<EconomyProvider.TopEntry> out = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid,balance FROM economy ORDER BY balance DESC LIMIT ?")) {
            ps.setInt(1, size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString(1));
                    out.add(new EconomyProvider.TopEntry(uuid, nameOf(uuid), rs.getDouble(2)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public int createOrder(UUID player, double total) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tb_orders (player_uuid,total,payment_status) VALUES (?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, player.toString());
            ps.setDouble(2, total);
            ps.setString(3, "Unpaid");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public double getTotalRecharged(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT total_recharged FROM tb_accounts WHERE player_uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total_recharged");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public CompletableFuture<Void> giveAsync(UUID uuid, double amount) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false); // bắt đầu transaction

                // 1️⃣ Cập nhật bảng economy
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO economy(uuid,balance) VALUES(?,?) " +
                                "ON CONFLICT(uuid) DO UPDATE SET balance=balance + excluded.balance")) {
                    ps.setString(1, uuid.toString());
                    ps.setDouble(2, amount);
                    ps.executeUpdate();
                }

                // 2️⃣ Cập nhật bảng tb_accounts
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tb_accounts(player_uuid,balance,total_recharged) VALUES(?,?,?) " +
                                "ON CONFLICT(player_uuid) DO UPDATE SET " +
                                "balance = balance + excluded.balance, " +
                                "total_recharged = total_recharged + excluded.total_recharged")) {
                    ps.setString(1, uuid.toString());
                    ps.setDouble(2, amount);
                    ps.setDouble(3, amount);
                    ps.executeUpdate();
                }

                conn.commit(); // commit transaction

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }


}
