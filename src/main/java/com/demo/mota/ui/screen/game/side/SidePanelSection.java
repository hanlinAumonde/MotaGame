package com.demo.mota.ui.screen.game.side;

/**
 * 右侧栏中的一个分区（技能组合 / 被动技能 / 当前装备……）。
 *
 * <p>右侧栏显示哪些分区、按什么顺序，由塔规则 {@code sidePanel.sections} 决定，
 * 分区 id 经 {@link SidePanelSectionRegistry} 找到实现。新增一种分区 = 实现本接口并注册，
 * 右侧栏本身不用改——将来造塔器里「这座塔的右侧栏长什么样」就落在这份配置上。
 *
 * <p>绘制分两步：右侧栏先向每个分区要 {@link #height}，再把剩余空间均匀分到分区之间
 * （首个贴顶、末个贴底），最后逐个 {@link #render}。
 */
public interface SidePanelSection {

    /** 按当前内容，本分区需要的高度 */
    double height(SidePanelContext context, double width);

    /** 从 {@code y} 开始自上而下绘制本分区，占用高度与 {@link #height} 一致 */
    void render(SidePanelContext context, double x, double y, double width);
}
