package com.demo.mota.engine.resource;

import com.demo.mota.engine.configs.GraphicsConfigConstants;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.resource.provider.ClasspathResourceProvider;
import com.demo.mota.engine.resource.provider.FileSystemResourceProvider;
import com.demo.mota.engine.resource.provider.ResourceProvider;
import com.demo.mota.engine.resource.sprite.CharacterSprites;
import com.demo.mota.engine.resource.sprite.SpriteSheetLoader;
import com.demo.mota.engine.resource.sprite.SpriteStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;

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
 * <p>
 * 图片资源统一位于资源根下的 {@code /Graphics/}，按用途分子目录（见
 * {@link GraphicsConfigConstants}）。需要切分的大图不在本类里硬编码，
 * 而是由 {@code resource.sprite} 模块按 {@code data/graphics/} 下的配置清单加载，
 * 本类只作为切分产物的缓存（实现 {@link SpriteStore}）。
 */
public class ResourceManager implements SpriteStore {
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
    private final Map<String, CharacterSprites> characterSpriteCache = new HashMap<>();

    private ResourceManager() {
        providers.addAll(pendingProviders);
        pendingProviders.clear();

        String externalDir = System.getProperty(EXTERNAL_DIR_PROPERTY);
        if (externalDir != null && !externalDir.isBlank()) {
            providers.add(new FileSystemResourceProvider(Path.of(externalDir)));
        }
        providers.add(new ClasspathResourceProvider());

        new SpriteSheetLoader(this).loadAll(GraphicsConfigConstants.SPRITE_SHEET_MANIFEST, this);
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

    // --- 精灵图切分产物（SpriteStore 实现） ---

    @Override
    public void putTile(String resourceId, Image image) {
        tileImageCache.put(resourceId, image);
    }

    @Override
    public void putCharacter(String characterId, CharacterSprites sprites) {
        characterSpriteCache.put(characterId, sprites);
    }

    public Image getTileImage(String resourceId) {
        return tileImageCache.get(resourceId);
    }

    // --- Character Sprites ---

    /**
     * 取某个角色的全部行走帧。当前只有主角，但缓存按 characterId 分开存放，
     * 将来加 NPC / 可切换的主角形象时只需多一个拆分配置文件。
     *
     * @return 该角色没有配置行走图时返回 {@code null}
     */
    public CharacterSprites getCharacterSprites(String characterId) {
        return characterSpriteCache.get(characterId);
    }

    /** 取某角色某朝向的静止帧；缺图时返回 {@code null}，渲染侧走 fallback */
    public Image getCharacterSprite(String characterId, Direction direction) {
        CharacterSprites sprites = characterSpriteCache.get(characterId);
        return sprites == null ? null : sprites.idle(direction);
    }

    /** {@link #getCharacterSprite} 针对主角的便捷入口 */
    public Image getPlayerSprite(Direction direction) {
        return getCharacterSprite(GraphicsConfigConstants.PLAYER_CHARACTER_ID, direction);
    }

    // --- Item / Monster / Skill Images ---

    /**
     * 按文件名加载指定类别目录下的图片并缓存；文件名为空或图片缺失时不写入缓存，
     * 由渲染侧走 fallback。同一 id 已缓存时直接跳过。
     */
    private void registerImage(Map<String, Image> cache, String id, String directory, String imageFileName) {
        if (imageFileName == null || imageFileName.isEmpty()) return;
        if (cache.containsKey(id)) return;
        try (InputStream is = getOptionalResourceStream(directory + imageFileName)) {
            if (is != null) {
                cache.put(id, new Image(is));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image: " + directory + imageFileName, e);
        }
    }

    public void registerItemImage(String itemId, String imageFileName) {
        registerImage(itemImageCache, itemId, GraphicsConfigConstants.ITEM_IMAGE_DIR, imageFileName);
    }

    public Image getItemImage(String itemId) {
        return itemImageCache.get(itemId);
    }

    public void registerMonsterImage(String monsterId, String imageFileName) {
        registerImage(monsterImageCache, monsterId, GraphicsConfigConstants.MONSTER_IMAGE_DIR, imageFileName);
    }

    public Image getMonsterImage(String monsterId) {
        return monsterImageCache.get(monsterId);
    }

    public void registerSkillImage(String skillId, String imageFileName) {
        registerImage(skillImageCache, skillId, GraphicsConfigConstants.SKILL_IMAGE_DIR, imageFileName);
    }

    public Image getSkillImage(String skillId) {
        return skillImageCache.get(skillId);
    }
}
