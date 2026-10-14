package com.demo.mota.engine.event;

public class MoveResult {
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

    private final Type type;
    private final String detail;

    private MoveResult(Type type, String detail) {
        this.type = type;
        this.detail = detail;
    }

    public static MoveResult of(Type type) {
        return new MoveResult(type, null);
    }

    public static MoveResult of(Type type, String detail) {
        return new MoveResult(type, detail);
    }

    public Type getType() {
        return type;
    }

    public String getDetail() {
        return detail;
    }
}
