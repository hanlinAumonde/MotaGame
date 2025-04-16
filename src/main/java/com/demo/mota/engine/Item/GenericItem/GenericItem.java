package com.demo.mota.engine.Item.GenericItem;

import com.demo.mota.engine.GameContext;
import com.demo.mota.engine.Item.Item;

public abstract class GenericItem extends Item {
    protected GenericItem(String itemId, String itemName, String itemDescription, long itemPrice, int itemCount, boolean isStorable, boolean isConsumable) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
    }

    abstract void applyEffect(GameContext gameContext);
}
