package org.fox.vneconomy.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.qr.QrMapRenderer;
import org.fox.vneconomy.storage.Storage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class NapQrCommand implements CommandExecutor {

    private final VNEconomy plugin;
    private final Storage storage;
    private final String bank, acc, template, prefix;

    public NapQrCommand(VNEconomy plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
        this.bank = plugin.getConfig().getString("sepay.bank", "MBBank");
        this.acc = plugin.getConfig().getString("sepay.account", "0000000000");
        this.template = plugin.getConfig().getString("sepay.template", "compact");
        this.prefix = plugin.getConfig().getString("sepay.prefix", "NapTien");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cLệnh này chỉ dành cho người chơi.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cSử dụng: /napqr <sotien>");
            return true;
        }

        Player player = (Player) sender;
        int amount;
        try { amount = Integer.parseInt(args[0]); }
        catch (NumberFormatException e) {
            player.sendMessage("§cSố tiền không hợp lệ!");
            return true;
        }
        if (amount < 1000) {
            player.sendMessage("§cSố tiền tối thiểu là 1,000đ!");
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int orderId = createOrder(player, amount);
            if (orderId == -1) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cKhông thể tạo đơn hàng, thử lại sau!"));
                return;
            }

            try {
                File file = new File(plugin.getDataFolder(), "pending_transactions.json");
                JsonArray arr = new JsonArray();
                if(file.exists()) {
                    try (FileReader fr = new FileReader(file)) {
                        arr = com.google.gson.JsonParser.parseReader(fr).getAsJsonArray();
                    } catch (Exception ignored){}
                }
                JsonObject tx = new JsonObject();
                tx.addProperty("content", prefix + "_DH" + orderId);
                tx.addProperty("transferAmount", amount);
                tx.addProperty("source", "order");
                tx.addProperty("expireAt", System.currentTimeMillis() + 60000);
                arr.add(tx);
                try (FileWriter fw = new FileWriter(file)) { fw.write(arr.toString()); }
            } catch (Exception e) {
                plugin.getLogger().warning("Không thể lưu pending transaction: " + e.getMessage());
            }

            try {
                String des = prefix + "_DH" + orderId;
                String encodedDes = URLEncoder.encode(des, StandardCharsets.UTF_8);
                String qrUrl = String.format("https://qr.sepay.vn/img?bank=%s&acc=%s&template=%s&amount=%d&des=%s",
                        bank, acc, template, amount, encodedDes);

                BufferedImage image = ImageIO.read(new java.net.URL(qrUrl));
                if (image == null) throw new IllegalStateException("Không thể tải Qr!");

                ItemStack mapItem = QrMapRenderer.createQrMap(player, image);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.getInventory().addItem(mapItem);
                    player.sendMessage("§aĐã tạo QR cho đơn hàng §eDH" + orderId + " §a(nạp " + amount + "đ).");
                    player.sendMessage("§7Nội dung chuyển khoản: §fDH" + orderId);
                });

            } catch (Exception ex) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cLỗi khi tạo QR: " + ex.getMessage()));
                ex.printStackTrace();
            }
        });

        return true;
    }

    private int createOrder(Player player, int amount) {
        try (Connection conn = storage.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO tb_orders (player_uuid, total, payment_status) VALUES (?, ?, 'Unpaid')",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, player.getUniqueId().toString());
                ps.setDouble(2, amount);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Lỗi tạo order: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }
}
