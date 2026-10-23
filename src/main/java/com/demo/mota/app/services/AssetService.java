package com.demo.mota.app.services;

import com.almasb.fxgl.app.services.FXGLAssetLoaderService;
import com.almasb.fxgl.core.EngineService;
import com.almasb.fxgl.logging.Logger;
import com.almasb.fxgl.texture.ImagesKt;
import com.demo.mota.app.services.SpriteCatalog.CharacterDef;
import com.demo.mota.app.services.SpriteCatalog.Geometry;
import com.demo.mota.app.services.SpriteCatalog.SheetDef;
import com.demo.mota.content.ContentSource;
import com.demo.mota.content.LoadingTask;
import com.demo.mota.engine.catalog.TowerContent;
import com.demo.mota.engine.enums.Direction;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片资源服务（FXGL {@link EngineService}）：画面用到的图片都从这里取。
 *
 * <ul>
 *   <li>{@link #onInit()}（FXGL 启动期、后台线程）按精灵目录（{@link SpriteCatalog}）载入图集，
 *       切出各精灵与角色帧并<b>最近邻放大</b>（{@code ImagesKt.resize}），像素图放大后依旧锐利；</li>
 *   <li>随后按塔的内容（{@link ContentService}，排在本服务之前初始化）把道具 / 怪物 / 技能表引用到的图片预载一遍，
 *       之后绘制时取图都直接命中缓存；进度汇入 {@link StartupProgress}；</li>
 *   <li>定位走内容层的 provider 链（{@link ContentSource#findResourceUrl}，外置目录能覆盖内置图片），
 *       载入走 FXGL 的资源加载器（带缓存）。</li>
 * </ul>
 *
 * <p>缺图一律返回 {@code null} 并告警一次，由绘制方画按语义着色的占位——FXGL 的加载器缺图时会返回
 * 一张通用占位图，所以这里先查 URL，不让它走到那一步。
 */
public final class AssetService extends EngineService {

    /** 主角在精灵目录 {@code characters} 里的 id */
    public static final String PLAYER = "player";

    private static final Logger log = Logger.get(AssetService.class);
    private static final String TEXTURES = "/assets/textures/";

    /** 由 FXGL 按字段类型注入（Engine.injectDependenciesIntoService） */
    private FXGLAssetLoaderService assetLoader;
    private ContentService contentService;

    private final ContentSource resources = ContentSource.getInstance();
    private int scale = 1;
    /** 塔的内容：按道具 / 怪物 id 查它们引用的精灵；启动期预载之前为 null */
    private volatile TowerContent content;

    private final List<LoadingTask> tasks = List.of(
            new LoadingTask("载入精灵目录", 5, this::loadCatalog),
            new LoadingTask("载入图像资源", 20, () -> preloadContentSprites(contentService.content())));

    public AssetService() {
        StartupProgress.get().reserve(tasks);
    }

    private final Map<String, Image> sprites = new ConcurrentHashMap<>();
    private final Map<String, CharacterSprite> characters = new ConcurrentHashMap<>();
    /** 直接写图片路径的精灵（已放大），以及查不到的 id：载不到的记为空，免得每次绘制都告警 */
    private final Map<String, Optional<Image>> looseSprites = new ConcurrentHashMap<>();

    @Override
    public void onInit() {
        StartupProgress.get().run(tasks);
    }

    private void loadCatalog() {
        SpriteCatalog catalog = SpriteCatalog.load(resources);
        scale = catalog.scale();

        Map<String, LoadedSheet> sheets = new HashMap<>();
        catalog.sheets().forEach((id, def) ->
                loadImage(def.image()).ifPresent(image -> sheets.put(id, new LoadedSheet(def, image))));

        catalog.sprites().forEach((id, def) -> {
            Image image = cut(sheets.get(def.sheet()), def.sheet(), def.cell().x(), def.cell().y(), "精灵 " + id);
            if (image != null) {
                sprites.put(id, image);
            }
        });
        catalog.characters().forEach((id, def) -> {
            CharacterSprite character = cutCharacter(id, def, sheets.get(def.sheet()));
            if (character != null) {
                characters.put(id, character);
            }
        });
        log.infof("精灵目录：%d 张图集、%d 个精灵、%d 个角色", sheets.size(), sprites.size(), characters.size());
    }

    // ==================== 取图 ====================

    /**
     * 按精灵引用取图（已放大）：精灵目录里登记的 id，或 {@code assets/textures/} 下的图片路径。
     *
     * @return 引用为空、查不到或载入失败时返回 {@code null}
     */
    public Image sprite(String ref) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        Image image = sprites.get(ref);
        if (image != null) {
            return image;
        }
        return looseSprites.computeIfAbsent(ref, this::loadLooseSprite).orElse(null);
    }

    /** 道具表里该道具的图 */
    public Image itemImage(String itemId) {
        TowerContent tower = content;
        return tower == null ? null : sprite(tower.items().getSprite(itemId));
    }

    /** 怪物表里该怪物的图 */
    public Image monsterImage(String monsterId) {
        TowerContent tower = content;
        return tower == null ? null : sprite(tower.monsters().getSprite(monsterId));
    }

    /** 角色行走图；精灵目录里没有该角色、或图集载入失败时返回 {@code null} */
    public CharacterSprite character(String characterId) {
        return characters.get(characterId);
    }

    /** 角色某朝向的静止帧；缺图时返回 {@code null} */
    public Image idleFrame(String characterId, Direction direction) {
        CharacterSprite character = characters.get(characterId);
        return character == null ? null : character.idle(direction);
    }

    /**
     * 记下塔的内容，并把道具 / 怪物 / 技能表引用到的图片全部载入一遍、对缺失的告警。
     * 启动期执行（后台线程），之后绘制时取图都直接命中缓存。
     */
    private void preloadContentSprites(TowerContent content) {
        this.content = content;
        Set<String> refs = new TreeSet<>();
        refs.addAll(content.skills().referencedSprites());
        refs.addAll(content.items().referencedSprites());
        refs.addAll(content.monsters().referencedSprites());
        refs.forEach(this::sprite);
        log.infof("预载内容表图片 %d 张", refs.size());
    }

    // ==================== 载入与切分 ====================

    private Optional<Image> loadLooseSprite(String ref) {
        if (!SpriteCatalog.isImagePath(ref)) {
            log.warning("精灵目录里没有 \"" + ref + "\"");
            return Optional.empty();
        }
        return loadImage(ref).map(this::scaled);
    }

    /** 定位并载入 {@code assets/textures/} 下的一张原图（未放大） */
    private Optional<Image> loadImage(String path) {
        Optional<URL> url = resources.findResourceUrl(TEXTURES + path);
        if (url.isEmpty()) {
            log.warning("图片不存在：" + TEXTURES + path);
            return Optional.empty();
        }
        Image image = assetLoader.loadImage(url.get());
        if (image == ImagesKt.getDummyImage()) {
            // FXGL 载入失败时返回通用占位图（并已告警），这里当作缺图处理
            return Optional.empty();
        }
        return Optional.of(image);
    }

    /** 从图集切出一格并放大；图集缺失或越界时告警并返回 {@code null} */
    private Image cut(LoadedSheet sheet, String sheetId, int col, int row, String what) {
        if (sheet == null) {
            log.warning(what + " 引用的图集 \"" + sheetId + "\" 不存在或载入失败");
            return null;
        }
        Geometry geometry = sheet.geometry();
        if (!geometry.contains(col, row)) {
            log.warning(what + " 的格子 (" + col + ", " + row + ") 越界（" + sheet.def().image() + " 共 "
                    + geometry.columns() + "×" + geometry.rows() + " 格）");
            return null;
        }
        Rectangle2D area = new Rectangle2D(geometry.x(col), geometry.y(row),
                geometry.cellWidth(), geometry.cellHeight());
        return scaled(ImagesKt.subImage(sheet.image(), area));
    }

    private CharacterSprite cutCharacter(String id, CharacterDef def, LoadedSheet sheet) {
        Map<Direction, Image> idle = new EnumMap<>(Direction.class);
        Map<Direction, List<Image>> walk = new EnumMap<>(Direction.class);
        def.rows().forEach((direction, row) -> {
            String what = "角色 " + id + " 的 " + direction;
            Image idleFrame = cut(sheet, def.sheet(), def.idle(), row, what);
            if (idleFrame == null) {
                return;
            }
            idle.put(direction, idleFrame);
            List<Image> frames = new ArrayList<>();
            for (int col : def.walk()) {
                Image frame = cut(sheet, def.sheet(), col, row, what);
                if (frame != null) {
                    frames.add(frame);
                }
            }
            walk.put(direction, frames);
        });
        if (idle.isEmpty()) {
            log.warning("角色 " + id + " 没有切出任何帧");
            return null;
        }
        return new CharacterSprite(idle, walk);
    }

    /** 最近邻放大到目录规定的倍数 */
    private Image scaled(Image image) {
        if (scale == 1) {
            return image;
        }
        return ImagesKt.resize(image, (int) image.getWidth() * scale, (int) image.getHeight() * scale);
    }

    /** 载入好的图集：原图 + 按它的尺寸算出的网格 */
    private record LoadedSheet(SheetDef def, Image image) {
        Geometry geometry() {
            return def.geometry((int) image.getWidth(), (int) image.getHeight());
        }
    }
}
