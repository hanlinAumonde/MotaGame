package com.demo.mota.engine.map;

import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.state.monster.Monster;

import java.util.Map;

public class GameMap {
    private Tile[][] tiles;
    private int floorNumber;
    private Map<Position, Item> items;
    private Map<Position, Monster> monsters;

    GameMap(){}

    public GameMap(Tile[][] tiles, int floorNumber, Map<Position, Item> items, Map<Position, Monster> monsters) {
        this.tiles = tiles;
        this.floorNumber = floorNumber;
        this.items = items;
        this.monsters = monsters;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Tile getTileAt(Position position) {
        return tiles[position.getX_index()][position.getY_index()];
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
}
