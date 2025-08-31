package org.fox.vneconomy.tst;

public enum CardType {
    VIETTEL("VTEL"),
    MOBIFONE("MOBI"),
    VINAPHONE("VINA"),
    ZING("ZING"),
    VNMOBI("VNM"),
    GATE("GATE");

    private final String apiName;

    CardType(String apiName) { this.apiName = apiName; }

    public String getApiName() { return apiName; }

    public static boolean isSupported(String name) {
        try {
            CardType.valueOf(name.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
