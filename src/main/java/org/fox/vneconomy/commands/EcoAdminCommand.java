package org.fox.vneconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.api.EconomyAPI;
import org.fox.vneconomy.util.Currency;

import java.util.UUID;

public class EcoAdminCommand implements CommandExecutor {

    private final VNEconomy plugin;
    public EcoAdminCommand(VNEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vneco.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/eco <give|set|take|reload> <player> <amount>");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            plugin.reloadAll();
            sender.sendMessage(plugin.msg("reloaded"));
            play(sender, "success");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("/eco " + sub + " <player> <amount>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage(plugin.msg("player-not-found").replace("%player%", args[1]));
            play(sender, "error");
            return true;
        }

        double amount;
        try { amount = Double.parseDouble(args[2]); } catch (Exception e) {
            sender.sendMessage(plugin.msg("invalid-number"));
            play(sender, "error");
            return true;
        }
        if (amount < 0) {
            sender.sendMessage(plugin.msg("negative-amount"));
            play(sender, "error");
            return true;
        }

        UUID uuid = target.getUniqueId();
        switch (sub) {
            case "set" -> {
                EconomyAPI.get().set(uuid, amount);
                sender.sendMessage(plugin.msg("set").replace("%player%", target.getName() == null ? uuid.toString() : target.getName()).replace("%amount%", Currency.formatVND(amount, plugin.getConfig().getString("currency.suffix", " ₫"), plugin.getConfig().getBoolean("currency.use-short-format", true), plugin.getConfig().getInt("currency.decimals", 0))));
                play(sender, "success");
            }
            case "give" -> {
                EconomyAPI.get().give(uuid, amount);
                sender.sendMessage(plugin.msg("give").replace("%player%", target.getName() == null ? uuid.toString() : target.getName()).replace("%amount%", Currency.formatVND(amount, plugin.getConfig().getString("currency.suffix", " ₫"), plugin.getConfig().getBoolean("currency.use-short-format", true), plugin.getConfig().getInt("currency.decimals", 0))));
                play(sender, "success");
            }
            case "take" -> {
                boolean ok = EconomyAPI.get().take(uuid, amount);
                if (!ok) { sender.sendMessage("Người chơi không đủ tiền."); play(sender, "error"); return true; }
                sender.sendMessage(plugin.msg("take").replace("%player%", target.getName() == null ? uuid.toString() : target.getName()).replace("%amount%", Currency.formatVND(amount, plugin.getConfig().getString("currency.suffix", " ₫"), plugin.getConfig().getBoolean("currency.use-short-format", true), plugin.getConfig().getInt("currency.decimals", 0))));
                play(sender, "success");
            }
            default -> sender.sendMessage("/eco <give|set|take|reload> <player> <amount>");
        }
        return true;
    }

    private void play(CommandSender sender, String key) {
        if (sender instanceof Player p) {
            String s = plugin.getConfig().getString("sounds." + key, null);
            boolean enabled = plugin.getConfig().getBoolean("sounds.enabled-by-default", true);
            if (s == null || !enabled) return;
            try {
                p.playSound(p.getLocation(), Sound.valueOf(s), 1f, 1f);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
