package com.demo.mota.engine.Item;

/**
 * 辅助类道具解锁的功能。配置表里写 {@link #getValue()} 的字符串（{@code parameters.auxiliaryType}）。
 */
public enum AuxiliaryType {
    /** 怪物手册：解锁游戏菜单里的「敌物资料」 */
    MONSTER_BOOK("monsterBook");

    private final String value;

    AuxiliaryType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AuxiliaryType fromString(String value) {
        for (AuxiliaryType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown auxiliary type: " + value);
    }
}
