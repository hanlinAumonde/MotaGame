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
