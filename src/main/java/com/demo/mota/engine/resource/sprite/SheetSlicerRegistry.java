package com.demo.mota.engine.resource.sprite;

import com.demo.mota.engine.resource.sprite.builtin.BuiltinSheetSlicers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置中 {@code type} → {@link SheetSlicer} 的注册表，精灵图拆分模块的扩展点。
 *
 * <p>与技能系统的 {@code SkillEffectRegistry} 同构，扩展方式也一致：
 * <ul>
 *   <li><b>多一张要切的图</b>——在 {@code data/graphics/} 下加一个配置文件并登记到
 *       清单里，Java 代码不动（同一种切法，只是参数不同）；</li>
 *   <li><b>多一种切法</b>——实现 {@link SheetSlicer} 并调
 *       {@link #register(String, SheetSlicer)} 注册，配置里引用新的 {@code type} 即可。</li>
 * </ul>
 *
 * <p>未注册的 type 由 {@link SpriteSheetLoader} 跳过并告警而不是抛异常：
 * 一张切不出来的图只会让对应位置走纯色 fallback，不应该让整局游戏启动不了。
 */
public final class SheetSlicerRegistry {

    private static class Holder {
        private static final SheetSlicerRegistry INSTANCE = new SheetSlicerRegistry();
    }

    public static SheetSlicerRegistry getInstance() {
        return Holder.INSTANCE;
    }

    private final Map<String, SheetSlicer> slicers = new ConcurrentHashMap<>();

    private SheetSlicerRegistry() {
        BuiltinSheetSlicers.registerAll(this::register);
    }

    /**
     * 注册（或覆盖）一种切法。
     *
     * @param type    拆分配置中 {@code type} 字段的取值
     * @param slicer  该切法的实现
     */
    public void register(String type, SheetSlicer slicer) {
        if (type == null || type.isBlank() || slicer == null) {
            throw new IllegalArgumentException("Invalid sheet slicer registration: " + type);
        }
        slicers.put(type.trim(), slicer);
    }

    public void unregister(String type) {
        slicers.remove(type);
    }

    public boolean isRegistered(String type) {
        return type != null && slicers.containsKey(type.trim());
    }

    public Set<String> registeredTypes() {
        return Set.copyOf(slicers.keySet());
    }

    /** @return 未注册时返回 {@code null} */
    public SheetSlicer get(String type) {
        return type == null ? null : slicers.get(type.trim());
    }
}
