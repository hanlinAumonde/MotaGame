package com.demo.mota.engine.item.generic;

import com.demo.mota.engine.session.GameSession;

public final class FloorJumper extends GenericItem{
    private int floorNumberSelected;

    public FloorJumper(String itemId, String itemName, String itemDescription,
                       long itemPrice, int itemCount,
                       boolean isStorable, boolean isConsumable,
                       int floorNumberSelected) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.floorNumberSelected = floorNumberSelected;
    }

    public void jumpToFloor(short floorNumber, GameSession session) {
        this.floorNumberSelected = floorNumber;
        applyEffect(session);
    }

    @Override
    public void applyEffect(GameSession session) {
        //TODO: Implement the logic to jump to the selected floor
    }
}
