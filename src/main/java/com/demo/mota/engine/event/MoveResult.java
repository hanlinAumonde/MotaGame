package com.demo.mota.engine.event;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家行动一步的结果。
 *
 * @param type   结果类型（状态栏文字、撤销点的依据）
 * @param detail 给状态栏的补充说明（怪物名、道具名、「第N层」……），没有时为 null
 * @param events 这一步按顺序发生的领域事件，见 {@link GameEvent}
 */
public record MoveResult(Type type, String detail, List<GameEvent> events) {

    public enum Type {
        MOVED,
        ITEM_PICKED,
        BATTLE_WON,
        BATTLE_LOST,
        DOOR_OPENED,
        DARK_WALL_REVEALED,
        FLOOR_SWITCHED,
        /** 区域检测格检查通过，已开启 */
        REGION_GATE_OPENED,
        /** 区域检测格检查未通过，detail 为剩余数量 */
        REGION_GATE_LOCKED,
        /** 通往下一区域的楼梯：本区域还有未开启的检测格 */
        REGION_EXIT_LOCKED,
        /** 通往已通过区域的楼梯：区域单向推进，不可返回 */
        REGION_BACKWARD_BLOCKED,
        /** 进入了新区域，detail 为「区域名 · 第N层」 */
        REGION_ENTERED,
        BLOCKED,
        NO_MOVE
    }

    public MoveResult {
        events = List.copyOf(events);
    }

    /** 追加事件（结果本身不可变，返回新对象） */
    public MoveResult withEvent(GameEvent event) {
        List<GameEvent> all = new ArrayList<>(events);
        all.add(event);
        return new MoveResult(type, detail, all);
    }
}
