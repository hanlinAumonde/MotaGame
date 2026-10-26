package com.demo.mota.scene;

import com.almasb.fxgl.scene.SubScene;
import com.demo.mota.ui.kit.Sketch;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * 菜单类子场景的公共基类：物品栏、装备、技能设置、套装、存读档、游戏结束横幅。
 *
 * <p><b>显示</b>：内容是一块 {@link Sketch}（按坐标摆放的节点），{@link #render()} 按当前状态重新摆一遍。
 * 节点在窗口缩放时按矢量清晰缩放，面板 / 选中框的样式来自 {@code mota.css}。
 *
 * <p><b>输入</b>：在本场景自己的 {@code Input} 上挂按键处理器，FXGL 只把按键交给栈顶的场景（源码：{@code MainWindow}），
 * 路由不必自己做。菜单不用 {@code UserAction}：在长列表里按住 ↓ 连续滚动依赖系统的按键自动重复，而动作会把它吃掉。
 *
 * <p><b>生命周期</b>：FXGL 每次 push 都调 {@link #onCreate()}（不是只调一次），这里交给 {@link #onOpen()}
 * 重置光标、按当前这一局重建状态，然后摆一遍；之后每处理一次按键重摆一次。
 */
public abstract class MenuSubScene extends SubScene {

    private final Sketch sketch = new Sketch();
    /** 叠上来的那个按键事件派发完之前为 false，见 {@link #onCreate()} */
    private boolean acceptingKeys;

    protected MenuSubScene() {
        getContentRoot().getChildren().add(sketch.node());
        getInput().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (acceptingKeys) onKey(e.getCode());
        });
        getInput().addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            if (acceptingKeys) onKeyRelease(e.getCode());
        });
    }

    /**
     * 每次叠上来时调用。<b>叠上本场景的那个按键不能再交给本场景</b>：FXGL 先把按键交给各活动场景的动作
     * （对局里按 Q、或走一步打输了），动作里叠上了本场景；随后同一个事件的「场景处理器」阶段会按<b>新的</b>
     * 活动场景再派发一遍（源码：{@code MainWindow} 两次 {@code runOnActiveStates}），本场景就会收到那个键——
     * 例如致死那一步的 ↓ 会把结束横幅的光标挪走。所以等这个事件派发完（下一次 FX 脉冲）才开始接收按键。
     */
    @Override
    public final void onCreate() {
        acceptingKeys = false;
        Platform.runLater(() -> acceptingKeys = true);
        onOpen();
        render();
    }

    /** 每次叠上来时调用：重置光标、按当前这一局重建状态 */
    protected abstract void onOpen();

    /** 处理一次按下（含系统的自动重复） */
    protected abstract void onKey(KeyCode code);

    /** 处理一次松开；只有用到组合快捷键的界面才需要关心 */
    protected void onKeyRelease(KeyCode code) {
    }

    /** 按当前状态重新摆一遍 */
    public final void render() {
        sketch.clear();
        draw(sketch);
    }

    protected abstract void draw(Sketch sketch);
}
