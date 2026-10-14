package com.demo.mota.engine.map;

import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.map.tile.RegionGateTile;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.map.tile.TrickyTile;
import com.demo.mota.engine.state.monster.Monster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

public class GameMap {
    private final Tile[][] tiles;
    private final int floorNumber;
    private final Map<Position, Item> items;
    private final Map<Position, Monster> monsters;
    /** 不计入区域结束检查的怪物 / 道具位置（如四面封闭、打不到的展柜怪物） */
    private final Set<Position> clearExempt;

    public GameMap(Tile[][] tiles, int floorNumber, Map<Position, Item> items, Map<Position, Monster> monsters) {
        this(tiles, floorNumber, items, monsters, Set.of());
    }

    public GameMap(Tile[][] tiles, int floorNumber, Map<Position, Item> items, Map<Position, Monster> monsters,
                   Set<Position> clearExempt) {
        this.tiles = tiles;
        this.floorNumber = floorNumber;
        this.items = new HashMap<>(items);
        this.monsters = new HashMap<>(monsters);
        this.clearExempt = Set.copyOf(clearExempt);
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Tile getTileAt(Position position) {
        return tiles[position.getX_index()][position.getY_index()];
    }

    public void setTileAt(Position position, Tile tile) {
        tiles[position.getX_index()][position.getY_index()] = tile;
    }

    public Item getItemAt(Position position) {
        return items.get(position);
    }

    public Monster getMonsterAt(Position position) {
        return monsters.get(position);
    }

    public void removeMonsterAt(Position position) {
        monsters.remove(position);
    }

    public void removeItemAt(Position position) {
        items.remove(position);
    }

    public Map<Position, Monster> getMonsters() {
        return monsters;
    }

    public Map<Position, Item> getItems() {
        return items;
    }

    public boolean isNotClearExempt(Position position) {
        return !clearExempt.contains(position);
    }

    /**
     * 获取所有 TrickyTile，用于怪物击败时检查机关状态
     */
    public List<TrickyTile> getAllTrickyTiles() {
        return tilesOfType(TrickyTile.class);
    }

    /** 获取所有尚未开启的区域检测格（开启后即替换为背景，不再出现在这里） */
    public List<RegionGateTile> getAllRegionGates() {
        return tilesOfType(RegionGateTile.class);
    }

    private <T extends Tile> List<T> tilesOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (int x = 0; x < MAP_SIDE_LENGTH; x++) {
            for (int y = 0; y < MAP_SIDE_LENGTH; y++) {
                if (type.isInstance(tiles[x][y])) {
                    result.add(type.cast(tiles[x][y]));
                }
            }
        }
        return result;
    }
}
