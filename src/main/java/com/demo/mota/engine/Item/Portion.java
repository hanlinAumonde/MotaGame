package com.demo.mota.engine.Item;

import java.math.BigInteger;

public class Portion extends Item {
    private final BigInteger replyAmount;

    public Portion(String itemId, String itemName, String itemDescription,
                   long itemPrice, int itemCount,
                   boolean isStorable, boolean isConsumable,
                   BigInteger replyAmount) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.replyAmount = replyAmount;
    }

    public BigInteger getReplyAmount() {
        return replyAmount;
    }
}
