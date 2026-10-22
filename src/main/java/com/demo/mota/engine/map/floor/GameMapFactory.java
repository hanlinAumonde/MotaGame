package com.demo.mota.engine.map.floor;

import com.demo.mota.engine.catalog.ItemCatalog;
import com.demo.mota.engine.catalog.MonsterCatalog;
import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.tile.BackGroundTile;
import com.demo.mota.engine.map.tile.DoorTile;
import com.demo.mota.engine.map.tile.FloorSwitcherTile;
import com.demo.mota.engine.map.tile.RegionGateTile;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.map.tile.TrickyTile;
import com.demo.mota.engine.map.tile.WallTile;
import com.demo.mota.engine.state.monster.Monster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 按楼层定义造出一张新的 {@link GameMap}：地块、道具、怪物都是新对象，
 * 因此同一层可以反复造（新开一局、撤销、读档），互不影响。
 */
public final class GameMapFactory {

    private final ItemCatalog items;
    private final MonsterCatalog monsters;

    public GameMapFactory(ItemCatalog items, MonsterCatalog monsters) {
        this.items = items;
        this.monsters = monsters;
    }

    public GameMap build(FloorDefinition floor) {
        Tile[][] tiles = new Tile[GameMap.SIDE][GameMap.SIDE];
        for (int y = 0; y < GameMap.SIDE; y++) {
            for (int x = 0; x < GameMap.SIDE; x++) {
                tiles[x][y] = tile(floor.tileAt(x, y), new Position(x, y));
            }
        }
        Set<Position> clearExempt = new HashSet<>();
        Map<Position, Item> placedItems = new HashMap<>();
        for (FloorDefinition.Placement placement : floor.items()) {
            placedItems.put(placement.position(), items.create(placement.id()));
            if (placement.clearExempt()) clearExempt.add(placement.position());
        }
        Map<Position, Monster> placedMonsters = new HashMap<>();
        for (FloorDefinition.Placement placement : floor.monsters()) {
            placedMonsters.put(placement.position(), monsters.create(placement.id()));
            if (placement.clearExempt()) clearExempt.add(placement.position());
        }
        return new GameMap(tiles, floor.floor(), placedItems, placedMonsters, clearExempt);
    }

    private static Tile tile(TileSpec spec, Position pos) {
        return switch (spec) {
            case TileSpec.Background b -> new BackGroundTile(pos, b.background());
            case TileSpec.Wall w -> new WallTile(pos, w.background(), w.wallType(), w.sprite());
            case TileSpec.Door d -> new DoorTile(pos, d.background(), d.keyColor(), d.sprite());
            case TileSpec.Stairs s -> new FloorSwitcherTile(pos, s.background(), s.targetFloor(), s.sprite(),
                    new Position(s.spawnX(), s.spawnY()));
            case TileSpec.Tricky t -> new TrickyTile(pos, t.background(), t.boundMonsterIds(), t.sprite());
            case TileSpec.RegionGate g -> new RegionGateTile(pos, g.background(), g.sprite());
        };
    }
}
