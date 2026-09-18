package com.demo.mota.engine.app;

/**
 * 游戏整体所处的阶段。
 *
 * <pre>
 *   LOADING ──加载完成──▶ TITLE ──开始游戏──▶ PLAYING ──生命归零──▶ GAME_OVER
 *                          ▲                                        │
 *                          └────────────── 返回标题 ─────────────────┘
 * </pre>
 *
 * <p>这是<b>最外层</b>的状态：游戏内菜单（{@code GameMenu}）属于 {@link #PLAYING}
 * 内部的子状态，不并到这一层来——否则「菜单开着」会和「游戏结束」这类互斥语义混在一起。
 */
public enum GamePhase {
    /** 资源与配置表加载中，显示进度 */
    LOADING,
    /** 标题界面：开始游戏 / 继续游戏 / 退出游戏 */
    TITLE,
    /** 对局进行中 */
    PLAYING,
    /** 玩家生命归零，游戏画面保留，底部弹出提示框 */
    GAME_OVER
}
