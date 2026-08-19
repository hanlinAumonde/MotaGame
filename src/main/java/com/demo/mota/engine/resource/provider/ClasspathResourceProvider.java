package com.demo.mota.engine.resource.provider;

import java.io.InputStream;
import java.net.URL;

/**
 * 类路径资源提供者：从 classpath（项目 resources 目录 / jar 包内部）读取资源，
 * 即原有的 getClass().getResourceAsStream 行为。
 * <p>
 * ResourceManager 默认将其注册为 provider 链的最后一位，作为兜底，
 * 因此原有行为完全保留。
 */
public class ClasspathResourceProvider implements ResourceProvider {

    @Override
    public InputStream openStream(String resourcePath) {
        return ClasspathResourceProvider.class.getResourceAsStream(resourcePath);
    }

    @Override
    public URL getResourceUrl(String resourcePath) {
        return ClasspathResourceProvider.class.getResource(resourcePath);
    }
}
