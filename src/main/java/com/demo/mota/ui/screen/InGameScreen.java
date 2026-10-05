package com.demo.mota.ui.screen;

import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

/**
 * 对局内、铺满整屏的界面（游戏菜单 / 物品栏 / 装备 / 技能设置）的公共基类。
 *
 * <p><b>统一用 {@link #CLOSE_KEY}（X）返回游戏</b>：在这里拦下，子类不必、也不应再处理这个键。
 * 其余按键交给 {@link #handleScreenKey}，它返回要去的阶段（{@code null} = 留在本界面并重绘）。
 */
public abstract class InGameScreen implements Screen {

    /** 所有对局内界面统一的「返回游戏」键 */
    public static final KeyCode CLOSE_KEY = KeyCode.X;

    protected final GameFlow flow;
    private final Canvas canvas;

    protected InGameScreen(GameFlow flow, Canvas canvas) {
        this.flow = flow;
        this.canvas = canvas;
    }

    @Override
    public final void handleKey(KeyCode code) {
        if (code == CLOSE_KEY) {
            flow.to(GamePhase.PLAYING);
            return;
        }
        GamePhase next = handleScreenKey(code);
        if (next != null) {
            flow.to(next);
        } else {
            render();
        }
    }

    /**
     * 处理除 X 以外的按键。
     *
     * @return 处理完要迁往的阶段；{@code null} 表示留在本界面（随后自动重绘）
     */
    protected abstract GamePhase handleScreenKey(KeyCode code);

    @Override
    public final void handleKeyRelease(KeyCode code) {
        if (handleScreenKeyRelease(code)) {
            render();
        }
    }

    /**
     * 处理松键（组合快捷键用）。
     *
     * @return 界面是否有变化需要重绘
     */
    protected boolean handleScreenKeyRelease(KeyCode code) {
        return false;
    }

    @Override
    public final void render() {
        render(canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight());
    }

    protected abstract void render(GraphicsContext gc, double width, double height);
}
