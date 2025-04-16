package com.demo.mota.engine.Item.GenericItem;

import com.demo.mota.engine.GameContext;

public class FloorJumper extends GenericItem{
    private short floorNumberSelected;

    public FloorJumper(String itemId, String itemName, String itemDescription,
                       long itemPrice, int itemCount,
                       boolean isStorable, boolean isConsumable,
                       short floorNumberSelected) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.floorNumberSelected = floorNumberSelected;
    }

    public void jumpToFloor(short floorNumber, GameContext context) {
        this.floorNumberSelected = floorNumber;
        applyEffect(context);
    }

    @Override
    public void applyEffect(GameContext context) {
        //TODO: Implement the logic to jump to the selected floor
    }
}
