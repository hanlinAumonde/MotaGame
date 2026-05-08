package com.demo.mota.engine.Item;

import com.demo.mota.engine.enums.EquipSlot;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.GameNumber;

import java.util.Map;

public class Equipment extends Item {
    private final Map<StateType, GameNumber> stateEffectMap;
    private EquipSlot slot;

    public Equipment(String itemId, String itemName, String itemDescription,
                     long itemPrice, int itemCount,
                     boolean isStorable, boolean isConsumable,
                     Map<StateType, GameNumber> stateMap) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.stateEffectMap = stateMap;
        this.slot = EquipSlot.NON_EQUIPPED;
    }

    public Map<StateType, GameNumber> getStateEffectMap() {
        return stateEffectMap;
    }

    public EquipSlot getSlot() {
        return slot;
    }

    public void setSlot(EquipSlot slot) {
        this.slot = slot;
    }
}
