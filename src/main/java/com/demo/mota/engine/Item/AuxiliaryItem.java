package com.demo.mota.engine.Item;

/**
 * 辅助类道具：拿到手里即可解锁某项功能，本身不产生数值效果，也不会被消耗。
 *
 * <p>具体解锁什么由 {@link AuxiliaryType} 决定；目前只有怪物手册——
 * 拾取后游戏菜单（X）里的「敌物资料」才可查看。
 */
public final class AuxiliaryItem extends Item {
    private final AuxiliaryType auxiliaryType;

    public AuxiliaryItem(String itemId, String itemName, String itemDescription,
                         long itemPrice, int itemCount,
                         boolean isStorable, boolean isConsumable,
                         AuxiliaryType auxiliaryType) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.auxiliaryType = auxiliaryType;
    }

    public AuxiliaryType getAuxiliaryType() {
        return auxiliaryType;
    }
}
