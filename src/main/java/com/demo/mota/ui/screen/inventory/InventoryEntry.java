package com.demo.mota.ui.screen.inventory;

import com.demo.mota.engine.Item.Item;

/**
 * 物品栏里的一行：同一种道具合并为一行，{@code count} 为持有数量。
 *
 * @param usable 能否在物品栏里直接使用；为 false 的只能查看，选中时界面叠一层灰色遮罩
 */
public record InventoryEntry(Item item, int count, boolean usable) {
}
