package com.demo.mota.ui.screen.game;

import com.demo.mota.engine.enums.Direction;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 行走动画期间的方向键缓冲。
 *
 * <p>一步的动画没播完时不执行下一步（否则画面落后于实际位置，撞上怪物时人还在半路），
 * 但按键不能丢：
 * <ul>
 *   <li>动画期间<b>新按下</b>的方向键记为 {@link #queue 待走的一步}，只记最后一个；</li>
 *   <li>同时记下哪些方向键<b>仍按着</b>（按下顺序，后按的优先）——一步走完时没有待走的一步，
 *       就沿仍按着的方向接着走，因此按住方向键是连续行走，不必等系统的按键重复延迟。</li>
 * </ul>
 * 系统的按键自动重复不算「新按下」（{@link #press} 返回 false）：连续行走已由「仍按着」负责，
 * 若把重复也记成待走的一步，松开方向键后人还会多走一格。
 * 不依赖 JavaFX，单元测试见 {@code StepBufferTest}。
 */
final class StepBuffer {

    private Direction pending;
    /** 仍按着的方向键，队首是最后按下的 */
    private final Deque<Direction> held = new ArrayDeque<>();

    /**
     * 方向键按下（含系统的自动重复）：记为按着。
     *
     * @return 是否是新按下的（此前没按着）；自动重复返回 false
     */
    boolean press(Direction direction) {
        boolean fresh = !held.remove(direction);
        held.push(direction);
        return fresh;
    }

    /** 方向键松开 */
    void release(Direction direction) {
        held.remove(direction);
    }

    /** 动画期间按下的方向键：记为待走的一步，覆盖之前没来得及走的那一步 */
    void queue(Direction direction) {
        pending = direction;
    }

    /**
     * 一步走完，取下一步：优先待走的一步，其次仍按着的方向；都没有返回 {@code null}。
     * 待走的一步取出即清空，按着的方向保留到松开为止。
     */
    Direction next() {
        Direction next = pending != null ? pending : held.peek();
        pending = null;
        return next;
    }

    /** 全部清空：切走界面、窗口失焦（松键事件可能收不到）、撤销等场合 */
    void clear() {
        pending = null;
        held.clear();
    }
}
