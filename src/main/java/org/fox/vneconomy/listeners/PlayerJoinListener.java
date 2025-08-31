package org.fox.vneconomy.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.api.EconomyAPI;

public class PlayerJoinListener implements Listener {
    private final VNEconomy plugin;
    public PlayerJoinListener(VNEconomy plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        EconomyAPI.get().set(e.getPlayer().getUniqueId(), EconomyAPI.get().getBalance(e.getPlayer().getUniqueId()));
    }
}
