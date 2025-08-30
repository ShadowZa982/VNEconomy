package org.fox.vneconomy.api;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyProvider {
    double getBalance(UUID uuid);
    void set(UUID uuid, double amount);
    void give(UUID uuid, double amount);
    boolean take(UUID uuid, double amount);

    CompletableFuture<Double> getBalanceAsync(UUID uuid);
    CompletableFuture<Void> setAsync(UUID uuid, double amount);
    CompletableFuture<Void> giveAsync(UUID uuid, double amount);
    CompletableFuture<Boolean> takeAsync(UUID uuid, double amount);
    List<TopEntry> getTop(int size);

    record TopEntry(UUID uuid, String name, double amount) {}
}
