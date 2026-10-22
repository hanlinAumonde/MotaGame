package com.demo.mota.content;

import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.TileType;
import com.demo.mota.engine.enums.WallType;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.floor.FloorDefinition;
import com.demo.mota.engine.map.floor.FloorDefinition.Placement;
import com.demo.mota.engine.map.floor.FloorSource;
import com.demo.mota.engine.map.floor.TileSpec;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从 {@code assets/levels/floor_N.json} 读楼层定义。
 *
 * <pre>{@code
 * {
 *   "floorNumber": 1,
 *   "regionId": "region-1",
 *   "tiles": [ [ {"type": "WALL", "bgResourceId": "bg_default", "wallType": "NORMAL", "wallResourceId": "wall_normal"}, ...], ... ],
 *   "items":    [ {"itemId": "yellow_key_1", "x": 3, "y": 5}, ... ],
 *   "monsters": [ {"monsterId": "slime_1", "x": 7, "y": 8, "clearExempt": true}, ... ],
 *   "playerSpawn": {"x": 6, "y": 11}
 * }
 * }</pre>
 *
 * <p>{@code tiles[y][x]}：外层下标是行。解析结果是不可变的 {@link FloorDefinition}，按层缓存——
 * 撤销 / 读档要重新载入整个区域，不必每次都读盘。
 */
public final class JsonFloorSource implements FloorSource {

    public static final String FLOOR_PATH_PREFIX = "/assets/levels/floor_";

    private final ContentSource source;
    private final Map<Integer, FloorDefinition> cache = new ConcurrentHashMap<>();

    public JsonFloorSource(ContentSource source) {
        this.source = source;
    }

    @Override
    public FloorDefinition load(int floor) {
        return cache.computeIfAbsent(floor, this::read);
    }

    private FloorDefinition read(int floor) {
        String path = FLOOR_PATH_PREFIX + floor + ".json";
        Map<String, Object> data = source.loadJsonResource(path, new TypeReference<>() {});
        try {
            return parse(floor, data);
        } catch (RuntimeException e) {
            throw new IllegalStateException("楼层文件格式错误：" + path + "（" + e.getMessage() + "）", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static FloorDefinition parse(int floor, Map<String, Object> data) {
        List<List<Map<String, Object>>> tileRows = (List<List<Map<String, Object>>>) data.get("tiles");
        List<List<TileSpec>> rows = new ArrayList<>();
        for (int y = 0; y < GameMap.SIDE; y++) {
            List<TileSpec> row = new ArrayList<>();
            for (int x = 0; x < GameMap.SIDE; x++) {
                row.add(tile(tileRows.get(y).get(x)));
            }
            rows.add(row);
        }
        return new FloorDefinition(floor, (String) data.get("regionId"), rows,
                placements((List<Map<String, Object>>) data.get("items"), "itemId"),
                placements((List<Map<String, Object>>) data.get("monsters"), "monsterId"),
                spawn((Map<String, Object>) data.get("playerSpawn")));
    }

    @SuppressWarnings("unchecked")
    private static TileSpec tile(Map<String, Object> d) {
        String bg = (String) d.get("bgResourceId");
        return switch (TileType.fromString((String) d.get("type"))) {
            case BACKGROUND -> new TileSpec.Background(bg);
            case WALL -> new TileSpec.Wall(bg, WallType.fromString((String) d.get("wallType")),
                    (String) d.get("wallResourceId"));
            case DOOR -> new TileSpec.Door(bg, KeyColor.fromString((String) d.get("keyColor")),
                    (String) d.get("doorResourceId"));
            case FLOOR_SWITCHER -> new TileSpec.Stairs(bg, Integer.parseInt(String.valueOf(d.get("aimedFloorId"))),
                    (String) d.get("switcherResourceId"), number(d, "targetSpawnX"), number(d, "targetSpawnY"));
            case TRICKY -> new TileSpec.Tricky(bg, (List<String>) d.get("boundMonsterIds"),
                    (String) d.get("trickyResourceId"));
            case REGION_GATE -> new TileSpec.RegionGate(bg, (String) d.get("gateResourceId"));
        };
    }

    private static List<Placement> placements(List<Map<String, Object>> list, String idKey) {
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .map(d -> new Placement((String) d.get(idKey), number(d, "x"), number(d, "y"),
                        Boolean.TRUE.equals(d.get("clearExempt"))))
                .toList();
    }

    private static Position spawn(Map<String, Object> d) {
        return d == null ? null : new Position(number(d, "x"), number(d, "y"));
    }

    private static int number(Map<String, Object> d, String key) {
        return ((Number) d.get(key)).intValue();
    }
}
