package org.fox.vneconomy.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.api.EconomyProvider;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Lưu trữ dữ liệu kinh tế bằng MySQL/MariaDB.
 */
public class MySQLStorage extends Storage {

    private final VNEconomy plugin;
    private HikariDataSource ds;

    public MySQLStorage(VNEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        HikariConfig cfg = new HikariConfig();

        String host = plugin.getConfig().getString("mysql.host", "localhost");
        int port = plugin.getConfig().getInt("mysql.port", 3306);
        String db = plugin.getConfig().getString("mysql.database", "vneconomy");
        String user = plugin.getConfig().getString("mysql.username", "root");
        String pass = plugin.getConfig().getString("mysql.password", "password");
        boolean useSSL = plugin.getConfig().getBoolean("mysql.useSSL", false);

        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db +
                "?useSSL=" + useSSL + "&serverTimezone=UTC");
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(plugin.getConfig().getInt("mysql.pool.maximumPoolSize", 10));
        cfg.setMinimumIdle(plugin.getConfig().getInt("mysql.pool.minimumIdle", 2));

        ds = new HikariDataSource(cfg);

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            // Table economy
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS economy (
                        uuid VARCHAR(36) PRIMARY KEY,
                        balance DOUBLE NOT NULL DEFAULT 0
                    )
                    """);

            // Table tb_orders
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tb_orders (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        player_uuid VARCHAR(36) NOT NULL,
                        total DOUBLE NOT NULL,
                        payment_status VARCHAR(20) NOT NULL DEFAULT 'Unpaid',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            // Table tb_accounts
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tb_accounts (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        balance DOUBLE DEFAULT 0,
                        total_recharged DOUBLE DEFAULT 0
                    )
                    """);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (ds != null) ds.close();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    @Override
    public double getBalance(UUID uuid) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT balance FROM economy WHERE uuid=?")) {
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
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO economy(uuid,balance) VALUES(?,?) " +
                             "ON DUPLICATE KEY UPDATE balance=VALUES(balance)")) {
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
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
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
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
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
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT total_recharged FROM tb_accounts WHERE player_uuid=?")) {
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
