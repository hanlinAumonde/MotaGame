package com.demo.mota.engine.map;

import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.TileType;
import com.demo.mota.engine.enums.WallType;
import com.demo.mota.engine.factory.item.ItemFactory;
import com.demo.mota.engine.factory.monster.MonsterFactory;
import com.demo.mota.engine.map.tile.*;
import com.demo.mota.engine.state.monster.Monster;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_FLOOR_PATH_PREFIX;
import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

public class MapManager {
    private GameMap currentMap;
    private Position playerPosition;

    // 已加载过的地图缓存，支持楼层切换时快速恢复
    private final Map<Integer, GameMap> mapCache = new HashMap<>();

    public GameMap getCurrentMap() {
        return currentMap;
    }

    public Position getPlayerPosition() {
        return playerPosition;
    }

    public void setPlayerPosition(Position playerPosition) {
        this.playerPosition = playerPosition;
    }

    /**
     * 加载指定楼层的地图。优先从缓存获取，否则从文件加载。
     */
    public void loadFloor(int floorNumber) {
        if (mapCache.containsKey(floorNumber)) {
            this.currentMap = mapCache.get(floorNumber);
            return;
        }
        GameMap map = loadMapFromFile(floorNumber);
        mapCache.put(floorNumber, map);
        this.currentMap = map;
    }

    /**
     * 切换楼层并设置玩家出生位置
     */
    public void switchFloor(int floorNumber, Position spawnPosition) {
        loadFloor(floorNumber);
        this.playerPosition = spawnPosition;
    }

    /**
     * 将指定位置的 Tile 替换为背景 Tile（用于开门、暗墙消失、机关开启等）
     */
    public void replaceTileWithBackground(Position position) {
        Tile original = currentMap.getTileAt(position);
        BackGroundTile bg = new BackGroundTile(position, original.getBgResourceId());
        currentMap.setTileAt(position, bg);
    }

    /**
     * 从 JSON 文件加载地图数据
     * 文件路径格式: data/map/floor_{floorNumber}.json
     */
    private GameMap loadMapFromFile(int floorNumber) {
        String filePath = MAP_FLOOR_PATH_PREFIX + floorNumber + ".json";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new RuntimeException("Map file not found: " + filePath);
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> mapData = mapper.readValue(inputStream, new TypeReference<>() {});
            return parseMapData(mapData, floorNumber);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load map for floor " + floorNumber, e);
        }
    }

    /**
     * 解析地图 JSON 数据为 GameMap 对象
     *
     * JSON 格式:
     * {
     *   "floorNumber": 1,
     *   "tiles": [
     *     [{"type": "BACKGROUND", "bgResourceId": "floor_1"}, ...],
     *     ...
     *   ],
     *   "items": [
     *     {"itemId": "yellow_key_1", "x": 3, "y": 5},
     *     ...
     *   ],
     *   "monsters": [
     *     {"monsterId": "slime_1", "x": 7, "y": 8},
     *     ...
     *   ],
     *   "playerSpawn": {"x": 12, "y": 24}
     * }
     */
    @SuppressWarnings("unchecked")
    private GameMap parseMapData(Map<String, Object> mapData, int floorNumber) {
        // 解析 tiles
        Tile[][] tiles = new Tile[MAP_SIDE_LENGTH][MAP_SIDE_LENGTH];
        List<List<Map<String, Object>>> tileRows = (List<List<Map<String, Object>>>) mapData.get("tiles");

        for (int y = 0; y < MAP_SIDE_LENGTH; y++) {
            List<Map<String, Object>> row = tileRows.get(y);
            for (int x = 0; x < MAP_SIDE_LENGTH; x++) {
                Map<String, Object> tileData = row.get(x);
                Position pos = new Position((short) x, (short) y);
                tiles[x][y] = parseTile(tileData, pos);
            }
        }

        // 解析 items
        Map<Position, Item> items = new HashMap<>();
        List<Map<String, Object>> itemList = (List<Map<String, Object>>) mapData.get("items");
        if (itemList != null) {
            for (Map<String, Object> itemData : itemList) {
                String itemId = (String) itemData.get("itemId");
                Position pos = new Position(((Number) itemData.get("x")).shortValue(),
                        ((Number) itemData.get("y")).shortValue());
                items.put(pos, ItemFactory.getInstance().createById(itemId));
            }
        }

        // 解析 monsters
        Map<Position, Monster> monsters = new HashMap<>();
        List<Map<String, Object>> monsterList = (List<Map<String, Object>>) mapData.get("monsters");
        if (monsterList != null) {
            for (Map<String, Object> monsterData : monsterList) {
                String monsterId = (String) monsterData.get("monsterId");
                Position pos = new Position(((Number) monsterData.get("x")).shortValue(),
                        ((Number) monsterData.get("y")).shortValue());
                monsters.put(pos, MonsterFactory.getInstance().createById(monsterId));
            }
        }

        // 解析玩家出生点
        Map<String, Object> spawnData = (Map<String, Object>) mapData.get("playerSpawn");
        if (spawnData != null && playerPosition == null) {
            this.playerPosition = new Position(
                    ((Number) spawnData.get("x")).shortValue(),
                    ((Number) spawnData.get("y")).shortValue()
            );
        }

        return new GameMap(tiles, floorNumber, items, monsters);
    }

    @SuppressWarnings("unchecked")
    private Tile parseTile(Map<String, Object> tileData, Position pos) {
        TileType type = TileType.fromString((String) tileData.get("type"));
        String bgResourceId = (String) tileData.get("bgResourceId");

        return switch (type) {
            case BACKGROUND -> new BackGroundTile(pos, bgResourceId);

            case WALL -> {
                WallType wallType = WallType.fromString((String) tileData.get("wallType"));
                String wallResourceId = (String) tileData.get("wallResourceId");
                yield new WallTile(pos, bgResourceId, wallType, wallResourceId);
            }

            case DOOR -> {
                KeyColor keyColor = KeyColor.fromString((String) tileData.get("keyColor"));
                String doorResourceId = (String) tileData.get("doorResourceId");
                yield new DoorTile(pos, bgResourceId, keyColor, doorResourceId);
            }

            case FLOOR_SWITCHER -> {
                String aimedFloorId = (String) tileData.get("aimedFloorId");
                String switcherResourceId = (String) tileData.get("switcherResourceId");
                yield new FloorSwitcherTile(pos, bgResourceId, aimedFloorId, switcherResourceId);
            }

            case TRICKY -> {
                List<String> boundMonsterIds = (List<String>) tileData.get("boundMonsterIds");
                String trickyResourceId = (String) tileData.get("trickyResourceId");
                yield new TrickyTile(pos, bgResourceId, boundMonsterIds, trickyResourceId);
            }
        };
    }
}
