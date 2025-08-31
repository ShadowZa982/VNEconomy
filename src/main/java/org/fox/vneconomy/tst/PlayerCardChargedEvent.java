package org.fox.vneconomy.tst;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerCardChargedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final String cardType;
    private final int cardPrice;

    public PlayerCardChargedEvent(Player player, String cardType, int cardPrice) {
        this.player = player;
        this.cardType = cardType;
        this.cardPrice = cardPrice;
    }

    public Player getPlayer() { return player; }
    public String getCardType() { return cardType; }
    public int getCardPrice() { return cardPrice; }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
