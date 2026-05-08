package com.demo.mota.engine.Item;

import com.demo.mota.engine.state.GameNumber;

public class Portion extends Item {
    private final GameNumber replyAmount;

    public Portion(String itemId, String itemName, String itemDescription,
                   long itemPrice, int itemCount,
                   boolean isStorable, boolean isConsumable,
                   GameNumber replyAmount) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.replyAmount = replyAmount;
    }

    public GameNumber getReplyAmount() {
        return replyAmount;
    }
}
