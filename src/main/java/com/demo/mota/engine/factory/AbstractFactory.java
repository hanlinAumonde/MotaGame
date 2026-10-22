package com.demo.mota.engine.factory;

import com.demo.mota.engine.resource.ResourceManager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractFactory<T, D, C> {
    protected final Map<String, D> dataRegistry = new HashMap<>();
    protected final Function<String, C> creator;

    protected AbstractFactory() {
        loadData();
        this.creator = this::generateCreator;
    }

    /**
     * 获取配置文件名称
     */
    protected abstract String getConfigFileName();

    protected void loadData() {
        String fileName = getConfigFileName();
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = ResourceManager.getInstance().getResourceStream(fileName)) {
            parseData(mapper, inputStream);
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to load data", e);
        }
    }

    /**
     * 解析配置数据并注册到dataRegistry
     */
    protected abstract void parseData(ObjectMapper mapper, InputStream inputStream) throws IOException;

    /**
     * 根据数据生成创建器
     */
    protected abstract C generateCreator(String id);

    /**
     * 使用创建器创建产品实例
     */
    protected abstract T createProduct(C creator, D data);

    /**
     * 配置条目引用的精灵：精灵目录（{@code assets/json/sprites.json}）里的 id，
     * 或 {@code assets/textures/} 下的图片路径；没有配图时为 null。
     * <p>只是数据——engine 不加载图片，表现层（{@code AssetService}）按它取图。
     */
    protected abstract String spriteOf(D data);

    /** 按 id 取该条目引用的精灵；id 不存在或没有配图时为 null */
    public String getSprite(String id) {
        D data = dataRegistry.get(id);
        return data == null ? null : spriteOf(data);
    }

    /** 全部条目引用到的精灵（去重、去空），供表现层启动时预载并校验 */
    public Set<String> referencedSprites() {
        Set<String> sprites = new TreeSet<>();
        dataRegistry.values().stream().map(this::spriteOf)
                .filter(Objects::nonNull).filter(s -> !s.isBlank())
                .forEach(sprites::add);
        return sprites;
    }

    /**
     * 根据ID创建产品
     */
    public T createById(String id) {
        D data = dataRegistry.get(id);
        if (data == null) {
            throw new IllegalArgumentException("Data not found for id: " + id);
        }
        C productCreator = creator.apply(id);
        return createProduct(productCreator, data);
    }
}
