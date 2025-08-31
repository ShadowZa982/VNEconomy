package org.fox.vneconomy.tst;

import org.bukkit.entity.Player;

public class Card {
    private String player;
    private CardType cardType;
    private int cardPrice;
    private String serial;
    private String pin;

    private String transactionID = null;
    private String randomMD5 = null;
    private int retry = 0;

    public Card(Player p, CardType type, int price, String serial, String pin) {
        this.player = p.getName();
        this.cardType = type;
        this.cardPrice = price;
        this.serial = serial;
        this.pin = pin;
    }

    public String getPlayer() { return player; }
    public CardType getCardType() { return cardType; }
    public int getCardPrice() { return cardPrice; }
    public String getSerial() { return serial; }
    public String getPin() { return pin; }

    // transactionID
    public String getTransactionID() { return transactionID; }
    public void setTransactionID(String transactionID) { this.transactionID = transactionID; }

    // randomMD5
    public String getRandomMD5() { return randomMD5; }
    public void setRandomMD5(String randomMD5) { this.randomMD5 = randomMD5; }

    // retry
    public int getRetry() { return retry; }
    public void setRetry(int retry) { this.retry = retry; }
}

