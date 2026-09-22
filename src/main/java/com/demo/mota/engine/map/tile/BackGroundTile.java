package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.map.Position;

public final class BackGroundTile extends Tile {
    public BackGroundTile(Position position, String bgResourceId) {
        super(position, true, false, bgResourceId);
    }
}
