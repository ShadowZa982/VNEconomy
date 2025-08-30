package org.fox.vneconomy.util;

import net.md_5.bungee.api.ChatColor;

public class Msg {
    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
