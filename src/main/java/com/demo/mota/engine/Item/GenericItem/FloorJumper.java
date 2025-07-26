package com.demo.mota.engine.Item.GenericItem;

import com.demo.mota.engine.GameEngine;

public class FloorJumper extends GenericItem{
    private short floorNumberSelected;

    public FloorJumper(String itemId, String itemName, String itemDescription,
                       long itemPrice, int itemCount,
                       boolean isStorable, boolean isConsumable,
                       short floorNumberSelected) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.floorNumberSelected = floorNumberSelected;
    }

    public void jumpToFloor(short floorNumber, GameEngine context) {
        this.floorNumberSelected = floorNumber;
        applyEffect(context);
    }

    @Override
    public void applyEffect(GameEngine context) {
        //TODO: Implement the logic to jump to the selected floor
    }
}
