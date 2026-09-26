package com.demo.mota.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * 游戏内各菜单（敌物资料 / 技能设置 / 装备）共用的面板风格：深蓝渐变底 + 半透明面板 + 描边高亮。
 * 集中在这里，改一处三个界面同时生效。
 */
public final class PanelStyle {

    public static final Color PANEL_FILL = Color.rgb(24, 28, 62, 0.86);
    public static final Color PANEL_BORDER = Color.web("#aab6ff");
    public static final Color PANEL_BORDER_DIM = Color.rgb(170, 182, 255, 0.45);
    public static final Color FOCUS_FILL = Color.rgb(255, 255, 255, 0.20);
    public static final Color FOCUS_FILL_DIM = Color.rgb(255, 255, 255, 0.08);

    public static final Color HINT_COLOR = Color.web("#c6ccf5");
    public static final Color TITLE_COLOR = Color.web("#ffb144");
    public static final Color SECTION_COLOR = Color.web("#7fdfff");
    public static final Color SKILL_TITLE_COLOR = Color.web("#7cfc7c");
    public static final Color DESC_COLOR = Color.web("#bfe9ff");
    public static final Color PASSIVE_SKILL_COLOR = Color.web("#ff9ad8");
    public static final Color ACTIVE_SKILL_COLOR = Color.web("#ff7a6b");
    public static final Color DISABLED_COLOR = Color.web("#8a8fa8");
    public static final Color LABEL_COLOR = Color.web("#4cf04c");
    public static final Color VALUE_COLOR = Color.web("#9fe8ff");
    public static final Color NOTICE_COLOR = Color.web("#ffd95a");

    private PanelStyle() {}

    public static void drawBackground(GraphicsContext gc, double width, double height) {
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2e3566")), new Stop(1, Color.web("#171a33"))));
        gc.fillRect(0, 0, width, height);
    }

    public static void drawPanel(GraphicsContext gc, double x, double y, double w, double h, boolean focused) {
        gc.setFill(PANEL_FILL);
        gc.fillRect(x, y, w, h);
        gc.setStroke(focused ? PANEL_BORDER : PANEL_BORDER_DIM);
        gc.setLineWidth(focused ? 3 : 1.5);
        gc.strokeRect(x, y, w, h);
    }

    /** 选中框：一层白色半透明填充 + 亮描边 */
    public static void drawSelection(GraphicsContext gc, double x, double y, double w, double h, boolean focused) {
        gc.setFill(focused ? FOCUS_FILL : FOCUS_FILL_DIM);
        gc.fillRect(x, y, w, h);
        gc.setStroke(focused ? PANEL_BORDER : PANEL_BORDER_DIM);
        gc.setLineWidth(2);
        gc.strokeRect(x, y, w, h);
    }
}
