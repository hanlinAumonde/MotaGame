package com.demo.mota.engine.map;

import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.map.tile.BackGroundTile;
import com.demo.mota.engine.map.tile.RegionGateTile;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.map.tile.TrickyTile;
import com.demo.mota.engine.snapshot.FloorState;
import com.demo.mota.engine.state.monster.Monster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameMap {
    /** 地图边长（格）：每层都是 SIDE × SIDE */
    public static final int SIDE = 13;

    private final Tile[][] tiles;
    private final int floorNumber;
    private final Map<Position, Item> items;
    private final Map<Position, Monster> monsters;
    /** 不计入区域结束检查的怪物 / 道具位置（如四面封闭、打不到的展柜怪物） */
    private final Set<Position> clearExempt;

    // 相对楼层文件的变化，供快照导出（见 FloorState）；只增不减——地图上被移走的东西不会再放回来
    private final Set<Position> defeatedMonsters = new LinkedHashSet<>();
    private final Set<Position> takenItems = new LinkedHashSet<>();
    private final Set<Position> openedTiles = new LinkedHashSet<>();

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
        return tiles[position.x()][position.y()];
    }

    public void setTileAt(Position position, Tile tile) {
        tiles[position.x()][position.y()] = tile;
    }

    public Item getItemAt(Position position) {
        return items.get(position);
    }

    public Monster getMonsterAt(Position position) {
        return monsters.get(position);
    }

    public void removeMonsterAt(Position position) {
        if (monsters.remove(position) != null) {
            defeatedMonsters.add(position);
        }
    }

    public void removeItemAt(Position position) {
        if (items.remove(position) != null) {
            takenItems.add(position);
        }
    }

    /** 把该位置的格子换成同底图的背景（开门、暗墙被揭、机关墙 / 检测格开启） */
    public void openTile(Position position) {
        Tile original = getTileAt(position);
        setTileAt(position, new BackGroundTile(position, original.getBgResourceId()));
        openedTiles.add(position);
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
        for (int x = 0; x < SIDE; x++) {
            for (int y = 0; y < SIDE; y++) {
                if (type.isInstance(tiles[x][y])) {
                    result.add(type.cast(tiles[x][y]));
                }
            }
        }
        return result;
    }

    // ==================== 快照 ====================

    /** 导出本层相对楼层文件的变化 */
    public FloorState toFloorState() {
        List<FloorState.TrickyBinding> bindings = getAllTrickyTiles().stream()
                .map(tricky -> new FloorState.TrickyBinding(tricky.getPosition().x(),
                        tricky.getPosition().y(), List.copyOf(tricky.getBoundMonsterIds())))
                .sorted(Comparator.comparingInt(FloorState.TrickyBinding::y).thenComparingInt(FloorState.TrickyBinding::x))
                .toList();
        return new FloorState(floorNumber, toCells(defeatedMonsters), toCells(takenItems), toCells(openedTiles), bindings);
    }

    /**
     * 把快照里记下的变化施加到刚从楼层文件解析出来的本层上。
     * 快照里的位置在当前楼层文件中已对不上（塔的数据改过）时跳过该条，不让整层恢复失败。
     */
    public void applyFloorState(FloorState state) {
        state.defeatedMonsters().forEach(cell -> removeMonsterAt(toPosition(cell)));
        state.takenItems().forEach(cell -> removeItemAt(toPosition(cell)));
        for (FloorState.Cell cell : state.openedTiles()) {
            Position position = toPosition(cell);
            if (position.isInside()) {
                openTile(position);
            }
        }
        for (FloorState.TrickyBinding binding : state.trickyBindings()) {
            Position position = new Position(binding.x(), binding.y());
            if (position.isInside() && getTileAt(position) instanceof TrickyTile tricky) {
                tricky.resetBoundMonsters(binding.boundMonsterIds());
            }
        }
    }

    private static List<FloorState.Cell> toCells(Collection<Position> positions) {
        return positions.stream().map(p -> new FloorState.Cell(p.x(), p.y())).toList();
    }

    private static Position toPosition(FloorState.Cell cell) {
        return new Position(cell.x(), cell.y());
    }
}
