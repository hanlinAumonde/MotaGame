package com.demo.mota.content;

import com.demo.mota.content.provider.ClasspathResourceProvider;
import com.demo.mota.content.provider.FileSystemResourceProvider;
import com.demo.mota.content.provider.ResourceProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 资源来源（单例），全项目所有资源读取的唯一入口：数据表、楼层、精灵目录、图片都经它定位。
 * <p>
 * 读取策略基于 provider 链：按下标顺序依次尝试各 {@link ResourceProvider}，
 * 前面的找不到则尝试后面的。默认链为：
 * <ol>
 *   <li>启动前通过 {@link #registerProvider} 注册的自定义 provider（可选）</li>
 *   <li>系统属性 {@link #EXTERNAL_DIR_PROPERTY} 指定的项目外目录（可选，用于资源外置）</li>
 *   <li>{@link ClasspathResourceProvider}（兜底，保持原有 getResourceAsStream 行为）</li>
 * </ol>
 * <p>
 * 资源统一放在资源根下的 {@code /assets/}，目录遵循 FXGL 的约定（{@code textures/}、{@code json/}、
 * {@code levels/}、{@code ui/fonts/} ……）。外置目录镜像同样的结构。
 * <p>
 * 本类只负责「按路径找到资源」与 JSON 读取，<b>不认识图片</b>：图片经 {@link #findResourceUrl}
 * 定位后交给表现层（{@code AssetService}）用 FXGL 的资源加载器载入。
 * 属于内容层：engine 不读文件，由 {@code ContentLoader} 经本类把数据读成 {@code TowerContent} 交给引擎。
 */
public class ContentSource {
    /**
     * 系统属性：资源外置目录，目录结构镜像资源根目录。
     * 例如 -Dmota.resource.externalDir=D:/mota-resources
     */
    public static final String EXTERNAL_DIR_PROPERTY = "mota.resource.externalDir";

    private static volatile ContentSource instance;

    /** 实例创建前注册的 provider，创建时按注册顺序加入链首 */
    private static final List<ResourceProvider> pendingProviders = new ArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** provider 链，下标越小优先级越高 */
    private final List<ResourceProvider> providers = new ArrayList<>();

    private ContentSource() {
        providers.addAll(pendingProviders);
        pendingProviders.clear();

        String externalDir = System.getProperty(EXTERNAL_DIR_PROPERTY);
        if (externalDir != null && !externalDir.isBlank()) {
            providers.add(new FileSystemResourceProvider(Path.of(externalDir)));
        }
        providers.add(new ClasspathResourceProvider());
    }

    public static ContentSource getInstance() {
        if (instance == null) {
            synchronized (ContentSource.class) {
                if (instance == null) {
                    instance = new ContentSource();
                }
            }
        }
        return instance;
    }

    /**
     * 注册自定义资源提供者（线程安全）。
     * 首次 getInstance() 之前调用时参与所有资源读取；
     * 之后调用则追加到链尾（仅影响后续读取，已缓存的资源不受影响）。
     */
    public static void registerProvider(ResourceProvider provider) {
        synchronized (ContentSource.class) {
            if (instance == null) {
                pendingProviders.add(provider);
            } else {
                instance.providers.add(provider);
            }
        }
    }

    // --- 统一 I/O ---

    /**
     * 依次尝试所有 provider 打开资源流，全部找不到时抛异常。
     * 适用于缺失即视为错误的资源（JSON 配置等）。
     */
    public InputStream getResourceStream(String resourcePath) {
        InputStream is = getOptionalResourceStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }
        return is;
    }

    /**
     * 依次尝试所有 provider 打开资源流，找不到时返回 null。
     * 适用于可选资源。
     */
    public InputStream getOptionalResourceStream(String resourcePath) {
        for (ResourceProvider provider : providers) {
            try {
                InputStream is = provider.openStream(resourcePath);
                if (is != null) return is;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read resource: " + resourcePath, e);
            }
        }
        return null;
    }

    /**
     * 依次尝试所有 provider 定位资源，返回第一个找到的 URL；全部找不到时为空。
     * 供需要 URL 的加载器使用（FXGL 的 {@code loadImage(URL)} 等），外置目录因此同样能覆盖图片。
     */
    public Optional<URL> findResourceUrl(String resourcePath) {
        for (ResourceProvider provider : providers) {
            URL url = provider.getResourceUrl(resourcePath);
            if (url != null) return Optional.of(url);
        }
        return Optional.empty();
    }

    public <T> T loadJsonResource(String resourcePath, TypeReference<T> typeRef) {
        try (InputStream is = getResourceStream(resourcePath)) {
            return objectMapper.readValue(is, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + resourcePath, e);
        }
    }
}
