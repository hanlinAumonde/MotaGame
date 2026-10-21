package com.demo.mota.ui.screen.game.map;

/**
 * 对局界面中间的地图显示。
 *
 * <p>{@code GameScreen} 与 {@code MotaController} 只认这个接口，不认识背后用什么画——
 * 目前的实现是 {@link EntityMapView}（FXGL 实体），由宿主 {@code MotaApplication} 创建后注入。
 */
public interface MapView {

    /** 按引擎当前的地图与玩家状态更新显示（换了一张地图则整张重建，否则只动变了的格子） */
    void sync();

    /** 显示 / 隐藏整张地图（随游戏画面一起，由阶段路由控制） */
    void setVisible(boolean visible);
}
