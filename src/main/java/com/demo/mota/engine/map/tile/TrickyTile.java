package com.demo.mota.engine.map.tile;

import com.demo.mota.engine.map.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * 机关墙：默认不可通行，当绑定的所有怪物被击败后变为可通行。
 * boundMonsterIds 记录绑定的怪物ID列表（可跨楼层），
 * 由事件处理器在怪物被击败时检查并更新状态。
 */
public class TrickyTile extends Tile {
    private final List<String> boundMonsterIds;
    private final String trickyResourceId;

    public TrickyTile(Position position, String bgResourceId,
                      List<String> boundMonsterIds, String trickyResourceId) {
        super(position, false, true, bgResourceId);
        this.boundMonsterIds = new ArrayList<>(boundMonsterIds);
        this.trickyResourceId = trickyResourceId;
    }

    public List<String> getBoundMonsterIds() {
        return boundMonsterIds;
    }

    /**
     * 移除一个已被击败的怪物ID，返回列表是否已清空（即机关是否应开启）
     */
    public boolean removeBoundMonster(String monsterId) {
        boundMonsterIds.remove(monsterId);
        return boundMonsterIds.isEmpty();
    }

    public String getTrickyResourceId() {
        return trickyResourceId;
    }
}
