package com.demo.mota.engine.snapshot;

import java.util.List;

/**
 * 一层地图相对楼层文件的变化。地图的变化只有这四种：
 * 怪物被击败、道具被拾取、格子被换成背景（门 / 暗墙 / 机关墙 / 区域检测格），以及机关墙绑定的怪物被删减。
 *
 * @param defeatedMonsters 已击败的怪物位置
 * @param takenItems       已拾取的道具位置
 * @param openedTiles      已换成背景的格子
 * @param trickyBindings   仍未开启的机关墙当前还绑定着哪些怪物
 */
public record FloorState(int floor,
                         List<Cell> defeatedMonsters,
                         List<Cell> takenItems,
                         List<Cell> openedTiles,
                         List<TrickyBinding> trickyBindings) {

    public FloorState {
        defeatedMonsters = PlayerSnapshot.orEmpty(defeatedMonsters);
        takenItems = PlayerSnapshot.orEmpty(takenItems);
        openedTiles = PlayerSnapshot.orEmpty(openedTiles);
        trickyBindings = PlayerSnapshot.orEmpty(trickyBindings);
    }

    public record Cell(int x, int y) {
    }

    public record TrickyBinding(int x, int y, List<String> boundMonsterIds) {
        public TrickyBinding {
            boundMonsterIds = PlayerSnapshot.orEmpty(boundMonsterIds);
        }
    }
}
