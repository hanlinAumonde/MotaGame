package com.demo.mota.ui.kit;

import javafx.scene.paint.Color;

/**
 * 界面文字的配色（原 {@code PanelStyle}）。面板、选中框、整屏底色这类「形状」的样式在 {@code mota.css} 里
 * （{@code .mota-screen} / {@code .mota-panel} / {@code .mota-selection}），这里只剩代码里给文字上色要用的颜色。
 */
public final class Palette {

    public static final Color HINT = Color.web("#c6ccf5");
    public static final Color TITLE = Color.web("#ffb144");
    public static final Color SECTION = Color.web("#7fdfff");
    public static final Color SKILL_TITLE = Color.web("#7cfc7c");
    public static final Color DESC = Color.web("#bfe9ff");
    public static final Color PASSIVE_SKILL = Color.web("#ff9ad8");
    public static final Color ACTIVE_SKILL = Color.web("#ff7a6b");
    public static final Color DISABLED = Color.web("#8a8fa8");
    public static final Color LABEL = Color.web("#4cf04c");
    public static final Color VALUE = Color.web("#9fe8ff");
    public static final Color NOTICE = Color.web("#ffd95a");

    /** 面板描边的暗色（虚线空槽、细分隔线用；面板本身的描边在 CSS 里） */
    public static final Color BORDER_DIM = Color.rgb(170, 182, 255, 0.45);

    private Palette() {
    }
}
