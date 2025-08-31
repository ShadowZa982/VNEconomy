package org.fox.vneconomy.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class TheSieuTocBridge {

    public static boolean isAvailable() {
        try {
            Class.forName("net.thesieutoc.api.ThesieutocAPI");
            return Bukkit.getPluginManager().getPlugin("TheSieuToc") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Build net.thesieutoc.api.Card via reflection and call ThesieutocAPI.processCard(player, card)
     */
    public static boolean processCard(Player p, String cardType, int price, String seri, String pin) {
        try {
            Class<?> cardCls = Class.forName("net.thesieutoc.api.Card");
            Constructor<?> cons = null;
            try {
                cons = cardCls.getConstructor(Player.class, String.class, int.class, String.class, String.class);
            } catch (NoSuchMethodException ignored) {}

            Object card;
            if (cons != null) {
                card = cons.newInstance(p, cardType, price, seri, pin);
            } else {
                card = cardCls.getConstructor().newInstance();
                // fallback to builder methods
                Method mPlayer = cardCls.getMethod("player", String.class);
                Method mType = cardCls.getMethod("cardType", String.class);
                Method mPrice = cardCls.getMethod("cardPrice", int.class);
                Method mSeri = cardCls.getMethod("SERIAL", String.class);
                Method mPin = cardCls.getMethod("PIN", String.class);
                mPlayer.invoke(card, p.getName());
                mType.invoke(card, cardType);
                mPrice.invoke(card, price);
                mSeri.invoke(card, seri);
                mPin.invoke(card, pin);
            }

            Class<?> apiCls = Class.forName("net.thesieutoc.api.ThesieutocAPI");
            Method process = apiCls.getMethod("processCard", Player.class, cardCls);
            process.invoke(null, p, card);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    /**
     * Register a runtime listener for PlayerCardChargedEvent without compile-time dependency.
     * On success, run onSuccess callback with (Player, int price, String type)
     */
    public static void registerChargedListener(Plugin plugin, CardSuccessHandler handler) {
        try {
            Class<?> eventCls = Class.forName("net.thesieutoc.event.PlayerCardChargedEvent");

            EventExecutor exec = new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) {
                    if (!eventCls.isInstance(event)) return;
                    try {
                        Method getPlayer = eventCls.getMethod("getPlayer");
                        Method getCardPrice = eventCls.getMethod("getCardPrice");
                        Method getCardType = eventCls.getMethod("getCardType");

                        Player p = (Player) getPlayer.invoke(event);
                        int price = (int) getCardPrice.invoke(event);
                        String type = (String) getCardType.invoke(event);

                        handler.onSuccess(p, type, price);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };

            Listener dummy = new Listener() {};
            Bukkit.getPluginManager().registerEvent(
                    (Class<? extends Event>) eventCls,
                    dummy,
                    EventPriority.NORMAL,
                    exec,
                    plugin,
                    true
            );
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("TheSieuToc not found; card success listener not registered.");
        }
    }

    @FunctionalInterface
    public interface CardSuccessHandler {
        void onSuccess(Player p, String cardType, int price);
    }
}
