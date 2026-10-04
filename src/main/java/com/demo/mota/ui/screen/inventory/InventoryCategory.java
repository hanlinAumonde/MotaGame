package com.demo.mota.ui.screen.inventory;

import com.demo.mota.engine.Item.GenericItem.GenericItem;
import com.demo.mota.engine.Item.Key;
import com.demo.mota.engine.state.PlayerStateManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 物品栏的分类（菜单左侧一列）。每个分类自己决定从玩家身上取哪些道具，
 * 新增一类 = 追加一个枚举常量并实现 {@link #collect}，状态机与渲染按枚举顺序自动展开。
 *
 * <p>剧情类暂不考虑；装备已独立为 Q 界面，不在此列。
 */
public enum InventoryCategory {
    /** 当前钥匙组里持有数量大于 0 的钥匙 */
    KEY("钥匙类") {
        @Override
        public List<InventoryEntry> collect(PlayerStateManager player) {
            List<InventoryEntry> entries = new ArrayList<>();
            for (Key key : player.getCurrentKeys()) {
                if (key.getItemCount() > 0) {
                    // 钥匙在碰到门时自动消耗，不能在物品栏里「使用」
                    entries.add(new InventoryEntry(key, key.getItemCount(), false));
                }
            }
            return entries;
        }
    },
    /** 辅助类：拿到即解锁某项功能（怪物手册……），只能查看 */
    AUXILIARY("辅助类") {
        @Override
        public List<InventoryEntry> collect(PlayerStateManager player) {
            return player.getAuxiliaryItemsOwned().stream()
                    .map(item -> new InventoryEntry(item, 1, false))
                    .toList();
        }
    },
    /** 通用类（{@link GenericItem}）：同 id 的合并为一行 */
    GENERIC("通用类") {
        @Override
        public List<InventoryEntry> collect(PlayerStateManager player) {
            Map<String, List<GenericItem>> grouped = new LinkedHashMap<>();
            for (GenericItem item : player.getGenericItemsOwned()) {
                grouped.computeIfAbsent(item.getItemId(), _ -> new ArrayList<>()).add(item);
            }
            return grouped.values().stream()
                    .map(items -> new InventoryEntry(items.getFirst(), items.size(), items.getFirst().isUsable()))
                    .toList();
        }
    };

    private final String displayName;

    InventoryCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 从玩家身上取出本分类下的全部道具，按获得顺序 */
    public abstract List<InventoryEntry> collect(PlayerStateManager player);
}
