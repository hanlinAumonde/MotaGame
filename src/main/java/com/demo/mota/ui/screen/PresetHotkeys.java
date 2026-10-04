package com.demo.mota.ui.screen;

import com.demo.mota.engine.rules.GameRules;
import javafx.scene.input.KeyCode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 切换技能预设的按键 → 预设下标，取自塔规则 {@code skill.hotkeys}。
 * 对局界面（激活 / 停用）与技能设置界面（切换正在编辑的预设）共用。
 */
public final class PresetHotkeys {

    private PresetHotkeys() {}

    public static Map<KeyCode, Integer> fromRules() {
        return parse(GameRules.get().skill().hotkeys());
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
