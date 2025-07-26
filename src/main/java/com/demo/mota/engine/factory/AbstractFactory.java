package com.demo.mota.engine.factory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new RuntimeException("Config file not found: " + fileName);
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
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
