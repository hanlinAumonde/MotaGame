package com.demo.mota.engine.factory.item;

import com.demo.mota.engine.Item.Item;

import java.util.Map;

@FunctionalInterface
public interface ItemCreator {
    Item createItem(String itemId, String itemName, String itemDescription,
                    long itemPrice, int itemCount,
                    boolean isStorable, boolean isConsumable,
                    Map<String, Object> parameters);
}
