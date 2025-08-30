package org.fox.vneconomy.api;

import java.util.UUID;

public class VNEconomyAPI {

    public static double getBalance(UUID uuid) {
        return EconomyAPI.getBalance(uuid);
    }

    public static boolean withdraw(UUID uuid, double amount) {
        return EconomyAPI.take(uuid, amount);
    }

    public static void deposit(UUID uuid, double amount) {
        EconomyAPI.give(uuid, amount);
    }

    public static void set(UUID uuid, double amount) {
        EconomyAPI.set(uuid, amount);
    }

    public static boolean has(UUID uuid, double amount) {
        return EconomyAPI.has(uuid, amount);
    }
}
