package com.demo.mota.engine.resource;

import com.demo.mota.engine.resource.provider.ClasspathResourceProvider;
import com.demo.mota.engine.resource.provider.FileSystemResourceProvider;
import com.demo.mota.engine.resource.provider.ResourceProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局资源管理器（单例），全项目所有资源读取的唯一入口。
 * <p>
 * 读取策略基于 provider 链：按下标顺序依次尝试各 {@link ResourceProvider}，
 * 前面的找不到则尝试后面的。默认链为：
 * <ol>
 *   <li>启动前通过 {@link #registerProvider} 注册的自定义 provider（可选）</li>
 *   <li>系统属性 {@link #EXTERNAL_DIR_PROPERTY} 指定的项目外目录（可选，用于资源外置）</li>
 *   <li>{@link ClasspathResourceProvider}（兜底，保持原有 getResourceAsStream 行为）</li>
 * </ol>
 */
public class ResourceManager {
    /**
     * 系统属性：资源外置目录，目录结构镜像资源根目录。
     * 例如 -Dmota.resource.externalDir=D:/mota-resources
     */
    public static final String EXTERNAL_DIR_PROPERTY = "mota.resource.externalDir";

    private static volatile ResourceManager instance;

    /** 实例创建前注册的 provider，创建时按注册顺序加入链首 */
    private static final List<ResourceProvider> pendingProviders = new ArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** provider 链，下标越小优先级越高 */
    private final List<ResourceProvider> providers = new ArrayList<>();

    private final Map<String, Image> tileImageCache = new HashMap<>();
    private final Map<String, Image> itemImageCache = new HashMap<>();
    private final Map<String, Image> monsterImageCache = new HashMap<>();
    private final Map<String, Image> skillImageCache = new HashMap<>();
    private final Map<Integer, Image> playerSpriteCache = new HashMap<>();

    private ResourceManager() {
        providers.addAll(pendingProviders);
        pendingProviders.clear();

        String externalDir = System.getProperty(EXTERNAL_DIR_PROPERTY);
        if (externalDir != null && !externalDir.isBlank()) {
            providers.add(new FileSystemResourceProvider(Path.of(externalDir)));
        }
        providers.add(new ClasspathResourceProvider());

        loadSpriteSheetConfig();
        loadPlayerSprites();
    }

    public static ResourceManager getInstance() {
        if (instance == null) {
            synchronized (ResourceManager.class) {
                if (instance == null) {
                    instance = new ResourceManager();
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
        synchronized (ResourceManager.class) {
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
     * 适用于可选资源（图片缺失时渲染走纯色 fallback）。
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
     * 依次尝试所有 provider 返回资源 URL（供 FXMLLoader 等需要 URL 的组件使用），
     * 全部找不到时抛异常。
     */
    public URL getResourceUrl(String resourcePath) {
        for (ResourceProvider provider : providers) {
            URL url = provider.getResourceUrl(resourcePath);
            if (url != null) return url;
        }
        throw new RuntimeException("Resource not found: " + resourcePath);
    }

    public <T> T loadJsonResource(String resourcePath, TypeReference<T> typeRef) {
        try (InputStream is = getResourceStream(resourcePath)) {
            return objectMapper.readValue(is, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + resourcePath, e);
        }
    }

    // --- Sprite Sheet ---

    @SuppressWarnings("unchecked")
    private void loadSpriteSheetConfig() {
        try (InputStream configStream = getResourceStream("/data/spritesheet-config.json")) {
            Map<String, Object> config = objectMapper.readValue(
                    configStream, new TypeReference<>() {});

            String sheetPath = (String) config.get("spriteSheet");
            int tileWidth = (int) config.get("tileWidth");
            int tileHeight = (int) config.get("tileHeight");

            Image spriteSheet;
            try (InputStream sheetStream = getResourceStream(sheetPath)) {
                spriteSheet = new Image(sheetStream);
            }
            PixelReader reader = spriteSheet.getPixelReader();

            Map<String, Map<String, Integer>> tiles =
                    (Map<String, Map<String, Integer>>) config.get("tiles");

            for (Map.Entry<String, Map<String, Integer>> entry : tiles.entrySet()) {
                String resourceId = entry.getKey();
                int col = entry.getValue().get("col");
                int row = entry.getValue().get("row");
                WritableImage subImage = new WritableImage(
                        reader,
                        col * tileWidth, row * tileHeight,
                        tileWidth, tileHeight);
                tileImageCache.put(resourceId, subImage);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load spritesheet config", e);
        }
    }

    public Image getTileImage(String resourceId) {
        return tileImageCache.get(resourceId);
    }

    // --- Player Sprites ---

    private void loadPlayerSprites() {
        try (InputStream is = getOptionalResourceStream("/images/011-Braver01.png")) {
            if (is == null) return;
            Image playerSheet = new Image(is);
            PixelReader reader = playerSheet.getPixelReader();

            int frameW = 32;
            int frameH = (int) (playerSheet.getHeight() / 4);
            //int staticCol = 0;

            // Direction ordinals: UP=0, DOWN=1, LEFT=2, RIGHT=3
            // Sprite sheet rows: row0=DOWN, row1=LEFT, row2=RIGHT, row3=UP
            int[] dirToRow = {3, 0, 1, 2};
            for (int dirIdx = 0; dirIdx < 4; dirIdx++) {
                int row = dirToRow[dirIdx];
                WritableImage frame = new WritableImage(
                        reader,
                        0, row * frameH,
                        frameW, frameH);
                playerSpriteCache.put(dirIdx, frame);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load player sprites", e);
        }
    }

    public Image getPlayerSprite(int directionOrdinal) {
        return playerSpriteCache.get(directionOrdinal);
    }

    // --- Item / Monster / Skill Images ---

    /**
     * 按文件名加载 {@code /images/} 下的图片并缓存；文件名为空或图片缺失时不写入缓存，
     * 由渲染侧走 fallback。同一 id 已缓存时直接跳过。
     */
    private void registerImage(Map<String, Image> cache, String id, String imageFileName) {
        if (imageFileName == null || imageFileName.isEmpty()) return;
        if (cache.containsKey(id)) return;
        try (InputStream is = getOptionalResourceStream("/images/" + imageFileName)) {
            if (is != null) {
                cache.put(id, new Image(is));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image: " + imageFileName, e);
        }
    }

    public void registerItemImage(String itemId, String imageFileName) {
        registerImage(itemImageCache, itemId, imageFileName);
    }

    public Image getItemImage(String itemId) {
        return itemImageCache.get(itemId);
    }

    public void registerMonsterImage(String monsterId, String imageFileName) {
        registerImage(monsterImageCache, monsterId, imageFileName);
    }

    public Image getMonsterImage(String monsterId) {
        return monsterImageCache.get(monsterId);
    }

    public void registerSkillImage(String skillId, String imageFileName) {
        registerImage(skillImageCache, skillId, imageFileName);
    }

    public Image getSkillImage(String skillId) {
        return skillImageCache.get(skillId);
    }
}
