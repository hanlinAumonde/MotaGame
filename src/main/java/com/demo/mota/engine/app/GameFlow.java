package com.demo.mota.engine.app;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 游戏整体状态机：持有当前 {@link GamePhase}，按转移表校验迁移，并在迁移时通知监听者。
 *
 * <p>只认「阶段」这一件事，不认识画布、按键与引擎——UI 层订阅变更后自行切换界面。
 * 另有一个「新开一局」事件（{@link #startNewGame()}），供各界面重建本局的状态：
 * 回到 {@link GamePhase#PLAYING} 并不一定是新的一局（从装备界面返回也会进来），两件事必须分开。
 */
public class GameFlow {

    /** 允许的迁移：键为来源阶段，值为可去往的阶段 */
    private static final Map<GamePhase, Set<GamePhase>> TRANSITIONS = new EnumMap<>(GamePhase.class);

    static {
        TRANSITIONS.put(GamePhase.LOADING, EnumSet.of(GamePhase.TITLE));
        TRANSITIONS.put(GamePhase.TITLE, EnumSet.of(GamePhase.PLAYING));
        TRANSITIONS.put(GamePhase.PLAYING, EnumSet.of(GamePhase.GAME_MENU, GamePhase.EQUIPMENT,
                GamePhase.SKILL_SETUP, GamePhase.INVENTORY, GamePhase.EQUIPMENT_SETS, GamePhase.GAME_OVER));
        TRANSITIONS.put(GamePhase.GAME_MENU, EnumSet.of(GamePhase.PLAYING, GamePhase.INVENTORY,
                GamePhase.EQUIPMENT, GamePhase.SKILL_SETUP, GamePhase.EQUIPMENT_SETS));
        TRANSITIONS.put(GamePhase.INVENTORY, EnumSet.of(GamePhase.PLAYING));
        TRANSITIONS.put(GamePhase.EQUIPMENT, EnumSet.of(GamePhase.PLAYING));
        TRANSITIONS.put(GamePhase.SKILL_SETUP, EnumSet.of(GamePhase.PLAYING));
        TRANSITIONS.put(GamePhase.EQUIPMENT_SETS, EnumSet.of(GamePhase.PLAYING));
        TRANSITIONS.put(GamePhase.GAME_OVER, EnumSet.of(GamePhase.TITLE));
    }

    private GamePhase phase = GamePhase.LOADING;

    private final List<Consumer<GamePhase>> listeners = new ArrayList<>();
    private final List<Runnable> newGameListeners = new ArrayList<>();

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

    /** 订阅「新开一局」：在迁入 {@code PLAYING} 之前触发，监听者据此重建本局状态 */
    public void addNewGameListener(Runnable listener) {
        if (listener != null) {
            newGameListeners.add(listener);
        }
    }

    public boolean canTransitionTo(GamePhase target) {
        return TRANSITIONS.getOrDefault(phase, Set.of()).contains(target);
    }

    // ==================== 迁移 ====================

    /**
     * 迁移到目标阶段。目标就是当前阶段时什么也不做。
     *
     * @throws IllegalStateException 转移表不允许这次迁移（属于程序错误，早暴露早好）
     */
    public void to(GamePhase target) {
        if (phase == target) return;
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("非法的阶段迁移: " + phase + " -> " + target);
        }
        this.phase = target;
        // 复制一份再遍历：监听者在回调里再次订阅 / 触发迁移时不至于并发修改
        for (Consumer<GamePhase> listener : List.copyOf(listeners)) {
            listener.accept(target);
        }
    }

    /** 新的一局已装配完毕：先广播「新开一局」，再迁入 {@code PLAYING} */
    public void startNewGame() {
        for (Runnable listener : List.copyOf(newGameListeners)) {
            listener.run();
        }
        to(GamePhase.PLAYING);
    }
}
