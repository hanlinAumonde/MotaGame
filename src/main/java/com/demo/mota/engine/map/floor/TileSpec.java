package com.demo.mota.engine.map.floor;

import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.WallType;

import java.util.List;

/**
 * 楼层定义里一格地块的数据（不可变）。{@link GameMapFactory} 每次载入楼层都按它造出新的
 * {@code Tile} 对象（机关墙的绑定列表会随游戏变化，所以不能共享）。
 *
 * <p>封闭类型：新增一种地块要同时补上解析器与 {@code GameMapFactory} 的分派，漏了编译期就会报错。
 * 各字段的精灵都是精灵目录里的 id。
 */
public sealed interface TileSpec {

    /** 底图（每一格都有） */
    String background();

    record Background(String background) implements TileSpec {
    }

    record Wall(String background, WallType wallType, String sprite) implements TileSpec {
    }

    record Door(String background, KeyColor keyColor, String sprite) implements TileSpec {
    }

    /** 楼梯：去往 {@code targetFloor}，落在 ({@code spawnX}, {@code spawnY}) */
    record Stairs(String background, int targetFloor, String sprite, int spawnX, int spawnY) implements TileSpec {
    }

    /** 机关墙：绑定的怪物都被击败后开启 */
    record Tricky(String background, List<String> boundMonsterIds, String sprite) implements TileSpec {
        public Tricky {
            boundMonsterIds = boundMonsterIds == null ? List.of() : List.copyOf(boundMonsterIds);
        }
    }

    /** 区域检测格 */
    record RegionGate(String background, String sprite) implements TileSpec {
    }
}
