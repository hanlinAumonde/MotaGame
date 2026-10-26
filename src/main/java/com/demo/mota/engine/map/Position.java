package com.demo.mota.engine.map;

import com.demo.mota.engine.enums.Direction;

/**
 * 地图上的一格（不可变）：{@code x} 为列、{@code y} 为行，左上角为 (0, 0)。
 * 可以直接当作 {@code Map} 的键，拿到手的位置不会被别处悄悄改掉。
 */
public record Position(int x, int y) {

    /** 是否落在地图范围内 */
    public boolean isInside() {
        return x >= 0 && y >= 0 && x < GameMap.SIDE && y < GameMap.SIDE;
    }

    /** 朝某方向相邻的一格；走出地图时为 {@code null} */
    public Position adjacent(Direction direction) {
        Position next = switch (direction) {
            case UP -> new Position(x, y - 1);
            case DOWN -> new Position(x, y + 1);
            case LEFT -> new Position(x - 1, y);
            case RIGHT -> new Position(x + 1, y);
        };
        return next.isInside() ? next : null;
    }

    /** 是否与另一格上下左右相邻 */
    public boolean isAdjacentTo(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y) == 1;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
