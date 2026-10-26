package com.demo.mota.scene.game;

import com.demo.mota.engine.enums.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * 行走动画期间的方向键缓冲。
 *
 * <p>一步的动画没播完时不执行下一步（否则画面落后于实际位置，撞上怪物时人还在半路），
 * 但按键不能丢：
 * <ul>
 *   <li>动画期间按下的方向键记为 {@link #queue 待走的一步}，只记最后一个；</li>
 *   <li>同时记下哪些方向键<b>仍按着</b>（按下顺序，后按的优先）——一步走完时没有待走的一步，
 *       就沿仍按着的方向接着走，因此按住方向键是连续行走；</li>
 *   <li>记下每个方向键按下的时刻：没有走动的行动（撞墙、开门、打怪）之后没有「走完一步」来接续，
 *       由调用方按 {@link #repeatDue} 判断按住够久了再试一次。</li>
 * </ul>
 * 按下事件来自 FXGL 的动作（{@code onActionBegin}），只在真正按下时触发一次，系统的按键自动重复不会再进来。
 * 不依赖 JavaFX，单元测试见 {@code StepBufferTest}。
 */
final class StepBuffer {

    private Direction pending;
    /** 仍按着的方向键，队首是最后按下的 */
    private final Deque<Direction> held = new ArrayDeque<>();
    /** 仍按着的方向键各自按下的时刻（秒，调用方的时钟） */
    private final Map<Direction, Double> pressedAt = new EnumMap<>(Direction.class);

    /** 方向键按下：记为按着（{@code now} 为调用方时钟的当前秒数） */
    void press(Direction direction, double now) {
        held.remove(direction);
        held.push(direction);
        pressedAt.put(direction, now);
    }

    /** 方向键松开 */
    void release(Direction direction) {
        held.remove(direction);
        pressedAt.remove(direction);
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

    /**
     * 最后按下、仍按着的方向是否已按住至少 {@code delay} 秒。
     *
     * @return 是则返回该方向，否则 {@code null}
     */
    Direction repeatDue(double now, double delay) {
        Direction latest = held.peek();
        if (latest == null || now - pressedAt.get(latest) < delay) {
            return null;
        }
        return latest;
    }

    /** 全部清空：离开对局、窗口失焦（松键事件可能收不到）、撤销等场合 */
    void clear() {
        pending = null;
        held.clear();
        pressedAt.clear();
    }
}
