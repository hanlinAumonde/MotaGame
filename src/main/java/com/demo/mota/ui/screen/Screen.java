package com.demo.mota.ui.screen;

import javafx.scene.input.KeyCode;

/**
 * 一个界面：自己知道往哪块画布上画、自己消化属于自己的按键。
 *
 * <p>与 {@code GamePhase} 一一对应（{@code GAME_OVER} 例外——它是叠在游戏画面上的一层，
 * 见 {@code GameOverOverlay}）。{@code MotaController} 只按当前阶段挑一个 Screen 转发，
 * 不再关心具体画什么。
 *
 * <p>{@link #render()} 刻意不带画布参数：各界面需要的画布数量并不一致
 * （{@code GameScreen} 要同时画状态面板与地图两块），由构造时注入更干净。
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

    /** 切入本界面时调用，用于重置内部状态 */
    default void onEnter() {}
}
