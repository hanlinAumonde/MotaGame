package com.demo.mota.engine.Item;

public abstract class Item {
    private String itemId;
    private String itemName;
    private String itemDescription;

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

    public int getItemCount() {
        return itemCount;
    }

    public void updateItemCount(int count) {
        this.itemCount += count;
    }
}
