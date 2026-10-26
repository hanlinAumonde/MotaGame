package com.demo.mota.scene;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.scene.Scene;
import com.demo.mota.app.nav.Navigator;
import javafx.scene.input.KeyCode;

/**
 * 对局内、铺满整屏的子场景（物品栏 / 装备 / 技能设置 / 套装）的公共基类。
 *
 * <ul>
 *   <li><b>统一用 {@link #CLOSE_KEY}（X）直接回到对局</b>：在这里拦下，子类不必、也不应再处理这个键；</li>
 *   <li>其余按键交给 {@link #handleScreenKey}，它返回 true 表示本界面关闭（Esc 退到底、或选定后自动关闭），弹回下一层；</li>
 *   <li><b>回到对局时通知对局界面</b>（{@code onReturnToGame}）：这里穿脱装备、改预设都可能影响战斗推演，
 *       由对局统一重算当前层伤害，各界面就不必各自汇报「改没改」。游戏场景是 FXGL 的 final 类，
 *       拿不到它自己的 onEnteredFrom，所以由离开的这一方在 {@link #onExitingTo} 里发出。</li>
 * </ul>
 */
public abstract class InGameSubScene extends MenuSubScene {

    /** 所有对局内界面统一的「返回游戏」键（也是 FXGL 的菜单键：在对局里按它打开游戏菜单） */
    public static final KeyCode CLOSE_KEY = KeyCode.X;

    protected final Navigator navigator;
    private final Runnable onReturnToGame;

    protected InGameSubScene(Navigator navigator, Runnable onReturnToGame) {
        this.navigator = navigator;
        this.onReturnToGame = onReturnToGame;
    }

    @Override
    protected final void onKey(KeyCode code) {
        if (code == CLOSE_KEY) {
            navigator.backToGame();
            return;
        }
        if (handleScreenKey(code)) {
            navigator.pop();
        } else {
            render();
        }
    }

    /**
     * 处理除 X 以外的按键。
     *
     * @return 本界面是否就此关闭（随后弹回下一层）；false 表示留在本界面（随后自动重画）
     */
    protected abstract boolean handleScreenKey(KeyCode code);

    @Override
    protected final void onKeyRelease(KeyCode code) {
        if (handleScreenKeyRelease(code)) {
            render();
        }
    }

    /**
     * 处理松键（组合快捷键用）。
     *
     * @return 界面是否有变化需要重画
     */
    protected boolean handleScreenKeyRelease(KeyCode code) {
        return false;
    }

    @Override
    public void onExitingTo(Scene next) {
        if (next == FXGL.getGameScene()) {
            onReturnToGame.run();
        }
    }
}
