package com.demo.mota.engine.snapshot;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 撤销历史：最近若干次「有意义行动」之前的快照，后进先出，容量由塔规则 {@code undo.maxSteps} 决定。
 *
 * <p>只活在内存里、不写进存档；新开一局 / 读档时整份换掉，进入新区域时清空（区域单向，已卸载的区域不再回来）。
 * 满了之后丢弃最旧的一条。容量为 0 表示本塔不允许撤销，{@link #push} 什么也不做。
 */
public final class UndoHistory {

    /** 一个撤销点：行动之前的快照 + 给玩家看的行动描述（如「打开黄门」） */
    public record Entry(GameSnapshot snapshot, String label) {
    }

    private final int capacity;
    private final Deque<Entry> entries = new ArrayDeque<>();

    public UndoHistory(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public boolean isEnabled() {
        return capacity > 0;
    }

    public int capacity() {
        return capacity;
    }

    public void push(GameSnapshot snapshot, String label) {
        if (!isEnabled() || snapshot == null) return;
        if (entries.size() == capacity) {
            entries.removeLast();
        }
        entries.addFirst(new Entry(snapshot, label));
    }

    /** @return 最近的一个撤销点，没有时为 null */
    public Entry pop() {
        return entries.pollFirst();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }
}
