package com.demo.mota.engine.menu;

/**
 * 左侧菜单项。当前只有「敌物资料」一项，后续的物品栏、存档 / 读档、系统设置等
 * 直接在此追加枚举常量，菜单状态机与渲染均按枚举顺序自动展开。
 */
public enum MenuEntry {
    MONSTER_BOOK("敌物资料");

    private final String displayName;

    MenuEntry(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
