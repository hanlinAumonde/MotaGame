package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.enums.WallType;
import com.demo.mota.engine.map.Position;

public class WallTile extends Tile {
    private final WallType wallType;
    private final String wallResourceId;

    public WallTile(Position position, String bgResourceId, WallType wallType, String wallResourceId) {
        // 暗墙(DARK)是可变的，普通墙和岩浆墙不可通行也不可变（除非有道具，后续扩展）
        super(position, false, wallType == WallType.DARK, bgResourceId);
        this.wallType = wallType;
        this.wallResourceId = wallResourceId;
    }

    public WallType getWallType() {
        return wallType;
    }

    public String getWallResourceId() {
        return wallResourceId;
    }
}
