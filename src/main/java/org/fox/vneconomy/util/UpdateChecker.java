package org.fox.vneconomy.util;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.fox.vneconomy.VNEconomy;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {

    private final String apiUrl = "https://api.github.com/repos/ShadowZa982/VNEconomy/releases/latest";
    private String latestVersion = "";
    private boolean isNewVersion = false;

    public UpdateChecker(VNEconomy plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setInstanceFollowRedirects(true);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    Bukkit.getConsoleSender().sendMessage("[VNEconomy] §cKhông thể kiểm tra phiên bản mới!");
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                latestVersion = json.getString("tag_name").trim();

                if (!plugin.getDescription().getVersion().equals(latestVersion)) {
                    isNewVersion = true;
                    Bukkit.getConsoleSender().sendMessage("[VNEconomy] §aCó phiên bản mới §6" + latestVersion + "§a, vui lòng cập nhật!");
                    Bukkit.getConsoleSender().sendMessage("https://github.com/ShadowZa982/VNEconomy/releases/latest");
                }

            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("[VNEconomy] §cLỗi khi kiểm tra phiên bản mới");
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().isOp() && isNewVersion) {
            event.getPlayer().sendMessage("[VNEconomy] §aCó phiên bản mới §6" + latestVersion + "§a, vui lòng cập nhật!");
            event.getPlayer().sendMessage("§6https://github.com/ShadowZa982/VNEconomy/releases/latest");
        }
    }
}
