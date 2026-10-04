package com.demo.mota.ui.screen.gamemenu;

/**
 * 左侧菜单项。后续的存档 / 读档、系统设置等直接在此追加枚举常量，
 * 菜单状态机与渲染均按枚举顺序自动展开。
 */
public enum MenuEntry {
    /** 敌物资料：需持有怪物手册（辅助类道具）才能查看 */
    MONSTER_BOOK("敌物资料"),
    /** 游戏选项：弹出次级菜单（物品栏 / 角色装备 / 角色技能），见 {@link GameOption} */
    GAME_OPTIONS("游戏选项");

    private final String displayName;

    MenuEntry(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
