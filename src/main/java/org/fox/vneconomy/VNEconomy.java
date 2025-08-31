package org.fox.vneconomy;

import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.fox.vneconomy.api.EconomyAPI;
import org.fox.vneconomy.commands.EcoAdminCommand;
import org.fox.vneconomy.commands.MoneyCommand;
import org.fox.vneconomy.commands.NapQrCommand;
import org.fox.vneconomy.commands.NapTheCommand;
import org.fox.vneconomy.leaderboard.TopManager;
import org.fox.vneconomy.listeners.CardListener;
import org.fox.vneconomy.listeners.PlayerJoinListener;
import org.fox.vneconomy.placeholders.VNEconomyExpansion;
import org.fox.vneconomy.storage.*;
import org.fox.vneconomy.tasks.SePayWebhookServer;
import org.fox.vneconomy.tst.CallbackServer;
import org.fox.vneconomy.tst.TheSieuTocAPI;
import org.fox.vneconomy.util.Msg;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class VNEconomy extends JavaPlugin {

    private static VNEconomy instance;
    private Storage storage;
    private TopManager topManager;
    private File dataFile;
    private FileConfiguration dataConfig;

    public static VNEconomy get() {
        return instance;
    }
    private CallbackServer callbackServer;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResourceIfNotExists("messages.yml");
        saveResourceIfNotExists("data.yml");

        dataFile = new File(getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        reloadStorage();

        EconomyAPI.init(storage);
        TheSieuTocAPI.init(this);

        var vnmoney = Objects.requireNonNull(getCommand("vnmoney"));
        vnmoney.setExecutor(new MoneyCommand(this));
        vnmoney.setTabCompleter(new MoneyCommand(this));

        var vneco = Objects.requireNonNull(getCommand("vneco"));
        vneco.setExecutor(new EcoAdminCommand(this));
        vneco.setTabCompleter(new EcoAdminCommand(this));

        var napqr = Objects.requireNonNull(getCommand("napqr"));
        napqr.setExecutor(new NapQrCommand(this));

        org.fox.vneconomy.tst.TheSieuTocAPI.init(this);
        getCommand("napthe").setExecutor(new NapTheCommand());
        getServer().getPluginManager().registerEvents(new CardListener(this), this);


        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        topManager = new TopManager(this, storage);
        topManager.start();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new VNEconomyExpansion(this).register();
            getLogger().info("PlaceholderAPI found, expansion registered.");
        }

        if (getConfig().getBoolean("sepay.webhook.enabled", true)) {
            int port = getConfig().getInt("sepay.webhook.port", 8080);
            try {
                SePayWebhookServer server = new SePayWebhookServer(this, port);
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                getLogger().info("SePay Webhook server running on " + port);
            } catch (Exception e) {
                getLogger().severe("Không thể khởi động webhook server: " + e.getMessage());
            }
        }

        if (getConfig().getBoolean("thesieutoc.webhook.enabled", true)) {
            int port = getConfig().getInt("thesieutoc.webhook.port", 8080);
            String path = getConfig().getString("thesieutoc.webhook.path", "/callback");

            try {
                callbackServer = new CallbackServer(this, port, path);
            } catch (IOException e) {
                getLogger().severe("❌ Không mở được callback server: " + e.getMessage());
            }
        }


        long expireMs = getConfig().getLong("order_expire_ms", 60000);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try (Connection conn = storage.getConnection()) {

                String sql;
                if (storage instanceof MySQLStorage) {
                    sql = "SELECT id, player_uuid FROM tb_orders " +
                            "WHERE payment_status='Unpaid' AND created_at < (NOW() - INTERVAL ? SECOND)";
                } else {
                    sql = "SELECT id, player_uuid FROM tb_orders " +
                            "WHERE payment_status='Unpaid' AND created_at < datetime('now', '-' || ? || ' seconds')";
                }

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, expireMs / 1000);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int orderId = rs.getInt("id");
                            UUID uuid = UUID.fromString(rs.getString("player_uuid"));

                            // Update trạng thái
                            try (PreparedStatement up = conn.prepareStatement(
                                    "UPDATE tb_orders SET payment_status='Expired' WHERE id=?")) {
                                up.setInt(1, orderId);
                                up.executeUpdate();
                            }

                            // Thông báo + xóa map
                            Bukkit.getScheduler().runTask(this, () -> {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null && player.isOnline()) {
                                    player.sendMessage("§c⏰ Đơn hàng §eDH" + orderId + " §cđã hết hạn!");

                                    Iterator<ItemStack> it = player.getInventory().iterator();
                                    while (it.hasNext()) {
                                        ItemStack item = it.next();
                                        if (item != null && item.getType() == Material.FILLED_MAP &&
                                                item.hasItemMeta() &&
                                                item.getItemMeta().hasDisplayName() &&
                                                item.getItemMeta().getDisplayName().contains("QR Nạp Tiền")) {
                                            it.remove();
                                        }
                                    }
                                }
                            });
                        }
                    }
                }

            } catch (Exception e) {
                getLogger().warning("Lỗi check expire order: " + e.getMessage());
                e.printStackTrace();
            }
        }, 20L, 200L); // delay 1s, repeat 10s

        logStartupMessage(true);
    }

    @Override
    public void onDisable() {
        if (topManager != null) topManager.stop();
        if (storage != null) storage.close();
        if (callbackServer != null) {
            callbackServer.stop();
        }
        saveDataYaml();
        logStartupMessage(false);
    }

    public void reloadAll() {
        reloadConfig();
        reloadDataYaml();
        reloadStorage();
        EconomyAPI.init(storage);
        if (topManager != null) {
            topManager.stop();
        }
        topManager = new TopManager(this, storage);
        topManager.start();
    }

    private void reloadStorage() {
        if (storage != null) storage.close();
        String type = getConfig().getString("storage.type", "yaml").toLowerCase();
        switch (type) {
            case "mysql" -> storage = new MySQLStorage(this);
            default -> storage = new SQLiteStorage(this);
        }
        storage.init();
    }

    private void saveResourceIfNotExists(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists()) {
            saveResource(name, false);
        }
    }

    public Storage getStorage() {
        return storage;
    }

    public TopManager getTopManager() {
        return topManager;
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    public void saveDataYaml() {
        try {
            dataConfig.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reloadDataYaml() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public String msg(String path) {
        String prefix = getConfig().getString("messages.prefix", "");
        String m = getConfig().getString("messages." + path, "");
        return Msg.color(prefix + m);
    }

    private void logStartupMessage(boolean enable) {
        if (enable) {
            logWithColor("&b========== &Fox Studio &b==========");
            logWithColor("&7[&a✔&7] &fPlugin: &b" + getDescription().getName());
            logWithColor("&7[&a✔&7] &fVersion: &a" + getDescription().getVersion());
            logWithColor("&7[&a✔&7] &fAuthor: &Fox Studio");
            logWithColor("&7[&a✔&7] &fDiscord: &9https://discord.gg/SFfB7gg2vr");
            logWithColor("&7[&a✔&7] &6" + getDescription().getName() + " &ađã được bật!");
            logWithColor("&b=====================================");
            logWithColor("");
        } else {
            logWithColor("&c[&6"+ getDescription().getName() + "&c] Plugin đã tắt.");
        }
    }

    private void logWithColor(String msg) {
        getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }
}
