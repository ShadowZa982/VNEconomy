package org.fox.vneconomy.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class EconomyChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID uuid;
    private final double oldBalance;
    private final double newBalance;
    private final String reason;

    public EconomyChangeEvent(UUID uuid, double oldBalance, double newBalance, String reason, boolean async) {
        super(async);
        this.uuid = uuid;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.reason = reason;
    }

    public UUID getUuid() { return uuid; }
    public double getOldBalance() { return oldBalance; }
    public double getNewBalance() { return newBalance; }
    public String getReason() { return reason; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
