package com.demo.mota.engine.map;

import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.map.tile.TrickyTile;
import com.demo.mota.engine.state.monster.Monster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

public class GameMap {
    private final Tile[][] tiles;
    private final int floorNumber;
    private final Map<Position, Item> items;
    private final Map<Position, Monster> monsters;

    public GameMap(Tile[][] tiles, int floorNumber, Map<Position, Item> items, Map<Position, Monster> monsters) {
        this.tiles = tiles;
        this.floorNumber = floorNumber;
        this.items = new HashMap<>(items);
        this.monsters = new HashMap<>(monsters);
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

    /**
     * 获取所有 TrickyTile，用于怪物击败时检查机关状态
     */
    public List<TrickyTile> getAllTrickyTiles() {
        List<TrickyTile> result = new ArrayList<>();
        for (int x = 0; x < MAP_SIDE_LENGTH; x++) {
            for (int y = 0; y < MAP_SIDE_LENGTH; y++) {
                if (tiles[x][y] instanceof TrickyTile tricky) {
                    result.add(tricky);
                }
            }
        }
        return result;
    }
}
