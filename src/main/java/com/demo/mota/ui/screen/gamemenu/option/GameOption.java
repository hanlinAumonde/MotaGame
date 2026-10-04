package com.demo.mota.ui.screen.gamemenu.option;

import com.demo.mota.engine.app.GamePhase;

/**
 * 「游戏选项」的次级菜单项（左侧选中「游戏选项」后弹出的小窗）。
 *
 * <p>每一项都对应一个独立界面：选中后迁往 {@link #getHandoff()} 指定的阶段，
 * 之后在那个界面里按 X 直接返回游戏。新增一项 = 追加常量 + 在 {@link GameOptionRenderer} 补一句说明。
 */
public enum GameOption {
    ITEMS("物品栏", GamePhase.INVENTORY),
    EQUIPMENT("角色装备", GamePhase.EQUIPMENT),
    SKILLS("角色技能", GamePhase.SKILL_SETUP);

    private final String displayName;
    private final GamePhase handoff;

    GameOption(String displayName, GamePhase handoff) {
        this.displayName = displayName;
        this.handoff = handoff;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 选中后要迁往的阶段 */
    public GamePhase getHandoff() {
        return handoff;
    }
}
