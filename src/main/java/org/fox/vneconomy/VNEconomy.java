package org.fox.vneconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.fox.vneconomy.api.EconomyAPI;
import org.fox.vneconomy.commands.EcoAdminCommand;
import org.fox.vneconomy.commands.MoneyCommand;
import org.fox.vneconomy.leaderboard.TopManager;
import org.fox.vneconomy.listeners.PlayerJoinListener;
import org.fox.vneconomy.placeholders.VNEconomyExpansion;
import org.fox.vneconomy.storage.*;
import org.fox.vneconomy.util.Msg;

import java.io.File;
import java.util.Objects;

public class VNEconomy extends JavaPlugin {

    private static VNEconomy instance;
    private Storage storage;
    private TopManager topManager;
    private File dataFile;
    private FileConfiguration dataConfig;

    public static VNEconomy get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResourceIfNotExists("messages.yml");
        saveResourceIfNotExists("data.yml");

        dataFile = new File(getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Storage
        reloadStorage();

        // API singleton
        EconomyAPI.init(storage);

        // Commands
        Objects.requireNonNull(getCommand("vnmoney")).setExecutor(new MoneyCommand(this));
        Objects.requireNonNull(getCommand("vneco")).setExecutor(new EcoAdminCommand(this));

        // Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Top manager
        topManager = new TopManager(this, storage);
        topManager.start();

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new VNEconomyExpansion(this).register();
            getLogger().info("PlaceholderAPI found, expansion registered.");
        }

        logStartupMessage(true);
    }

    @Override
    public void onDisable() {
        if (topManager != null) topManager.stop();
        if (storage != null) storage.close();
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
            case "sqlite" -> storage = new SQLiteStorage(this);
            default -> storage = new YamlStorage(this);
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
            logWithColor("&b========== &fKazami Studio &b==========");
            logWithColor("&7[&a✔&7] &fPlugin: &b" + getDescription().getName());
            logWithColor("&7[&a✔&7] &fVersion: &a" + getDescription().getVersion());
            logWithColor("&7[&a✔&7] &fAuthor: &eKazami Studio");
            logWithColor("&7[&a✔&7] &fDiscord: &9https://discord.gg/kQsg6JyT");
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
