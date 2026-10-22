package com.demo.mota.engine.map;

import com.demo.mota.engine.Item.AbilityGem;
import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.TileType;
import com.demo.mota.engine.enums.WallType;
import com.demo.mota.engine.factory.item.ItemFactory;
import com.demo.mota.engine.factory.monster.MonsterFactory;
import com.demo.mota.engine.map.region.*;
import com.demo.mota.engine.map.tile.*;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.snapshot.FloorState;
import com.demo.mota.engine.snapshot.WorldSnapshot;
import com.demo.mota.engine.state.monster.Monster;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_FLOOR_PATH_PREFIX;
import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

/**
 * 地图管理器：以**区域**为单位载入 / 卸载地图（区域划分见 {@link RegionCatalog}）。
 *
 * <p>任一时刻只持有当前区域的全部楼层；跨区由 {@link #enterRegion} 完成（卸载当前、载入新区域），
 * 区域单向推进，已通过的区域卸载后不再回来。区域结束检查（{@link #checkRegionClear}）
 * 与出口兜底（{@link #isRegionExitUnlocked}）也在这里，供移动分发与将来的检查界面调用。
 */
public class MapManager {
    private final RegionCatalog regionCatalog;

    private GameMap currentMap;
    private Position playerPosition;

    private RegionInfo currentRegion;
    // 当前区域的全部楼层；楼层号 → 地图
    private final Map<Integer, GameMap> regionMaps = new HashMap<>();
    // 当前区域各楼层文件里写的 playerSpawn（只有新开一局的那一层会用到）
    private final Map<Integer, Position> regionSpawns = new HashMap<>();

    public MapManager() {
        this(RegionCatalog.get());
    }

    public MapManager(RegionCatalog regionCatalog) {
        this.regionCatalog = regionCatalog;
    }

    public GameMap getCurrentMap() {
        return currentMap;
    }

    public Position getPlayerPosition() {
        return playerPosition;
    }

    public void setPlayerPosition(Position playerPosition) {
        this.playerPosition = playerPosition;
    }

    /** 当前所在区域（尚未载入任何楼层时为 null） */
    public RegionInfo getCurrentRegion() {
        return currentRegion;
    }

    /**
     * 切到指定楼层。楼层必须属于当前区域；尚未进入任何区域时（新开一局）自动进入该层所在区域，
     * 并以该层文件里的 {@code playerSpawn} 作为玩家位置。
     *
     * @throws IllegalStateException 楼层属于别的区域——跨区必须经 {@link #enterRegion}，以免绕过区域检查
     */
    public void loadFloor(int floorNumber) {
        if (currentRegion == null) {
            enterRegion(regionCatalog.regionOf(floorNumber).regionId());
            if (playerPosition == null) {
                playerPosition = regionSpawns.get(floorNumber);
            }
        }
        GameMap map = regionMaps.get(floorNumber);
        if (map == null) {
            throw new IllegalStateException("第 " + floorNumber + " 层不在当前区域 "
                    + currentRegion.regionId() + " 内，跨区请使用 enterRegion");
        }
        this.currentMap = map;
    }

    /**
     * 切换楼层并设置玩家出生位置
     */
    public void switchFloor(int floorNumber, Position spawnPosition) {
        loadFloor(floorNumber);
        this.playerPosition = spawnPosition;
    }

    // ── 区域 ────────────────────────────────────────────────

    /**
     * 一次读入区域的全部楼层，替换掉当前持有的地图。
     * 楼层文件里的 {@code regionId} 与区域清单不一致时抛异常（打包错误）。
     */
    public void loadRegion(String regionId) {
        RegionInfo region = regionCatalog.byId(regionId);
        Map<Integer, GameMap> maps = new HashMap<>();
        Map<Integer, Position> spawns = new HashMap<>();
        for (int floor : region.floors()) {
            Map<String, Object> mapData = readFloorFile(floor);
            Object declared = mapData.get("regionId");
            if (!region.regionId().equals(declared)) {
                throw new IllegalStateException("第 " + floor + " 层的 regionId 为 " + declared
                        + "，但区域清单把它划入 " + region.regionId());
            }
            maps.put(floor, parseMapData(mapData, floor));
            Position spawn = parseSpawn(mapData);
            if (spawn != null) spawns.put(floor, spawn);
        }
        unloadRegion();
        regionMaps.putAll(maps);
        regionSpawns.putAll(spawns);
        currentRegion = region;
    }

    /** 卸载当前区域的全部地图（已通过的区域此时早已不在内存，因此只有当前区域可卸） */
    public void unloadRegion() {
        regionMaps.clear();
        regionSpawns.clear();
        currentRegion = null;
        currentMap = null;
    }

    /**
     * 进入另一个区域：卸载当前区域并载入新区域。调用方随后用 {@link #switchFloor} 落到目标楼层。
     * 区域切换的动画 / 提示尚未实现，返回值留作将来 UI 层的取数口。
     */
    public RegionTransition enterRegion(String regionId) {
        RegionInfo from = currentRegion;
        loadRegion(regionId);
        return new RegionTransition(from, currentRegion);
    }

    /** 目标楼层相对当前区域的位置 */
    public RegionRelation relationTo(int floorNumber) {
        return regionCatalog.relation(currentRegion, floorNumber);
    }

    /** 楼层所属区域 */
    public RegionInfo regionOf(int floorNumber) {
        return regionCatalog.regionOf(floorNumber);
    }

    /**
     * 区域结束检查：当前区域里还剩多少怪物没杀、多少能力宝石没拾取（标了 {@code clearExempt} 的不计）。
     * 由区域检测格调用；将来的检查详情界面直接用这份报告。
     */
    public RegionClearReport checkRegionClear() {
        Map<Integer, Integer> monsters = new HashMap<>();
        Map<Integer, Integer> gems = new HashMap<>();
        regionMaps.forEach((floor, map) -> {
            int monsterCount = (int) map.getMonsters().keySet().stream()
                    .filter(map::isNotClearExempt)
                    .count();
            int gemCount = (int) map.getItems().entrySet().stream()
                    .filter(e -> e.getValue() instanceof AbilityGem && map.isNotClearExempt(e.getKey()))
                    .count();
            monsters.put(floor, monsterCount);
            gems.put(floor, gemCount);
        });
        return new RegionClearReport(currentRegion.regionId(), monsters, gems);
    }

    /**
     * 出口兜底：当前区域的检测格是否都已开启（没有检测格的区域视为放行）。
     * 只看检测格本身——检查的实质内容已经由检测格把过关了。
     */
    public boolean isRegionExitUnlocked() {
        return regionMaps.values().stream().allMatch(map -> map.getAllRegionGates().isEmpty());
    }

    /**
     * 将指定位置的 Tile 替换为背景 Tile（用于开门、暗墙消失、机关开启等）
     */
    public void replaceTileWithBackground(Position position) {
        currentMap.openTile(position);
    }

    // ── 快照 ────────────────────────────────────────────────

    /** 导出当前区域各层的变化 + 当前楼层与玩家位置 */
    public WorldSnapshot toSnapshot() {
        List<FloorState> floors = regionMaps.values().stream()
                .sorted(Comparator.comparingInt(GameMap::getFloorNumber))
                .map(GameMap::toFloorState)
                .toList();
        return new WorldSnapshot(currentRegion.regionId(), currentMap.getFloorNumber(),
                playerPosition.getX_index(), playerPosition.getY_index(), floors);
    }

    /**
     * 按快照恢复：重新解析快照所在区域的全部楼层，把各层记下的变化施加上去，再落到当时的楼层与位置。
     * 快照里有、当前区域清单里已没有的楼层直接忽略。
     *
     * @throws IllegalArgumentException 区域 id 已不在区域清单中
     */
    public void restore(WorldSnapshot snapshot) {
        loadRegion(snapshot.regionId());
        for (FloorState floor : snapshot.floors()) {
            GameMap map = regionMaps.get(floor.floor());
            if (map != null) {
                map.applyFloorState(floor);
            }
        }
        loadFloor(snapshot.currentFloor());
        this.playerPosition = new Position((short) snapshot.playerX(), (short) snapshot.playerY());
    }

    /**
     * 读取楼层 JSON
     * 文件路径格式: assets/levels/floor_{floorNumber}.json
     */
    private Map<String, Object> readFloorFile(int floorNumber) {
        String filePath = MAP_FLOOR_PATH_PREFIX + floorNumber + ".json";
        return ResourceManager.getInstance().loadJsonResource(filePath, new TypeReference<>() {});
    }

    /**
     * 解析地图 JSON 数据为 GameMap 对象
     *
     * JSON 格式:
     * {
     *   "floorNumber": 1,
     *   "regionId": "region-1",
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
     *     {"monsterId": "slime_2", "x": 9, "y": 8, "clearExempt": true},   // 不计入区域检查
     *     ...
     *   ],
     *   "playerSpawn": {"x": 12, "y": 24}
     * }
     */
    @SuppressWarnings("unchecked")
    private GameMap parseMapData(Map<String, Object> mapData, int floorNumber) {
        Set<Position> clearExempt = new HashSet<>();
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
                if (Boolean.TRUE.equals(itemData.get("clearExempt"))) clearExempt.add(pos);
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
                if (Boolean.TRUE.equals(monsterData.get("clearExempt"))) clearExempt.add(pos);
            }
        }

        return new GameMap(tiles, floorNumber, items, monsters, clearExempt);
    }

    /** 楼层文件里的玩家出生点；只对新开一局的那一层生效，其余楼层的落点由来源楼梯的 targetSpawn 决定 */
    @SuppressWarnings("unchecked")
    private Position parseSpawn(Map<String, Object> mapData) {
        Map<String, Object> spawnData = (Map<String, Object>) mapData.get("playerSpawn");
        if (spawnData == null) return null;
        return new Position(
                ((Number) spawnData.get("x")).shortValue(),
                ((Number) spawnData.get("y")).shortValue()
        );
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
                short spawnX = ((Number) tileData.get("targetSpawnX")).shortValue();
                short spawnY = ((Number) tileData.get("targetSpawnY")).shortValue();
                yield new FloorSwitcherTile(pos, bgResourceId, aimedFloorId, switcherResourceId, spawnX, spawnY);
            }

            case TRICKY -> {
                List<String> boundMonsterIds = (List<String>) tileData.get("boundMonsterIds");
                String trickyResourceId = (String) tileData.get("trickyResourceId");
                yield new TrickyTile(pos, bgResourceId, boundMonsterIds, trickyResourceId);
            }

            case REGION_GATE -> new RegionGateTile(pos, bgResourceId, (String) tileData.get("gateResourceId"));
        };
    }
}
