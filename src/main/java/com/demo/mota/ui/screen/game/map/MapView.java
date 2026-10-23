package com.demo.mota.ui.screen.game.map;

/**
 * 对局界面中间的地图显示。
 *
 * <p>{@code GameScreen} 只认这个接口，不认识背后用什么画——
 * 目前的实现是 {@link EntityMapView}（FXGL 实体），每局新建一个。
 *
 * <p>显示可以带动画（玩家走一格、楼层切换的黑幕）。动画只是表现：引擎状态在调用 {@link #sync()}
 * 之前就已经是最终结果，动画追上它而已。调用方据 {@link #isAnimating()} 决定要不要先缓一缓下一步输入。
 */
public interface MapView {

    /** 按引擎当前的地图与玩家状态更新显示（换了一张地图则整张重建，否则只动变了的格子） */
    void sync();

    /**
     * 让下一次 {@link #sync()} 遇到换图时播楼层切换过渡（落幕 → 黑屏显示 {@code caption} → 升幕），
     * 而不是直接重建（{@code caption} 可含换行，逐行居中）。下一次同步没有换图则作废。撤销、读档等换图不调它，直接切换。
     */
    void playFloorTransition(String caption);

    /** 是否有动画在播（行走、楼层过渡） */
    boolean isAnimating();

    /** 动画全部播完时回调（每次从「有动画」变为「无动画」调用一次），供调用方接着处理缓下来的输入 */
    void setOnAnimationsFinished(Runnable listener);
}
