package org.fox.vneconomy.tst;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.fox.vneconomy.VNEconomy;

import java.util.HashMap;
import java.util.Map;

public class TheSieuTocAPI {

    private static String apiKey;
    private static VNEconomy plugin;

    private static final Map<String, Card> pendingCards = new HashMap<>();

    public static void init(VNEconomy pl) {
        plugin = pl;
        apiKey = plugin.getConfig().getString("TheSieuToc-API.key", "");
    }

    public static void processCard(Player p, Card card) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (card.getCardPrice() <= 0 || card.getCardType() == null) {
                p.sendMessage("§cThẻ không hợp lệ!");
                return;
            }

            String content = Utils.randomMD5();
            card.setRandomMD5(content);

            try {
                String url = "https://thesieutoc.net/chargingws/v2";
                String postData = "APIkey=" + apiKey
                        + "&mathe=" + card.getPin()
                        + "&seri=" + card.getSerial()
                        + "&type=" + card.getCardType().name()
                        + "&menhgia=" + card.getCardPrice()
                        + "&content=" + content;

                JsonObject response = Utils.postJson(url, postData);

                if (response == null) {
                    p.sendMessage("§cKhông kết nối được đến hệ thống nạp thẻ!");
                    return;
                }

                String status = response.has("status") ? response.get("status").getAsString() : "";
                if ("00".equals(status)) {
                    synchronized (pendingCards) {
                        pendingCards.put(content, card);
                    }
                    p.sendMessage("§aThẻ đã gửi lên hệ thống, vui lòng chờ xử lý...");
                } else {
                    String msg = response.has("msg") ? response.get("msg").getAsString() : "Lỗi không xác định!";
                    p.sendMessage("§cNạp thẻ thất bại: " + msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                p.sendMessage("§cCó lỗi khi gửi thẻ!");
            }
        });
    }

    public static void handleCallback(String content, String status, int amount) {
        Card card;
        synchronized (pendingCards) {
            card = pendingCards.remove(content);
        }
        if (card == null) return;

        Player p = Bukkit.getPlayer(card.getPlayer());
        if (p == null || !p.isOnline()) return;

        if ("thanhcong".equalsIgnoreCase(status)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getPluginManager().callEvent(
                        new PlayerCardChargedEvent(p, card.getCardType().getApiName(), amount)
                );
                p.sendMessage("§aNạp thẻ thành công, mệnh giá: " + amount);
            });
        } else {
            Bukkit.getScheduler().runTask(plugin, () ->
                    p.sendMessage("§cNạp thẻ thất bại! (" + status + ")")
            );
        }
    }

    public static void checkStatus(Card card) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String url = "https://thesieutoc.net/chargingws/status_card";
            String postData = "APIkey=" + apiKey + "&content=" + card.getRandomMD5();
            JsonObject response = Utils.postJson(url, postData);

            if (response != null) {
                String status = response.has("status") ? response.get("status").getAsString() : "";
                int amount = response.has("amount") ? response.get("amount").getAsInt() : 0;
                handleCallback(card.getRandomMD5(), status, amount);
            }
        });
    }

    public static int getMappedPrice(int inputPrice) {
        if (plugin == null) return -1;
        return plugin.getConfig().getInt("TheSieuToc-API.prices." + inputPrice, -1);
    }
}
