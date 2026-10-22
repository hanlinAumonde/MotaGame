package com.demo.mota.app.services;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.resource.ResourceManager;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 精灵目录（{@code assets/json/sprites.json}）：画面只引用逻辑精灵 id，
 * 「它在哪张图的哪一格、放大几倍、角色帧怎么排」都写在这里，由 {@link AssetService} 按它载图、放大、切帧。
 *
 * <pre>{@code
 * scale       源图放大倍数（最近邻）：32px 的素材 × 2 = 64px 的格子
 * sheets      图集：image（相对 assets/textures/）+ 网格，cell [宽, 高] 或 grid [列, 行]（由图尺寸反推格子大小）
 * sprites     静态精灵：sheet + cell [列, 行]
 * characters  行走图：sheet + rows（朝向 → 行）+ idle（静止帧所在列）+ walk（行走循环用到的列，按播放顺序）
 * }</pre>
 *
 * <p>数据表（道具 / 怪物 / 技能）的 {@code sprite} 字段既可以写这里登记的 id，也可以直接写图片路径
 * （{@link #isImagePath}），后者视为一张不切分的静态图，不必登记。
 *
 * <p>本类是纯数据，不依赖 JavaFX：网格计算（{@link Geometry}）由加载与单元测试共用。
 */
public record SpriteCatalog(int scale,
                            Map<String, SheetDef> sheets,
                            Map<String, SpriteDef> sprites,
                            Map<String, CharacterDef> characters) {

    public static final String PATH = "/assets/json/sprites.json";

    public SpriteCatalog {
        scale = Math.max(1, scale);
        sheets = sheets == null ? Map.of() : Map.copyOf(sheets);
        sprites = sprites == null ? Map.of() : Map.copyOf(sprites);
        characters = characters == null ? Map.of() : Map.copyOf(characters);
    }

    /** 读取精灵目录；文件缺失或格式错误直接抛异常（属于打包错误） */
    public static SpriteCatalog load(ResourceManager resources) {
        return resources.loadJsonResource(PATH, new TypeReference<>() {});
    }

    /** 精灵引用是否是一张图片的路径（相对 {@code assets/textures/}），而不是目录里登记的 id */
    public static boolean isImagePath(String ref) {
        String lower = ref.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".bmp");
    }

    /** 一对整数，JSON 里写成两元素数组：{@code [宽, 高]}、{@code [列, 行]} */
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"x", "y"})
    public record Pair(int x, int y) {
    }

    /**
     * 图集。{@code cell} 与 {@code grid} 二选一：行走图这类素材的单帧尺寸未必是整数
     * （{@code 011-Braver01.png} 为 128×132 的 4×4，单帧 32×33），写「几列几行」比写「每格多大」不容易错。
     */
    public record SheetDef(String image, Pair cell, Pair grid) {

        public Geometry geometry(int imageWidth, int imageHeight) {
            int cellWidth;
            int cellHeight;
            if (cell != null) {
                cellWidth = cell.x();
                cellHeight = cell.y();
            } else if (grid != null && grid.x() > 0 && grid.y() > 0) {
                cellWidth = imageWidth / grid.x();
                cellHeight = imageHeight / grid.y();
            } else {
                throw new IllegalArgumentException("图集 " + image + " 必须写 cell 或 grid");
            }
            if (cellWidth <= 0 || cellHeight <= 0) {
                throw new IllegalArgumentException("图集 " + image + " 的格子尺寸无效：" + cellWidth + "x" + cellHeight);
            }
            return new Geometry(cellWidth, cellHeight, imageWidth / cellWidth, imageHeight / cellHeight);
        }
    }

    /** 一张图集按网格切开后的几何：格子尺寸与行列数 */
    public record Geometry(int cellWidth, int cellHeight, int columns, int rows) {

        public boolean contains(int col, int row) {
            return col >= 0 && row >= 0 && col < columns && row < rows;
        }

        public int x(int col) {
            return col * cellWidth;
        }

        public int y(int row) {
            return row * cellHeight;
        }
    }

    /** 静态精灵：图集里的一格 */
    public record SpriteDef(String sheet, Pair cell) {
    }

    /**
     * 行走图：一行是一个朝向；{@code idle} 是静止帧所在的列，{@code walk} 是行走循环依次用到的列。
     * XP 式 4 帧行走图写 {@code walk: [1, 2, 3, 0]}——每步播两帧，左右脚交替、每步落回站立帧。
     */
    public record CharacterDef(String sheet, Map<Direction, Integer> rows, int idle, List<Integer> walk) {

        public CharacterDef {
            rows = rows == null ? Map.of() : Map.copyOf(rows);
            walk = walk == null ? List.of() : List.copyOf(walk);
        }
    }
}
