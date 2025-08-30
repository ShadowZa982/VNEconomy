package org.fox.vneconomy.api;

import org.bukkit.Bukkit;
import org.fox.vneconomy.events.EconomyChangeEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * EconomyAPI - API chính cho plugin khác sử dụng.
 *
 * Dùng trực tiếp: EconomyAPI.getBalance(uuid), EconomyAPI.give(uuid, amount), ...
 */
public class EconomyAPI {
    private static EconomyProvider provider;

    public static void init(EconomyProvider p) { provider = p; }
    public static EconomyProvider get() { return provider; }

    // ==============================
    //  Sync Methods
    // ==============================
    public static double getBalance(UUID uuid) {
        return provider.getBalance(uuid);
    }

    public static boolean has(UUID uuid, double amount) {
        return provider.getBalance(uuid) >= amount;
    }

    public static void set(UUID uuid, double amount) {
        double oldBal = provider.getBalance(uuid);

        EconomyChangeEvent event = new EconomyChangeEvent(uuid, oldBal, amount, "SET", false);
        Bukkit.getPluginManager().callEvent(event);

        provider.set(uuid, amount);
    }

    public static void give(UUID uuid, double amount) {
        double oldBal = provider.getBalance(uuid);
        double newBal = oldBal + amount;

        EconomyChangeEvent event = new EconomyChangeEvent(uuid, oldBal, newBal, "GIVE", false);
        Bukkit.getPluginManager().callEvent(event);

        provider.give(uuid, amount);
    }

    public static boolean take(UUID uuid, double amount) {
        double oldBal = provider.getBalance(uuid);
        double newBal = oldBal - amount;

        if (oldBal < amount) return false; // không đủ tiền

        EconomyChangeEvent event = new EconomyChangeEvent(uuid, oldBal, newBal, "TAKE", false);
        Bukkit.getPluginManager().callEvent(event);

        return provider.take(uuid, amount);
    }

    // ==============================
    //  Async Methods
    // ==============================
    public static CompletableFuture<Double> getBalanceAsync(UUID uuid) {
        return provider.getBalanceAsync(uuid);
    }

    public static CompletableFuture<Void> setAsync(UUID uuid, double amount) {
        return provider.setAsync(uuid, amount);
    }

    public static CompletableFuture<Void> giveAsync(UUID uuid, double amount) {
        return provider.giveAsync(uuid, amount);
    }

    public static CompletableFuture<Boolean> takeAsync(UUID uuid, double amount) {
        return provider.takeAsync(uuid, amount);
    }
}
