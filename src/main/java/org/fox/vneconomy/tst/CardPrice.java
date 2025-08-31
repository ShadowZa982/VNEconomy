package org.fox.vneconomy.tst;

public enum CardPrice {
    _10K(10000, 1),
    _20K(20000, 2),
    _50K(50000, 3),
    _100K(100000, 4),
    _200K(200000, 5),
    _500K(500000, 6),
    _1M(1000000, 7),
    UNKNOWN(0, -1);

    private final int price;
    private final int id;

    CardPrice(int price, int id) {
        this.price = price;
        this.id = id;
    }

    public static CardPrice getPrice(int price) {
        for (CardPrice cp : values()) {
            if (cp.price == price) return cp;
        }
        return UNKNOWN;
    }

    public int getPrice() {
        return price;
    }

    public int getId() {
        return id;
    }
}
