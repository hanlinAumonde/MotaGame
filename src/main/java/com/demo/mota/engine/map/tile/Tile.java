package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.map.Position;

/**
 * 地图格子基类。
 *
 * <p><b>封闭类型</b>：子类在此显式列出。渲染与移动分发都按实际类型做穷尽 switch，
 * 封闭之后新增一种格子会让所有没跟着更新的分发点编译失败——这正是目的：
 * 漏改的分支在编译期暴露，而不是运行时悄悄走进兜底分支。
 */
public abstract sealed class Tile
        permits BackGroundTile, WallTile, DoorTile, FloorSwitcherTile, TrickyTile {
    private final Position position;
    private boolean isPassable;
    private final boolean isChangeable;
    private final String bgResourceId;

    public Tile(Position position, boolean isPassable, boolean isChangeable, String bgResourceId) {
        this.position = position;
        this.isPassable = isPassable;
        this.isChangeable = isChangeable;
        this.bgResourceId = bgResourceId;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isPassable() {
        return isPassable;
    }

    public void setPassable(boolean passable) {
        this.isPassable = passable;
    }

    public boolean isChangeable() {
        return isChangeable;
    }

    public String getBgResourceId() {
        return bgResourceId;
    }
}
