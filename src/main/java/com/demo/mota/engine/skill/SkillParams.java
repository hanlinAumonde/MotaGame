package com.demo.mota.engine.skill;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 技能效果参数包：对 {@code skillList.json} 中 {@code params} 对象的只读、带类型的视图。
 *
 * <p>之所以用「弱类型键值 + 取值时转换」而不是给每个技能写一个配置类，
 * 是为了让<b>新增技能只改 JSON、不改 Java</b>：同一个效果实现（如
 * {@code stat-modifier}）配上不同参数就是一个新技能；只有出现全新的
 * 效果<i>机制</i>时才需要写新的 {@link com.demo.mota.engine.skill.effect.SkillEffectProvider}。
 * 这也是未来「用外部脚本扩展技能」时脚本侧唯一需要读懂的数据结构。
 *
 * <p>取值一律带默认值：配置缺项、类型不符或解析失败时回退到默认值而不是抛异常，
 * 避免一条写错的技能配置让整座塔打不开。
 */
public final class SkillParams {

    public static final SkillParams EMPTY = new SkillParams(Map.of());

    private final Map<String, Object> values;

    public SkillParams(Map<String, Object> values) {
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

    public double getDouble(String key, double defaultValue) {
        Object raw = values.get(key);
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
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

    public long getLong(String key, long defaultValue) {
        Object raw = values.get(key);
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object raw = values.get(key);
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object raw = values.get(key);
        if (raw == null) {
            return defaultValue;
        }
        String text = String.valueOf(raw).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    /** 读取枚举型参数，大小写不敏感；缺省 / 非法值回退到 {@code defaultValue} */
    public <E extends Enum<E>> E getEnum(Class<E> enumType, String key, E defaultValue) {
        String text = getString(key, null);
        if (text == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, text.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return defaultValue;
        }
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
