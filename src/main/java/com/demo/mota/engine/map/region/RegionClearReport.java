package com.demo.mota.engine.map.region;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * 区域结束检查的结果：本区域里还剩多少怪物没杀、多少能力宝石没拾取（标了 {@code clearExempt} 的不计）。
 * 只按楼层计数，将来的检查详情界面可据此列出「哪一层还剩什么」。
 *
 * @param regionId          区域 id
 * @param remainingMonsters 楼层 → 剩余怪物数（只含数量 > 0 的楼层）
 * @param remainingGems     楼层 → 剩余能力宝石数（只含数量 > 0 的楼层）
 */
public record RegionClearReport(String regionId,
                                Map<Integer, Integer> remainingMonsters,
                                Map<Integer, Integer> remainingGems) {

    public RegionClearReport {
        remainingMonsters = withoutZeros(remainingMonsters);
        remainingGems = withoutZeros(remainingGems);
    }

    public boolean isClear() {
        return remainingMonsters.isEmpty() && remainingGems.isEmpty();
    }

    public int totalMonsters() {
        return remainingMonsters.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int totalGems() {
        return remainingGems.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static Map<Integer, Integer> withoutZeros(Map<Integer, Integer> counts) {
        Map<Integer, Integer> result = new TreeMap<>();
        if (counts != null) {
            counts.forEach((floor, count) -> {
                if (count != null && count > 0) result.put(floor, count);
            });
        }
        return Collections.unmodifiableMap(result);
    }
}
