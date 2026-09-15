package com.demo.mota.engine.resource.sprite;

import com.demo.mota.engine.resource.ResourceManager;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 精灵图拆分的编排者：读清单 → 逐个读配置 → 载图 → 按 {@code type} 找 slicer → 切。
 *
 * <p>之所以用<b>清单文件</b>而不是扫描配置目录：类路径下的目录遍历在打成 jar 后
 * 需要额外挂载 jar FileSystem，而 {@code ResourceProvider} 只有「按路径取流」的能力，
 * 为此加一个 {@code list()} 会把资源抽象搞脏。清单既显式、加载顺序也确定。
 *
 * <p>失败策略与改造前保持一致：
 * <ul>
 *   <li>清单 / 配置文件缺失或格式错误 → <b>抛异常</b>（属于打包错误，早发现早好）；</li>
 *   <li>图片缺失、{@code type} 未注册、个别格子越界 → <b>告警并跳过</b>，
 *       对应位置的渲染自然走纯色 fallback，不至于让整局游戏开不了。</li>
 * </ul>
 */
public final class SpriteSheetLoader {

    private static final String KEY_SHEETS = "spriteSheets";

    private final ResourceManager resourceManager;

    public SpriteSheetLoader(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /** 按清单加载全部精灵图 */
    public void loadAll(String manifestPath, SpriteStore store) {
        for (String configPath : readManifest(manifestPath)) {
            load(configPath, store);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readManifest(String manifestPath) {
        Map<String, Object> manifest =
                resourceManager.loadJsonResource(manifestPath, new TypeReference<>() {});
        Object sheets = manifest.get(KEY_SHEETS);
        if (!(sheets instanceof List<?> list)) {
            throw new IllegalArgumentException(
                    "Sprite sheet manifest missing \"" + KEY_SHEETS + "\" array: " + manifestPath);
        }
        return (List<String>) list;
    }

    /** 加载单个拆分配置 */
    public void load(String configPath, SpriteStore store) {
        Map<String, Object> raw =
                resourceManager.loadJsonResource(configPath, new TypeReference<>() {});
        SpriteSheetConfig config = SpriteSheetConfig.from(configPath, raw);

        SheetSlicer slicer = SheetSlicerRegistry.getInstance().get(config.type());
        if (slicer == null) {
            System.err.println("[SpriteSheet] 未注册的切分类型 \"" + config.type() + "\"，已跳过："
                    + configPath + "（已注册：" + SheetSlicerRegistry.getInstance().registeredTypes() + "）");
            return;
        }

        Image image = readImage(config.imagePath());
        if (image == null) {
            System.err.println("[SpriteSheet] 图片不存在，已跳过：" + config.imagePath()
                    + "（来自 " + configPath + "）");
            return;
        }

        SpriteSheet sheet = new SpriteSheet(
                image,
                config.resolveCellWidth(image.getWidth()),
                config.resolveCellHeight(image.getHeight()),
                config.originX(), config.originY(),
                config.spacingX(), config.spacingY());
        slicer.slice(sheet, config, store);
    }

    private Image readImage(String imagePath) {
        try (InputStream is = resourceManager.getOptionalResourceStream(imagePath)) {
            return is == null ? null : new Image(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sprite sheet image: " + imagePath, e);
        }
    }
}
