package com.demo.mota.ui.screen;

import com.demo.mota.engine.rules.GameRules;
import javafx.scene.input.KeyCode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 组合快捷键：按住修饰键（{@link #SKILL_CHORD} D / {@link #EQUIPMENT_SET_CHORD} W）再按数字键。
 * 数字键 → 下标取自塔规则：技能组 {@code skill.hotkeys}、套装 {@code equipment.setHotkeys}。
 * 对局界面与技能设置 / 装备界面共用同一套按法。
 */
public final class PresetHotkeys {

    /** 技能组快捷键的修饰键：按住 D 再按数字键（对局中切换 / 停用，技能设置界面中切到该套编辑） */
    public static final KeyCode SKILL_CHORD = KeyCode.D;
    /** 套装快捷键的修饰键：按住 W 再按数字键（对局中换上该套，装备界面中把当前穿戴存为该套） */
    public static final KeyCode EQUIPMENT_SET_CHORD = KeyCode.W;

    private PresetHotkeys() {}

    public static Map<KeyCode, Integer> fromRules() {
        return parse(GameRules.get().skill().hotkeys());
    }

    /** 装备套装的按键 → 套装下标，取自塔规则 {@code equipment.setHotkeys}（装备界面与套装界面共用） */
    public static Map<KeyCode, Integer> equipmentSetsFromRules() {
        return parse(GameRules.get().equipment().setHotkeys());
    }

    /** 规则里写的是 JavaFX {@code KeyCode} 名；写错的忽略，不影响其余按键 */
    static Map<KeyCode, Integer> parse(List<String> names) {
        Map<KeyCode, Integer> keys = new EnumMap<>(KeyCode.class);
        for (int i = 0; i < names.size(); i++) {
            try {
                keys.put(KeyCode.valueOf(names.get(i).trim().toUpperCase()), i);
            } catch (IllegalArgumentException e) {
                System.err.println("[PresetHotkeys] 无法识别的预设按键: " + names.get(i));
            }
        }
        return keys;
    }
}
