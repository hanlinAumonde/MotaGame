package com.demo.mota.world;

import com.almasb.fxgl.app.scene.Viewport;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.ui.MotaLayout;

/**
 * 世界坐标：地图左上角即世界原点，一格 {@link #CELL} 像素。
 *
 * <p>地图在窗口里的位置（中间栏）交给视口：{@link #applyViewport} 把视口挪到 {@code (-MAP_X, -MAP_Y)}。
 * 这样 FXGL 的 {@code CellMoveComponent}（按「格号 × 格宽 + 半格」算格心，默认第 0 格在世界原点）可以直接用，
 * 鼠标所在格也只是 {@code floor(mouseXWorld / CELL)}；两侧的 HUD 在 UI 层，不受视口影响。
 */
public final class WorldGrid {

    /** 一格的像素数（832 / 13 = 64） */
    public static final int CELL = MotaLayout.MAP_SIZE / GameMap.SIDE;

    private WorldGrid() {
    }

    public static double x(Position pos) {
        return pos.x() * CELL;
    }

    public static double y(Position pos) {
        return pos.y() * CELL;
    }

    /** {@code startNewGame()} 会解绑视口（源码：GameScene.reset）；每局开始时设置一次 */
    public static void applyViewport(Viewport viewport) {
        viewport.setX(-MotaLayout.MAP_X);
        viewport.setY(-MotaLayout.MAP_Y);
    }
}
