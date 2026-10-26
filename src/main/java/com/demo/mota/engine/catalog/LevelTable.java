package com.demo.mota.engine.catalog;

import com.demo.mota.engine.GameNumber;

import java.util.List;

/**
 * 等级表（{@code levels.json}）：各级所需经验、升级加成、回满与习得技能。
 *
 * @param defaultBonus 没有单独配置 {@code bonus} 的等级使用的加成
 * @param levels       按等级顺序排列，第一项即开局等级
 */
public record LevelTable(List<BonusDefinition> defaultBonus, List<LevelDefinition> levels) {

    public LevelTable {
        defaultBonus = defaultBonus == null ? List.of() : List.copyOf(defaultBonus);
        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException("等级表为空");
        }
        levels = List.copyOf(levels);
    }

    /**
     * @param stat  {@code StateType} 的字符串值（生命成长用 {@code maxHealth}）
     * @param type  {@code flat} / {@code percentage}
     */
    public record BonusDefinition(String stat, String type, int value) {
    }

    /**
     * @param bonus       升到该级时的加成，缺省用 {@link LevelTable#defaultBonus()}
     * @param fullHeal    升到该级时回满生命值（在全部加成生效之后），缺省为否
     * @param learnSkills 升到该级时习得的技能 id，缺省为不习得
     */
    public record LevelDefinition(int levelNumber, String levelName, GameNumber maxExperience,
                                  List<BonusDefinition> bonus, Boolean fullHeal, List<String> learnSkills) {
    }
}
