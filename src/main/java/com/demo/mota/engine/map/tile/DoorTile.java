package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.map.Position;

public class DoorTile extends Tile {
    private final KeyColor keyColor;
    private final String doorResourceId;

    public DoorTile(Position position, String bgResourceId, KeyColor keyColor, String doorResourceId) {
        super(position, false, true, bgResourceId);
        this.keyColor = keyColor;
        this.doorResourceId = doorResourceId;
    }

    public KeyColor getKeyColor() {
        return keyColor;
    }

    public String getDoorResourceId() {
        return doorResourceId;
    }
}
