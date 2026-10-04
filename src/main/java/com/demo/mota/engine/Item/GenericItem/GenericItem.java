package com.demo.mota.engine.Item.GenericItem;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.Item.Item;

public abstract non-sealed class GenericItem extends Item {
    protected GenericItem(String itemId, String itemName, String itemDescription, long itemPrice, int itemCount, boolean isStorable, boolean isConsumable) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
    }

    abstract void applyEffect(GameEngine gameContext);

    /**
     * 能否在物品栏里直接使用。默认不能（只能查看），实现了使用逻辑的道具覆写为 true；
     * 物品栏据此决定选中时是否叠一层灰色「仅可查看」遮罩。
     */
    public boolean isUsable() {
        return false;
    }
}
