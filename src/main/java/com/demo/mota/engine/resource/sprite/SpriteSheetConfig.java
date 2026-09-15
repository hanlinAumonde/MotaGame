package com.demo.mota.engine.resource.sprite;

import java.util.Map;

/**
 * 一个精灵图拆分配置文件的模型。
 *
 * <p>配置分两段：
 * <ul>
 *   <li><b>公共段</b>——{@code type}（用哪种切法）、{@code image}（切哪张图）、
 *       网格几何（{@code cellWidth/cellHeight} 或 {@code columns/rows}、
 *       {@code originX/originY}、{@code spacingX/spacingY}），所有切法都认；</li>
 *   <li><b>切法专属段</b>——原样包进 {@link SheetParams} 交给对应的 {@link SheetSlicer} 解释。</li>
 * </ul>
 *
 * <p>格子尺寸允许不写而由 {@code columns}/{@code rows} 反推：
 * 行走图这类素材的单帧高度未必是整数倍的常见值（{@code 011-Braver01.png} 为 32×33），
 * 写「几行几列」比写「每格多大」更不容易出错。
 */
public record SpriteSheetConfig(String sourcePath,
                                String type,
                                String imagePath,
                                int cellWidth,
                                int cellHeight,
                                int columns,
                                int rows,
                                int originX,
                                int originY,
                                int spacingX,
                                int spacingY,
                                SheetParams params) {

    public static SpriteSheetConfig from(String sourcePath, Map<String, Object> raw) {
        SheetParams params = new SheetParams(raw);
        String type = params.getString("type", null);
        String imagePath = params.getString("image", null);
        if (type == null) {
            throw new IllegalArgumentException("Sprite sheet config missing \"type\": " + sourcePath);
        }
        if (imagePath == null) {
            throw new IllegalArgumentException("Sprite sheet config missing \"image\": " + sourcePath);
        }
        return new SpriteSheetConfig(
                sourcePath, type, imagePath,
                params.getInt("cellWidth", 0), params.getInt("cellHeight", 0),
                params.getInt("columns", 0), params.getInt("rows", 0),
                params.getInt("originX", 0), params.getInt("originY", 0),
                params.getInt("spacingX", 0), params.getInt("spacingY", 0),
                params);
    }

    /** 格宽：显式配置优先，否则由图宽与 {@code columns} 反推 */
    public int resolveCellWidth(double imageWidth) {
        return resolveCellSize(cellWidth, columns, imageWidth, originX, spacingX, "cellWidth", "columns");
    }

    /** 格高：显式配置优先，否则由图高与 {@code rows} 反推 */
    public int resolveCellHeight(double imageHeight) {
        return resolveCellSize(cellHeight, rows, imageHeight, originY, spacingY, "cellHeight", "rows");
    }

    private int resolveCellSize(int explicitSize, int count, double imageSize,
                                int origin, int spacing, String sizeKey, String countKey) {
        if (explicitSize > 0) {
            return explicitSize;
        }
        if (count > 0) {
            return ((int) imageSize - origin + spacing) / count - spacing;
        }
        throw new IllegalArgumentException(
                "Sprite sheet config must provide \"" + sizeKey + "\" or \"" + countKey + "\": " + sourcePath);
    }
}
