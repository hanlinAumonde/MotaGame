package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.map.Position;

public abstract class Tile {
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
