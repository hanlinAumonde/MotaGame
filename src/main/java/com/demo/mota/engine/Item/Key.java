package com.demo.mota.engine.Item;

import com.demo.mota.engine.enums.KeyColor;

public class Key extends Item {
    private final KeyColor keyColor;

    public Key(String itemId, String itemName, String itemDescription,
               long itemPrice, int itemCount,
               boolean isStorable, boolean isConsumable,
               KeyColor keyColor) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.keyColor = keyColor;
    }

    public KeyColor getKeyColor() {
        return keyColor;
    }
}
