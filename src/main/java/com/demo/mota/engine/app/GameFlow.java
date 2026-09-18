package com.demo.mota.engine.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 游戏生命周期状态机：持有当前 {@link GamePhase}，并在迁移时通知监听者。
 *
 * <p>只认「阶段」这一件事，不认识画布、按键与引擎——UI 层订阅变更后自行切换界面，
 * 引擎层需要时可以主动查询。迁移方法按语义命名（而不是暴露一个裸的 setter），
 * 让非法迁移在编译期就少一半可能。
 */
public class GameFlow {

    private GamePhase phase = GamePhase.LOADING;

    private final List<Consumer<GamePhase>> listeners = new ArrayList<>();

    public GamePhase getPhase() {
        return phase;
    }

    public boolean is(GamePhase expected) {
        return phase == expected;
    }

    /** 订阅阶段变更；回调在触发迁移的那个线程上执行 */
    public void addListener(Consumer<GamePhase> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    // ==================== 迁移 ====================

    /** 加载完成 → 标题界面 */
    public void toTitle() {
        transitionTo(GamePhase.TITLE);
    }

    /** 开始 / 继续一局 → 对局中 */
    public void toPlaying() {
        transitionTo(GamePhase.PLAYING);
    }

    /** 生命归零 → 游戏结束 */
    public void toGameOver() {
        transitionTo(GamePhase.GAME_OVER);
    }

    private void transitionTo(GamePhase target) {
        if (phase == target) return;
        this.phase = target;
        // 复制一份再遍历：监听者在回调里再次订阅 / 触发迁移时不至于并发修改
        for (Consumer<GamePhase> listener : List.copyOf(listeners)) {
            listener.accept(target);
        }
    }
}
