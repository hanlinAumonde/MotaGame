package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.map.Position;

public abstract class Tile {
    private Position position;
    private boolean isPassable;
    private boolean isChangeable;
    private String bgResourceId;

    public Tile(Position position, boolean isPassable, boolean isChangeable, String bgResourceId) {
        this.position = position;
        this.isPassable = isPassable;
        this.isChangeable = isChangeable;
        this.bgResourceId = bgResourceId;
    }

    public abstract void interact();
}
