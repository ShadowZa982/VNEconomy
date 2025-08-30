package org.fox.vneconomy.storage;

import org.bukkit.OfflinePlayer;
import org.fox.vneconomy.api.EconomyProvider;

import java.util.UUID;

public abstract class Storage implements EconomyProvider {
    public abstract void init();
    public abstract void close();

    public String nameOf(UUID uuid) {
        OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
        return op != null && op.getName() != null ? op.getName() : uuid.toString();
    }
}
