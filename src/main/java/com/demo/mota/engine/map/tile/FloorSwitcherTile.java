package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.map.Position;

/** 楼梯口：踩上去切到 {@code targetFloor}，落在 {@code targetSpawn}。 */
public final class FloorSwitcherTile extends Tile {
    private final int targetFloor;
    private final String switcherResourceId;
    // 玩家在目标楼层落地的坐标（通常直接指向目标楼层对应楼梯的自身坐标，
    // 这样玩家切层后"站在"目标楼梯上，连续按同一方向不会再次触发切层）
    private final Position targetSpawn;

    public FloorSwitcherTile(Position position, String bgResourceId, int targetFloor,
                             String switcherResourceId, Position targetSpawn) {
        super(position, true, false, bgResourceId);
        this.targetFloor = targetFloor;
        this.switcherResourceId = switcherResourceId;
        this.targetSpawn = targetSpawn;
    }

    public int getTargetFloor() {
        return targetFloor;
    }

    public String getSwitcherResourceId() {
        return switcherResourceId;
    }

    public Position getTargetSpawn() {
        return targetSpawn;
    }
}
