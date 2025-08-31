package org.fox.vneconomy.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.api.EconomyAPI;
import org.fox.vneconomy.api.EconomyProvider;
import org.fox.vneconomy.util.Currency;

import java.util.List;

public class VNEconomyExpansion extends PlaceholderExpansion {

    private final VNEconomy plugin;

    public VNEconomyExpansion(VNEconomy plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "vneco"; }
    @Override public String getAuthor() { return "FoxTM"; }
    @Override public String getVersion() { return "1.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (params.equalsIgnoreCase("balance")) {
            if (p == null) return "0";
            return String.valueOf(EconomyAPI.get().getBalance(p.getUniqueId()));
        }
        if (params.equalsIgnoreCase("balance_formatted")) {
            if (p == null) return "0";
            return Currency.formatVND(
                EconomyAPI.get().getBalance(p.getUniqueId()),
                plugin.getConfig().getString("currency.suffix", " ₫"),
                plugin.getConfig().getBoolean("currency.use-short-format", true),
                plugin.getConfig().getInt("currency.decimals", 0)
            );
        }
        if (params.startsWith("top_")) {
            String[] sp = params.split("_");
            if (sp.length >= 3) {
                try {
                    int n = Integer.parseInt(sp[1]);
                    List<EconomyProvider.TopEntry> top = plugin.getTopManager().getCachedTop(Math.max(10, n));
                    if (n <= top.size() && n > 0) {
                        EconomyProvider.TopEntry e = top.get(n - 1);
                        if (sp[2].equalsIgnoreCase("name")) return e.name();
                        if (sp[2].equalsIgnoreCase("amount"))
                            return Currency.formatVND(e.amount(),
                                plugin.getConfig().getString("currency.suffix", " ₫"),
                                plugin.getConfig().getBoolean("currency.use-short-format", true),
                                plugin.getConfig().getInt("currency.decimals", 0));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}
