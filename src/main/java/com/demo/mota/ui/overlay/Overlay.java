package com.demo.mota.ui.overlay;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

/**
 * 对局中盖在游戏画面上的一层界面（敌物资料 / 技能设置 / 装备……），画在整屏的菜单画布上。
 *
 * <p>{@code GameScreen} 同一时刻只持有一个打开的覆盖层，打开期间按键全部交给它。
 * 每个覆盖层内部仍是「状态在 engine、绘制在 ui」：这里只负责按键 → 语义的翻译与转发。
 *
 * <p><b>返回游戏统一用 {@link #CLOSE_KEY}（X）</b>，由 {@code GameScreen} 在转发按键之前拦下并调用 {@link #close()}，
 * 各覆盖层不必、也不应再自己处理这个键；新增覆盖层自动遵守同一约定。
 */
public interface Overlay {

    /** 所有游戏内界面统一的「返回游戏」键 */
    KeyCode CLOSE_KEY = KeyCode.X;

    /** 呼出本覆盖层的按键 */
    KeyCode toggleKey();

    boolean isOpen();

    void open();

    void close();

    /**
     * 处理一次按键。
     *
     * @return 是否改动了会影响战斗推演的状态（穿脱装备、改预设……），调用方据此重算伤害
     */
    boolean handleKey(KeyCode code);

    void render(GraphicsContext gc, double width, double height);
}
