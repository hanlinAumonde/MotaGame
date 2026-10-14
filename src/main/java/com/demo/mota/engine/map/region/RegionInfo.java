package com.demo.mota.engine.map.region;

import java.util.List;

/**
 * 一个地图区域：连续的若干层，作为一个整体载入 / 卸载。
 *
 * @param regionId 区域 id，楼层文件里的 {@code regionId} 与之对应
 * @param name     展示用名称（状态栏）
 * @param floors   该区域包含的楼层号
 */
public record RegionInfo(String regionId, String name, List<Integer> floors) {

    public RegionInfo {
        if (regionId == null || regionId.isBlank()) {
            throw new IllegalArgumentException("区域缺少 regionId");
        }
        if (floors == null || floors.isEmpty()) {
            throw new IllegalArgumentException("区域 " + regionId + " 没有任何楼层");
        }
        name = name == null ? regionId : name;
        floors = List.copyOf(floors);
    }

    public boolean contains(int floor) {
        return floors.contains(floor);
    }
}
