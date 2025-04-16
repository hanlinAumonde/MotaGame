package com.demo.mota.engine.enums;

public enum EquipSlot {
    NON_EQUIPPED(-1),
    SLOT1(0),
    SLOT2(1);

    private final int value;

    EquipSlot(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
