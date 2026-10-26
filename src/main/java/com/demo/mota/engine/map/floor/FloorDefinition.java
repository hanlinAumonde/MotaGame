package com.demo.mota.engine.map.floor;

import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;

import java.util.List;

/**
 * 一层楼的定义（纯数据，不可变）：地块、道具与怪物摆在哪、出生点。
 * 与存储格式无关——JSON、将来的 Tiled 地图都先变成它，再由 {@link GameMapFactory} 造出 {@code GameMap}。
 *
 * @param rows        地块，{@code rows.get(y).get(x)}，必须是 {@link GameMap#SIDE} × {@link GameMap#SIDE}
 * @param items       道具摆放（id = 道具表里的 id）
 * @param monsters    怪物摆放（id = 怪物表里的 id）
 * @param playerSpawn 玩家出生点，只对开局楼层生效；没有时为 null
 */
public record FloorDefinition(int floor, String regionId, List<List<TileSpec>> rows,
                              List<Placement> items, List<Placement> monsters, Position playerSpawn) {

    public FloorDefinition {
        if (rows == null || rows.size() != GameMap.SIDE
                || rows.stream().anyMatch(row -> row == null || row.size() != GameMap.SIDE)) {
            throw new IllegalArgumentException("第 " + floor + " 层的地块不是 " + GameMap.SIDE + "×" + GameMap.SIDE);
        }
        rows = rows.stream().map(List::copyOf).toList();
        items = items == null ? List.of() : List.copyOf(items);
        monsters = monsters == null ? List.of() : List.copyOf(monsters);
    }

    public TileSpec tileAt(int x, int y) {
        return rows.get(y).get(x);
    }

    /**
     * 一个道具 / 怪物的摆放。
     *
     * @param clearExempt 不计入区域结束检查（打不到的展柜怪物等）
     */
    public record Placement(String id, int x, int y, boolean clearExempt) {

        public Position position() {
            return new Position(x, y);
        }
    }
}
