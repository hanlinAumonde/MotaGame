package com.demo.mota.ui;

/**
 * 窗口与对局画面的布局尺寸（原来写在 {@code MotaController} 里）。
 *
 * <p>窗口 1296×840：左状态栏 208 + 中间地图 832 + 右技能装备栏 256；高 840（地图 832 + 底部留白）。
 * 菜单类界面铺满窗口；地图是世界层里的实体，左上角落在 {@link #MAP_X} / {@link #MAP_Y}。
 */
public final class MotaLayout {

    public static final int WIDTH = 1296;
    public static final int HEIGHT = 840;

    public static final int STATUS_WIDTH = 208;
    public static final int SIDE_WIDTH = 256;

    /** 地图区域：左上角在窗口中的位置与边长 */
    public static final int MAP_X = STATUS_WIDTH;
    public static final int MAP_Y = 0;
    public static final int MAP_SIZE = 832;

    private MotaLayout() {
    }
}
