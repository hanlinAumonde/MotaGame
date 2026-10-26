package com.demo.mota.app.nav;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.scene.SceneService;
import com.almasb.fxgl.scene.SubScene;

/**
 * 界面导航：在 FXGL 的场景栈上叠 / 弹子场景。栈本身表达了「从哪来、回哪去」，原来的阶段转移表不再需要。
 *
 * <pre>
 *   对局                      [Game]
 *   按 X                      [Game, GameMenu]                 FXGL 的菜单键直接处理
 *   游戏选项 → 物品栏          [Game, Inventory]                {@link #replaceTop}：在物品栏按 X / Esc 都回到对局
 *   游戏菜单 → 读档            [Game, GameMenu, SaveLoad]       Esc 回游戏菜单；X 回对局
 *   死亡 → 读取存档            [Game, GameOver, SaveLoad]
 *   标题 → 继续游戏            [主菜单底座, Title, SaveLoad]
 * </pre>
 *
 * <p>只有栈顶场景接收输入与更新（源码：{@code StateMachine}），下层照常显示。
 */
public final class Navigator {

    private final Runnable beforeLeavingGame;

    /**
     * @param beforeLeavingGame 从对局叠上子场景之前调用：清掉 D / W 的按住状态。FXGL 在 push 时会对对局的输入
     *                          {@code clearAll()} 并给仍按着的动作补发 {@code onActionEnd}，不先清掉的话，
     *                          「按住 D 时被游戏逻辑叠上了子场景」（如走上楼梯后死亡）会被当成单按 D 而弹出技能设置
     */
    public Navigator(Runnable beforeLeavingGame) {
        this.beforeLeavingGame = beforeLeavingGame;
    }

    private static SceneService scenes() {
        return FXGL.getSceneService();
    }

    public void push(SubScene scene) {
        if (scenes().getCurrentScene() == FXGL.getGameScene()) {
            beforeLeavingGame.run();
        }
        scenes().pushSubScene(scene);
    }

    public void pop() {
        scenes().popSubScene();
    }

    /** 弹掉栈顶、叠上另一个（游戏菜单 → 物品栏等：在后者里返回直接回到对局，而不是回到菜单） */
    public void replaceTop(SubScene scene) {
        pop();
        push(scene);
    }

    /**
     * 弹到对局为止。不用 {@code gotoPlay()}：父场景已经是游戏场景时，它会把游戏场景当作「新的父场景」再切换一遍，
     * 依次触发它的 onExitingTo / onEnteredFrom / onDestroy（源码：MainWindow.setScene）。
     */
    public void backToGame() {
        if (!scenes().isInHierarchy(FXGL.getGameScene())) {
            return;
        }
        while (scenes().getCurrentScene() != FXGL.getGameScene()) {
            pop();
        }
    }

    /** 当前是否停在对局上（没有任何子场景叠着） */
    public boolean isInGame() {
        return scenes().getCurrentScene() == FXGL.getGameScene();
    }
}
