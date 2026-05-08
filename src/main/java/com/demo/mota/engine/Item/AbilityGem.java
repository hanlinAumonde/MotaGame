package com.demo.mota.engine.Item;

import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.GameNumber;

public class AbilityGem extends Item {
    private final StateType effectedAbilityType;
    private final GameNumber effectValue;

    public AbilityGem(String itemId, String itemName, String itemDescription,
                      long itemPrice, int itemCount,
                      boolean isStorable, boolean isConsumable,
                      StateType effectedAbilityType, GameNumber effectValue) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.effectedAbilityType = effectedAbilityType;
        this.effectValue = effectValue;
    }

    public StateType getEffectedAbilityType() {
        return effectedAbilityType;
    }

    public GameNumber getEffectValue() {
        return effectValue;
    }
}
