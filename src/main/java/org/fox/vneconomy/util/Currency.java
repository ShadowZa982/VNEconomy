package org.fox.vneconomy.util;

import java.text.DecimalFormat;

public class Currency {
    public static String formatVND(double amount, String suffix, boolean shortForm, int decimals) {
        if (shortForm) {
            if (amount >= 1_000_000_000) return trim(amount / 1_000_000_000D, decimals) + "B" + suffix;
            if (amount >= 1_000_000) return trim(amount / 1_000_000D, decimals) + "M" + suffix;
            if (amount >= 1_000) return trim(amount / 1_000D, decimals) + "K" + suffix;
        }
        DecimalFormat df = new DecimalFormat("#,###" + (decimals > 0 ? "." + "0".repeat(decimals) : ""));
        return df.format(amount) + suffix;
    }

    private static String trim(double v, int decimals) {
        String pattern = "0" + (decimals > 0 ? "." + "0".repeat(decimals) : "");
        return new java.text.DecimalFormat(pattern).format(v);
    }
}
