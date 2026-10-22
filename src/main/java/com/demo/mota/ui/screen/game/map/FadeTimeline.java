package com.demo.mota.ui.screen.game.map;

/**
 * 楼层切换过渡的时间线：淡出到全黑 → 黑屏停留（显示楼层名）→ 淡入。
 *
 * <p>只回答「第 t 秒时遮罩该多不透明、换图了没有、结束了没有」，不碰任何节点，
 * 由 {@link EntityMapView} 的过渡组件每帧调用。换图点在淡出结束的那一刻：
 * 旧楼层在黑幕落下的过程中仍然可见，新楼层在黑幕升起时出现。
 *
 * @param fadeOut 淡出秒数
 * @param hold    全黑停留秒数
 * @param fadeIn  淡入秒数
 */
public record FadeTimeline(double fadeOut, double hold, double fadeIn) {

    /** 默认节奏：0.15 秒落幕、0.3 秒停留、0.2 秒升幕，整段不到 0.7 秒 */
    public static final FadeTimeline FLOOR_SWITCH = new FadeTimeline(0.15, 0.3, 0.2);

    /** 第 t 秒时遮罩的不透明度，{@code [0, 1]} */
    public double opacityAt(double t) {
        if (t <= 0) {
            return 0;
        }
        if (t < fadeOut) {
            return t / fadeOut;
        }
        double fadeInStart = fadeOut + hold;
        if (t < fadeInStart) {
            return 1;
        }
        if (t < duration()) {
            return 1 - (t - fadeInStart) / fadeIn;
        }
        return 0;
    }

    /** 第 t 秒时是否已经到了换图点（淡出完毕、全黑） */
    public boolean isSwapReached(double t) {
        return t >= fadeOut;
    }

    public boolean isDone(double t) {
        return t >= duration();
    }

    public double duration() {
        return fadeOut + hold + fadeIn;
    }
}
