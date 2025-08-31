package org.fox.vneconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.api.EconomyAPI;
import org.fox.vneconomy.api.EconomyProvider;
import org.fox.vneconomy.util.Currency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoneyCommand implements CommandExecutor, TabCompleter {

    private final VNEconomy plugin;
    public MoneyCommand(VNEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(plugin.msg("player-not-found").replace("%player%", "Console"));
                return true;
            }
            double bal = EconomyAPI.get().getBalance(p.getUniqueId());
            sender.sendMessage(plugin.msg("balance-self").replace("%amount%", format(bal)));
            play(p, "success");
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            if (!sender.hasPermission("vneco.use")) {
                sender.sendMessage(plugin.msg("no-permission")); return true;
            }
            int size = plugin.getConfig().getInt("leaderboard.size", 10);
            List<EconomyProvider.TopEntry> top = plugin.getTopManager().getCachedTop(size);
            sender.sendMessage(plugin.msg("top-header").replace("%size%", String.valueOf(size)));
            int pos = 1;
            for (EconomyProvider.TopEntry e : top) {
                sender.sendMessage(plugin.msg("top-line")
                        .replace("%pos%", String.valueOf(pos++))
                        .replace("%player%", e.name())
                        .replace("%amount%", format(e.amount())));
            }
            if (sender instanceof Player p) play(p, "top");
            return true;
        }

        if (args[0].equalsIgnoreCase("pay")) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Only players."); return true; }
            if (!sender.hasPermission("vneco.pay")) { sender.sendMessage(plugin.msg("no-permission")); return true; }
            if (args.length < 3) { sender.sendMessage("Usage: /money pay <player> <amount>"); return true; }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage(plugin.msg("player-not-found").replace("%player%", args[1])); return true; }
            if (target.getUniqueId().equals(p.getUniqueId())) { sender.sendMessage(plugin.msg("self-pay")); return true; }
            double amount;
            try { amount = Double.parseDouble(args[2]); } catch (Exception ex) { sender.sendMessage(plugin.msg("invalid-number")); return true; }
            if (amount <= 0) { sender.sendMessage(plugin.msg("negative-amount")); return true; }
            if (!EconomyAPI.get().take(p.getUniqueId(), amount)) {
                sender.sendMessage(plugin.msg("invalid-number").replace("Số tiền không hợp lệ.", "Bạn không đủ tiền."));
                play(p, "error");
                return true;
            }
            EconomyAPI.get().give(target.getUniqueId(), amount);
            p.sendMessage(plugin.msg("paid-sender").replace("%amount%", format(amount)).replace("%target%", target.getName()));
            target.sendMessage(plugin.msg("paid-receiver").replace("%amount%", format(amount)).replace("%sender%", p.getName()));
            play(p, "pay");
            play(target, "pay");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null) {
            double bal = EconomyAPI.get().getBalance(target.getUniqueId());
            sender.sendMessage(plugin.msg("balance-other")
                    .replace("%player%", target.getName())
                    .replace("%amount%", format(bal)));
            if (sender instanceof Player p) play(p, "success");
            return true;
        } else {
            sender.sendMessage(plugin.msg("player-not-found").replace("%player%", args[0]));
            if (sender instanceof Player p) play(p, "error");
            return true;
        }
    }

    private String format(double v) {
        String suffix = plugin.getConfig().getString("currency.suffix", " ₫");
        boolean shortForm = plugin.getConfig().getBoolean("currency.use-short-format", true);
        int decimals = plugin.getConfig().getInt("currency.decimals", 0);
        return Currency.formatVND(v, suffix, shortForm, decimals);
    }

    private void play(Player p, String key) {
        String s = plugin.getConfig().getString("sounds." + key, null);
        boolean enabled = plugin.getConfig().getBoolean("sounds.enabled-by-default", true);
        if (s == null || !enabled) return;
        try {
            p.playSound(p.getLocation(), Sound.valueOf(s), 1f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("vneco.use")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("top");
            completions.add("pay");
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pay")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
            return completions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("pay")) {
            completions.add("1000");
            completions.add("5000");
            completions.add("10000");
            return completions;
        }
        return Collections.emptyList();
    }
}
