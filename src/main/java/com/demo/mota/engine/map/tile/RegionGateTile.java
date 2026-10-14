package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.map.Position;

/**
 * 区域检测格：玩家主动接触时做区域结束检查（杀光本区域怪物 + 拾取全部能力宝石），
 * 通过后变为背景、可以通行；未通过则挡住。
 *
 * <p>地图设计时应把它卡在通往区域出口的必经之路上，且它之后不能再有需计数的怪物 / 宝石，
 * 否则检查永远过不去。出口楼梯另有兜底：本区域还有未开启的检测格时不放行。
 */
public final class RegionGateTile extends Tile {
    private final String gateResourceId;

    public RegionGateTile(Position position, String bgResourceId, String gateResourceId) {
        super(position, false, true, bgResourceId);
        this.gateResourceId = gateResourceId;
    }

    public String getGateResourceId() {
        return gateResourceId;
    }
}
