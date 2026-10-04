package com.demo.mota.ui.screen;

import javafx.scene.input.KeyCode;

/**
 * 一个界面：自己知道往哪块画布上画、自己消化属于自己的按键。
 *
 * <p>与 {@code GamePhase} 一一对应。{@code MotaController} 只按当前阶段挑一个 Screen 转发按键，
 * 切换阶段时统一调 {@link #onEnter()} 与 {@link #render()}，不关心具体画什么。
 *
 * <p>{@link #render()} 刻意不带画布参数：各界面需要的画布数量并不一致
 * （{@code GameScreen} 要同时画状态面板、地图与右侧栏三块），由构造时注入更干净。
 */
public interface Screen {

    /** 重绘本界面 */
    void render();

    /**
     * 处理一次按键。
     *
     * @return 是否消费了该按键；返回 false 表示这个键与本界面无关
     */
    boolean handleKey(KeyCode code);

    /** 每次切入本界面时调用，用于重置光标等界面内状态 */
    default void onEnter() {}

    /**
     * 新开一局时调用（早于迁入 {@code PLAYING}），用于按本局的玩家 / 地图重建界面状态。
     * 与 {@link #onEnter()} 分开：从装备界面回到游戏也会进入对局界面，但那不是新的一局。
     */
    default void onNewGame() {}

    /**
     * 本界面是否叠在游戏画面之上（背后露出地图，如游戏结束提示框）。
     * 为 true 时 Controller 先画对局界面再画本界面；为 false 的界面铺满整屏。
     */
    default boolean overlaysGame() {
        return false;
    }
}
