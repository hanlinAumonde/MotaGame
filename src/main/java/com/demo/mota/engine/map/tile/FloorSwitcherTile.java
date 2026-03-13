package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.map.Position;

public class FloorSwitcherTile extends Tile {
    private final String aimedFloorId;
    private final String switcherResourceId;

    public FloorSwitcherTile(Position position, String bgResourceId, String aimedFloorId, String switcherResourceId) {
        super(position, true, false, bgResourceId);
        this.aimedFloorId = aimedFloorId;
        this.switcherResourceId = switcherResourceId;
    }

    public String getAimedFloorId() {
        return aimedFloorId;
    }

    public String getSwitcherResourceId() {
        return switcherResourceId;
    }
}
