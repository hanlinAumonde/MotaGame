package com.demo.mota.ui;

import com.demo.mota.engine.state.monster.DamageRange;
import javafx.scene.paint.Color;

/**
 * 伤害分级配色，地图上的伤害数字与怪物手册共用同一套颜色语义。
 */
public final class DamagePalette {

    private DamagePalette() {}

    public static Color colorOf(DamageRange range) {
        return switch (range) {
            case NONE -> Color.LIGHTGREEN;
            case LOW -> Color.LIMEGREEN;
            case MEDIUM -> Color.YELLOW;
            case HIGH -> Color.ORANGE;
            case DEATH -> Color.RED;
            case OVER_KILL -> Color.DARKRED;
        };
    }

    /**
     * 伤害数字的最终颜色：此刻会被打死的怪物一律标红，
     * 其余按分级取色（分级以生命上限为基准，不体现「此刻会不会死」）。
     */
    public static Color colorOf(DamageRange range, boolean lethalNow) {
        return lethalNow ? Color.RED : colorOf(range);
    }

    /**
     * 深色背景（菜单面板）下的伤害配色：语义与 {@link #colorOf(DamageRange, boolean)} 一致，
     * 只把压在深底上看不清的 {@code OVER_KILL} 暗红提亮，其余沿用同一套颜色。
     */
    public static Color colorOnDark(DamageRange range, boolean lethalNow) {
        if (!lethalNow && range == DamageRange.OVER_KILL) {
            return Color.web("#ff5c5c");
        }
        return colorOf(range, lethalNow);
    }
}
