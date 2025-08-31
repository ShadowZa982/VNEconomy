package org.fox.vneconomy.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DiscordWebhookSender {

    private final FileConfiguration embedConfig;
    private final FileConfiguration pluginConfig;

    public DiscordWebhookSender(File embedFile, FileConfiguration pluginConfig) {
        this.embedConfig = YamlConfiguration.loadConfiguration(embedFile);
        this.pluginConfig = pluginConfig;
    }

    public void sendRecharge(String playerName, double amount, double newBalance, double totalRecharged) {
        if (!pluginConfig.getBoolean("webhook.enabled", false)) return;

        String webhookUrl = pluginConfig.getString("webhook.url", "").trim();
        if (webhookUrl.isEmpty()) return;

        try {
            String dateStr = new SimpleDateFormat("dd/MM/yyyy • HH:mm:ss").format(new Date());

            String amountStr = String.format("%,d VNĐ", (long) amount);
            String balanceStr = String.format("%,d VNĐ", (long) newBalance);
            String totalStr = String.format("%,d VNĐ", (long) totalRecharged);

            String title = embedConfig.getString("title", "💎 GIAO DỊCH THÀNH CÔNG 💎");
            String separator = embedConfig.getString("separator", "═══════════════════════");
            String transactionInfo = embedConfig.getString("transaction_info", "🎮 THÔNG TIN GIAO DỊCH");
            String footer = embedConfig.getString("footer", "").replace("\n", "\\n");

            var fields = embedConfig.getConfigurationSection("fields");
            String accountField = fields != null ? fields.getString("account", "🏆 Tài Khoản") : "🏆 Tài Khoản";
            String amountField = fields != null ? fields.getString("amount", "💰 Số Tiền Nạp") : "💰 Số Tiền Nạp";
            String balanceField = fields != null ? fields.getString("balance", "💳 Số Dư Mới") : "💳 Số Dư Mới";
            String totalField = fields != null ? fields.getString("total_recharged", "📊 Tổng Đã Nạp") : "📊 Tổng Đã Nạp";
            String dateField = fields != null ? fields.getString("date", "⏰ Thời Gian") : "⏰ Thời Gian";
            String statusField = fields != null ? fields.getString("status", "[ ✅ Giao dịch đã được xử lý tự động ]")
                    : "[ ✅ Giao dịch đã được xử lý tự động ]";

            String descriptionBlock = "```" + "\\n" + separator + "\\n" + transactionInfo + "\\n" + separator + "\\n```";
            String statusBlock = "```" + statusField + "```";

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"").append(title).append("\",")
                    .append("\"description\":\"").append(descriptionBlock).append("\",")
                    .append("\"color\":3066993,")
                    .append("\"fields\":[")
                    .append("{\"name\":\"").append(accountField).append("\",\"value\":\"").append(playerName).append("\",\"inline\":true},")
                    .append("{\"name\":\"").append(balanceField).append("\",\"value\":\"").append(balanceStr).append("\",\"inline\":true},")
                    .append("{\"name\":\"").append(amountField).append("\",\"value\":\"").append(amountStr).append("\",\"inline\":true},")
                    .append("{\"name\":\"").append(totalField).append("\",\"value\":\"").append(totalStr).append("\",\"inline\":true},")
                    .append("{\"name\":\"").append(dateField).append("\",\"value\":\"").append(dateStr).append("\",\"inline\":false},")
                    .append("{\"name\":\"Trạng Thái\",\"value\":\"").append(statusBlock).append("\",\"inline\":false}")
                    .append("],")
                    .append("\"footer\":{\"text\":\"").append(footer).append("\"}");

            json.append("}]}");

            String jsonPayload = json.toString();

            URL url = new URL(webhookUrl);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = con.getResponseCode();
            if (responseCode / 100 != 2) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) error.append(line);
                    System.err.println("[Webhook Error] HTTP " + responseCode + ": " + error);
                }
            }

            con.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
