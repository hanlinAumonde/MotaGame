package com.demo.mota.engine.Item;

import com.demo.mota.engine.Item.GenericItem.GenericItem;

/**
 * 道具基类。
 *
 * <p><b>封闭类型</b>：按「道具大类」列出子类。拾取分派（{@code PlayerStateManager.gainItem}）
 * 与占位图绘制都按实际类型做穷尽 switch，新增一个大类会让这些分派点编译失败，
 * 不至于出现「捡起来却什么也没发生」这种静默漏处理。
 *
 * <p>{@link GenericItem} 保持 {@code non-sealed}：通用道具本身就是留给后续内容扩展的口子，
 * 每加一件都要回来改 permits 列表并不合理，而分派只需认到「这是个通用道具」这一层。
 */
public abstract sealed class Item
        permits Key, Equipment, Portion, AbilityGem, GenericItem {
    private final String itemId;
    private final String itemName;
    private final String itemDescription;

    private long itemPrice;
    private int itemCount;

    private boolean isStorable;
    private boolean isConsumable;

    protected Item(String itemId, String itemName, String itemDescription,
                   long itemPrice, int itemCount,
                   boolean isStorable, boolean isConsumable) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.itemPrice = itemPrice;
        this.itemCount = itemCount;
        this.isStorable = isStorable;
        this.isConsumable = isConsumable;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void updateItemCount(int count) {
        this.itemCount += count;
    }
}
