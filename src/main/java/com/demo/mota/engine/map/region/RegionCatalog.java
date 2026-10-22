package com.demo.mota.engine.map.region;

import com.demo.mota.engine.resource.ResourceManager;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.demo.mota.engine.configs.MapConfigConstants.REGION_MANIFEST_PATH;

/**
 * 区域清单（{@code assets/json/regions.json}）：列出全部区域，**顺序即推进顺序**。
 *
 * <p>楼层文件各自写着 {@code regionId}，但「这个区域有哪些层」必须有一处集中回答——
 * 否则载入一个区域就得扫描全部楼层文件，而资源 provider 只能按路径取流、不能列目录
 * （与精灵图用清单而不扫目录是同一个理由）。两边不一致时由 {@code MapManager} 载入时报错。
 */
public final class RegionCatalog {

    private record Manifest(List<RegionInfo> regions) {}

    private final List<RegionInfo> regions;
    private final Map<Integer, RegionInfo> byFloor = new HashMap<>();
    private final Map<String, Integer> indexById = new HashMap<>();

    public RegionCatalog(List<RegionInfo> regions) {
        if (regions == null || regions.isEmpty()) {
            throw new IllegalArgumentException("区域清单为空");
        }
        this.regions = List.copyOf(regions);
        for (int i = 0; i < this.regions.size(); i++) {
            RegionInfo region = this.regions.get(i);
            if (indexById.put(region.regionId(), i) != null) {
                throw new IllegalArgumentException("区域 id 重复：" + region.regionId());
            }
            for (int floor : region.floors()) {
                RegionInfo previous = byFloor.put(floor, region);
                if (previous != null) {
                    throw new IllegalArgumentException("第 " + floor + " 层同时属于区域 "
                            + previous.regionId() + " 与 " + region.regionId());
                }
            }
        }
    }

    private static class Holder {
        private static final RegionCatalog INSTANCE = load();
    }

    /** 当前塔的区域清单（首次访问时读盘，之后常驻）；由 {@code GameBootstrap} 在加载阶段提前触发 */
    public static RegionCatalog get() {
        return Holder.INSTANCE;
    }

    private static RegionCatalog load() {
        Manifest manifest = ResourceManager.getInstance()
                .loadJsonResource(REGION_MANIFEST_PATH, new TypeReference<>() {});
        return new RegionCatalog(manifest == null ? null : manifest.regions());
    }

    public List<RegionInfo> all() {
        return regions;
    }

    /** 楼层所属的区域；不属于任何区域时抛异常（清单漏写，属于打包错误） */
    public RegionInfo regionOf(int floor) {
        RegionInfo region = byFloor.get(floor);
        if (region == null) {
            throw new IllegalStateException("第 " + floor + " 层不属于任何区域，请检查 " + REGION_MANIFEST_PATH);
        }
        return region;
    }

    public RegionInfo byId(String regionId) {
        Integer index = indexById.get(regionId);
        if (index == null) {
            throw new IllegalArgumentException("未知区域：" + regionId);
        }
        return regions.get(index);
    }

    /** 区域在推进顺序中的下标 */
    public int indexOf(String regionId) {
        Integer index = indexById.get(regionId);
        if (index == null) {
            throw new IllegalArgumentException("未知区域：" + regionId);
        }
        return index;
    }

    /** 楼层 {@code floor} 相对区域 {@code current} 的位置 */
    public RegionRelation relation(RegionInfo current, int floor) {
        int target = indexOf(regionOf(floor).regionId());
        int here = indexOf(current.regionId());
        if (target == here) return RegionRelation.SAME;
        return target > here ? RegionRelation.FORWARD : RegionRelation.BACKWARD;
    }
}
