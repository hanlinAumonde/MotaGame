package com.demo.mota.ui.screen.gamemenu.option;

/**
 * 「游戏选项」的次级菜单项（左侧选中「游戏选项」后弹出的小窗）。
 *
 * <p>每一项都对应一个独立界面：选中后游戏菜单换成那个界面（对应关系在组装处 {@code MotaScenes}），
 * 之后在那个界面里按 X 直接返回游戏。新增一项 = 追加常量 + 在 {@link GameOptionRenderer} 补一句说明 + 组装处补一个界面。
 */
public enum GameOption {
    ITEMS("物品栏"),
    EQUIPMENT("角色装备"),
    SKILLS("角色技能"),
    EQUIPMENT_SETS("套装设定");

    private final String displayName;

    GameOption(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
