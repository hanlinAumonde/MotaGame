package com.demo.mota.engine.app;

/**
 * 游戏整体所处的阶段，每个阶段对应一个界面（{@code ui.screen} 下的一个 Screen）。
 *
 * <pre>
 *   LOADING ──▶ TITLE ──开始游戏──▶ PLAYING ──生命归零──▶ GAME_OVER ──返回标题──▶ TITLE
 *                                   │  ▲
 *               X / D / Q ──────────┘  └────────── X / Esc 返回
 *                 ▼
 *   GAME_MENU ──游戏选项──▶ INVENTORY / EQUIPMENT / SKILL_SETUP
 * </pre>
 *
 * 允许的迁移见 {@link GameFlow}。各阶段两两互斥：同一时刻只有一个界面在接收按键。
 */
public enum GamePhase {
    /** 资源与配置表加载中，显示进度 */
    LOADING(false),
    /** 标题界面：开始游戏 / 继续游戏 / 退出游戏 */
    TITLE(false),
    /** 对局进行中：地图 + 状态栏 + 右侧栏 */
    PLAYING(true),
    /** 游戏菜单（X）：敌物资料 / 游戏选项 */
    GAME_MENU(true),
    /** 物品栏（经游戏选项进入） */
    INVENTORY(true),
    /** 装备界面（Q） */
    EQUIPMENT(true),
    /** 技能设置（D） */
    SKILL_SETUP(true),
    /** 玩家生命归零，游戏画面保留，底部弹出提示框 */
    GAME_OVER(true);

    private final boolean inGame;

    GamePhase(boolean inGame) {
        this.inGame = inGame;
    }

    /** 是否处于一局游戏之中（对局本身、对局内的各界面、以及结束提示） */
    public boolean isInGame() {
        return inGame;
    }
}
