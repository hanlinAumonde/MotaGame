package com.demo.mota.engine.resource.sprite;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 拆分配置中「与切分方式相关」那部分字段的只读、带类型的视图。
 *
 * <p>与 {@code SkillParams} 是同一套思路：公共字段（图片路径、网格几何）由
 * {@link SpriteSheetConfig} 固定承载，剩下的字段各个 {@link SheetSlicer} 自己解释——
 * 这样<b>新增一种切法只要写一个 slicer，不必改动配置模型</b>。
 *
 * <p>取值一律带默认值：缺项、类型不符、解析失败都回退到默认值而不抛异常，
 * 一条写错的拆分配置不应该让游戏启动不了（对应的图缺失时渲染侧本来就有 fallback）。
 */
public final class SheetParams {

    public static final SheetParams EMPTY = new SheetParams(Map.of());

    private final Map<String, Object> values;

    public SheetParams(Map<String, Object> values) {
        this.values = values == null || values.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Map<String, Object> asMap() {
        return values;
    }

    public boolean contains(String key) {
        return values.get(key) != null;
    }

    public int getInt(String key, int defaultValue) {
        Object raw = values.get(key);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object raw = values.get(key);
        return raw instanceof String text && !text.isBlank() ? text.trim() : defaultValue;
    }

    /** 取一个「键 → 整数」的对象，例如 {@code directionRows}；缺项返回空表 */
    public Map<String, Integer> getIntMap(String key) {
        Object raw = values.get(key);
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String name && entry.getValue() instanceof Number number) {
                result.put(name, number.intValue());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** 取一个整数数组，例如 {@code walkColumns}；缺项或元素类型不符时返回默认值 */
    public List<Integer> getIntList(String key, List<Integer> defaultValue) {
        Object raw = values.get(key);
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return defaultValue;
        }
        List<Integer> result = new java.util.ArrayList<>(list.size());
        for (Object element : list) {
            if (element instanceof Number number) {
                result.add(number.intValue());
            }
        }
        return result.isEmpty() ? defaultValue : Collections.unmodifiableList(result);
    }

    /**
     * 取一个「键 → 子对象」的对象，例如 {@code tiles}，每个子对象再包成 {@code SheetParams}，
     * 于是 {@code tiles.get("bg_default").getInt("col", 0)} 这样层层取值仍然享受同一套容错规则。
     */
    @SuppressWarnings("unchecked")
    public Map<String, SheetParams> getObjectMap(String key) {
        Object raw = values.get(key);
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, SheetParams> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String name && entry.getValue() instanceof Map<?, ?> child) {
                result.put(name, new SheetParams((Map<String, Object>) child));
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
