package com.demo.mota.ui.screen.game.side;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分区 id → {@link SidePanelSection} 的注册表，与 {@code SkillEffectRegistry} / {@code SheetSlicerRegistry} 同构。
 * 内置分区在构造时由 {@link BuiltinSidePanelSections} 注册。未注册的 id 在右侧栏里直接跳过。
 */
public final class SidePanelSectionRegistry {

    private static class Holder {
        private static final SidePanelSectionRegistry INSTANCE = new SidePanelSectionRegistry();
    }

    public static SidePanelSectionRegistry getInstance() {
        return Holder.INSTANCE;
    }

    private final Map<String, SidePanelSection> sections = new ConcurrentHashMap<>();

    private SidePanelSectionRegistry() {
        BuiltinSidePanelSections.registerAll(this::register);
    }

    public void register(String id, SidePanelSection section) {
        if (id == null || id.isBlank() || section == null) {
            throw new IllegalArgumentException("Invalid side panel section registration: " + id);
        }
        sections.put(id.trim(), section);
    }

    public void unregister(String id) {
        sections.remove(id);
    }

    /** @return 对应分区，未注册时为 null */
    public SidePanelSection get(String id) {
        return id == null ? null : sections.get(id.trim());
    }

    public Set<String> registeredIds() {
        return Set.copyOf(sections.keySet());
    }
}
