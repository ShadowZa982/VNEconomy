package org.fox.vneconomy.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.fox.vneconomy.VNEconomy;
import org.fox.vneconomy.storage.Storage;
import org.fox.vneconomy.tst.PlayerCardChargedEvent;

public class CardListener implements Listener {

    private final VNEconomy plugin;

    public CardListener(VNEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCardSuccess(PlayerCardChargedEvent e) {
        int amount = e.getCardPrice();

        Storage storage = plugin.getStorage();
        storage.giveAsync(e.getPlayer().getUniqueId(), amount).thenRun(() -> {
            e.getPlayer().sendMessage("§aBạn vừa nạp thành công thẻ " + e.getCardType() +
                    " mệnh giá " + amount + "đ");
            e.getPlayer().sendMessage("§aSố dư mới của bạn: " + storage.getBalance(e.getPlayer().getUniqueId()));
        });
    }
}