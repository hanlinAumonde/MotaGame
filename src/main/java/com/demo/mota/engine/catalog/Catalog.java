package com.demo.mota.engine.catalog;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * 一张按 id 索引的数据表（不可变）。道具 / 怪物 / 技能目录共用的部分：
 * 按 id 查定义、id 重复即报错、取条目引用的精灵。
 *
 * <p>目录只持有<b>定义</b>（纯数据），由子类按定义造出可变的游戏对象；它不读文件，
 * 读文件、解析 JSON 是内容层（{@code com.demo.mota.content}）的事。
 *
 * @param <D> 条目定义
 */
public abstract class Catalog<D> {

    private final String kind;
    private final Map<String, D> definitions = new LinkedHashMap<>();

    protected Catalog(String kind, Collection<D> definitions, Function<D, String> idOf) {
        this.kind = kind;
        for (D definition : definitions) {
            String id = Objects.requireNonNull(idOf.apply(definition), kind + "缺少 id");
            if (this.definitions.put(id, definition) != null) {
                throw new IllegalArgumentException(kind + " id 重复：" + id);
            }
        }
    }

    public boolean contains(String id) {
        return definitions.containsKey(id);
    }

    /** 全部 id，按表里的顺序 */
    public Set<String> ids() {
        return definitions.keySet();
    }

    /** 按 id 取定义；不存在时为 {@code null} */
    public D find(String id) {
        return definitions.get(id);
    }

    /** 按 id 取定义；不存在时抛 {@link IllegalArgumentException} */
    public D require(String id) {
        D definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("找不到" + kind + "：" + id);
        }
        return definition;
    }

    /**
     * 条目引用的精灵：精灵目录（{@code assets/json/sprites.json}）里的 id，
     * 或 {@code assets/textures/} 下的图片路径；没有配图时为 null。
     * 只是数据——engine 不加载图片，表现层按它取图。
     */
    protected abstract String spriteOf(D definition);

    /** 按 id 取该条目引用的精灵；id 不存在或没有配图时为 null */
    public String getSprite(String id) {
        D definition = definitions.get(id);
        return definition == null ? null : spriteOf(definition);
    }

    /** 全部条目引用到的精灵（去重、去空），供表现层预载并校验 */
    public Set<String> referencedSprites() {
        Set<String> sprites = new TreeSet<>();
        definitions.values().stream().map(this::spriteOf)
                .filter(Objects::nonNull).filter(s -> !s.isBlank())
                .forEach(sprites::add);
        return sprites;
    }
}
