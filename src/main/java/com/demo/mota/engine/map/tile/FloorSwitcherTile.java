package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.map.Position;

public class FloorSwitcherTile extends Tile {
    private final String aimedFloorId;
    private final String switcherResourceId;
    // 玩家在目标楼层落地的坐标（通常直接指向目标楼层对应楼梯的自身坐标，
    // 这样玩家切层后"站在"目标楼梯上，连续按同一方向不会再次触发切层）
    private final short targetSpawnX;
    private final short targetSpawnY;

    public FloorSwitcherTile(Position position, String bgResourceId, String aimedFloorId,
                             String switcherResourceId, short targetSpawnX, short targetSpawnY) {
        super(position, true, false, bgResourceId);
        this.aimedFloorId = aimedFloorId;
        this.switcherResourceId = switcherResourceId;
        this.targetSpawnX = targetSpawnX;
        this.targetSpawnY = targetSpawnY;
    }

    public String getAimedFloorId() {
        return aimedFloorId;
    }

    public String getSwitcherResourceId() {
        return switcherResourceId;
    }

    public Position getTargetSpawn() {
        return new Position(targetSpawnX, targetSpawnY);
    }
}
