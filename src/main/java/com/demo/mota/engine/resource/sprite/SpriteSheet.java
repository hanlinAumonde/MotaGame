package com.demo.mota.engine.resource.sprite;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/**
 * 一张精灵图 + 它的网格几何，是本模块唯一真正执行「按格切子图」的地方。
 *
 * <p>无论是地图元件图（每格是一个命名元件）还是角色行走图（行=朝向、列=动画帧），
 * 落到底层都是同一个动作：<b>按规则网格取第 (col, row) 格</b>。
 * 差异只在于「谁来解释这个网格」，那部分交给各个 {@link SheetSlicer}。
 *
 * <p>几何模型支持 origin 偏移与格间距（spacing），以兼容带外边距 / 分隔线的图集；
 * 现有两张图都是 0，但参数留在这里可以让新素材直接配置，不必改代码。
 */
public final class SpriteSheet {

    private final Image image;
    private final PixelReader reader;

    private final int cellWidth;
    private final int cellHeight;
    private final int originX;
    private final int originY;
    private final int spacingX;
    private final int spacingY;

    private final int columns;
    private final int rows;

    public SpriteSheet(Image image, int cellWidth, int cellHeight,
                       int originX, int originY, int spacingX, int spacingY) {
        if (image == null) {
            throw new IllegalArgumentException("SpriteSheet image must not be null");
        }
        if (cellWidth <= 0 || cellHeight <= 0) {
            throw new IllegalArgumentException(
                    "Invalid cell size: " + cellWidth + "x" + cellHeight);
        }
        this.image = image;
        this.reader = image.getPixelReader();
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.originX = Math.max(0, originX);
        this.originY = Math.max(0, originY);
        this.spacingX = Math.max(0, spacingX);
        this.spacingY = Math.max(0, spacingY);
        this.columns = countCells((int) image.getWidth() - this.originX, this.cellWidth, this.spacingX);
        this.rows = countCells((int) image.getHeight() - this.originY, this.cellHeight, this.spacingY);
    }

    private static int countCells(int available, int cell, int spacing) {
        if (available < cell) return 0;
        return (available + spacing) / (cell + spacing);
    }

    /** 该格是否落在图内；越界即视为配置写错了对应的 col/row */
    public boolean contains(int col, int row) {
        return col >= 0 && row >= 0 && col < columns && row < rows;
    }

    /**
     * 切出第 (col, row) 格。
     *
     * @return 越界时返回 {@code null}，由调用方决定是跳过还是报错
     */
    public Image sub(int col, int row) {
        if (!contains(col, row)) {
            return null;
        }
        int x = originX + col * (cellWidth + spacingX);
        int y = originY + row * (cellHeight + spacingY);
        return new WritableImage(reader, x, y, cellWidth, cellHeight);
    }

    public Image getImage() {
        return image;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }
}
