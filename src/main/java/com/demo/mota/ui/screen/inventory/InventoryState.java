package com.demo.mota.ui.screen.inventory;

import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.MenuCommand;

import java.util.List;

/**
 * 物品栏的状态机（从游戏菜单「游戏选项 → 物品栏」进入）。与装备 / 技能界面同级，只管状态、不管绘制。
 *
 * <p>焦点在两处之间切换：
 * <ul>
 *   <li><b>分类</b>（左列）：↑↓ 选择，右侧随之预览该分类的道具；确认 = 焦点移到道具列表（分类为空时不动）；
 *       返回 = 关闭界面回到游戏。</li>
 *   <li><b>道具列表</b>：↑↓ 选择；确认 = 使用（目前没有可使用的道具，只能查看的给出提示）；返回 = 回到分类。</li>
 * </ul>
 * 条目每次现取：界面打开期间玩家状态不会变化，无需快照。
 */
public class InventoryState {

    public enum Focus { CATEGORIES, ITEMS }

    private final PlayerStateManager player;

    private boolean open;
    private Focus focus = Focus.CATEGORIES;
    private int categoryIndex;
    private int itemIndex;
    /** 最近一次操作的提示，下一次操作即清除 */
    private String notice;

    public InventoryState(PlayerStateManager player) {
        this.player = player;
    }

    public void open() {
        this.open = true;
        this.focus = Focus.CATEGORIES;
        this.categoryIndex = 0;
        this.itemIndex = 0;
        this.notice = null;
    }

    public void close() {
        this.open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void handle(MenuCommand command) {
        if (!open || command == null) {
            return;
        }
        notice = null;
        switch (command) {
            case BACK -> {
                if (focus == Focus.ITEMS) {
                    focus = Focus.CATEGORIES;
                } else {
                    close();
                }
            }
            case CLOSE -> close();
            case UP -> move(-1);
            case DOWN -> move(1);
            case CONFIRM -> confirm();
            default -> { /* 本界面不响应左右与清除 */ }
        }
    }

    private void confirm() {
        if (focus == Focus.CATEGORIES) {
            if (!getEntries().isEmpty()) {
                focus = Focus.ITEMS;
                itemIndex = 0;
            }
            return;
        }
        // 使用逻辑待可使用的道具（通用道具 isUsable）实现后接入，目前所有道具都只能查看
        InventoryEntry entry = getSelectedEntry();
        if (entry != null && !entry.usable()) {
            notice = "该物品只能查看，无法使用";
        }
    }

    private void move(int delta) {
        if (focus == Focus.CATEGORIES) {
            categoryIndex = wrap(categoryIndex + delta, InventoryCategory.values().length);
            itemIndex = 0;
        } else {
            itemIndex = wrap(itemIndex + delta, getEntries().size());
        }
    }

    private static int wrap(int index, int size) {
        if (size <= 0) return 0;
        return ((index % size) + size) % size;
    }

    // ==================== 状态查询（供渲染层读取） ====================

    public Focus getFocus() {
        return focus;
    }

    public String getNotice() {
        return notice;
    }

    public List<InventoryCategory> getCategories() {
        return List.of(InventoryCategory.values());
    }

    public int getSelectedCategoryIndex() {
        return categoryIndex;
    }

    public InventoryCategory getSelectedCategory() {
        return InventoryCategory.values()[categoryIndex];
    }

    /** 当前分类下的道具 */
    public List<InventoryEntry> getEntries() {
        return getSelectedCategory().collect(player);
    }

    public int getSelectedItemIndex() {
        return itemIndex;
    }

    public InventoryEntry getSelectedEntry() {
        List<InventoryEntry> entries = getEntries();
        if (itemIndex < 0 || itemIndex >= entries.size()) {
            return null;
        }
        return entries.get(itemIndex);
    }
}
