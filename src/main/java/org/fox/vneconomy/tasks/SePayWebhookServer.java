package org.fox.vneconomy.tasks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.storage.Storage;
import org.fox.vneconomy.util.DiscordWebhookSender;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SePayWebhookServer extends NanoHTTPD {

    private final VNEconomy plugin;
    private final Storage storage;
    private static final Pattern ORDER_PATTERN = Pattern.compile("DH(\\d+)");

    public SePayWebhookServer(VNEconomy plugin, int port) {
        super(port);
        this.plugin = plugin;
        this.storage = plugin.getStorage();
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getMethod() != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                    "application/json", "{\"success\":false,\"message\":\"Only POST allowed\"}");
        }

        try {
            // Đọc raw JSON từ InputStream
            InputStream in = session.getInputStream();
            int length = Integer.parseInt(session.getHeaders().getOrDefault("content-length", "0"));
            byte[] buf = new byte[length];
            in.read(buf, 0, length);
            String body = new String(buf, StandardCharsets.UTF_8);

            if (body.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"success\":false,\"message\":\"Empty body\"}");
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            processTransaction(json);

            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true}");
        } catch (Exception e) {
            plugin.getLogger().warning("Lỗi xử lý webhook: " + e.getMessage());
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"success\":false}");
        }
    }


    public void processTransaction(JsonObject tx) {
        String description = tx.has("content") ? tx.get("content").getAsString() : "";
        double amount = tx.has("transferAmount") ? tx.get("transferAmount").getAsDouble() : 0;

        Matcher matcher = ORDER_PATTERN.matcher(description);
        if (!matcher.find()) return;

        int orderId = Integer.parseInt(matcher.group(1));

        try (Connection conn = storage.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT player_uuid, total, payment_status FROM tb_orders WHERE id=? AND payment_status='Unpaid'")) {

            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            double expected = rs.getDouble("total");
            if (Math.abs(expected - amount) > 0.001) return;

            UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));

            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE tb_orders SET payment_status='Paid' WHERE id=?")) {
                update.setInt(1, orderId);
                update.executeUpdate();
            }

            // Cộng tiền vào tài khoản
            CompletableFuture.runAsync(() -> storage.giveAsync(playerUUID, amount));

            // Lấy Player nếu online để thông báo trong game
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null && player.isOnline()) {
                    player.sendMessage("§aBạn đã nạp thành công §e" + amount + "đ §avào tài khoản!");
                    removeQrMap(player);

                    // 🔔 Gửi thông báo Discord
                    double newBalance = storage.getBalance(playerUUID); // lấy số dư mới
                    double totalRecharged = storage.getTotalRecharged(playerUUID); // tổng đã nạp
                    File embedFile = new File(plugin.getDataFolder(), "embed.yml");

                    DiscordWebhookSender sender = new DiscordWebhookSender(embedFile, plugin.getConfig());
                    sender.sendRecharge(player.getName(), amount, newBalance, totalRecharged);
                }
            });

            plugin.getLogger().info("Webhook: Cộng " + amount + " cho player " + playerUUID + " từ đơn hàng DH" + orderId);

        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi xử lý đơn hàng DH" + orderId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void removeQrMap(Player player) {
        player.getInventory().forEach(item -> {
            if (item != null && item.getType() == Material.FILLED_MAP &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().contains("QR Nạp Tiền")) {
                player.getInventory().remove(item);
            }
        });
    }
}
