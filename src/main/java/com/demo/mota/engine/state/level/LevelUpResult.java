package com.demo.mota.engine.state.level;

import java.util.List;

/**
 * 一次经验结算的结果。
 *
 * @param bonuses         本次升级（可能跨多级）累计的全部属性加成
 * @param fullHeal        本次升级经过的等级中，是否存在配置了 {@code fullHeal} 的等级；
 *                        为 true 时在所有加成生效后将生命值直接回满
 * @param learnedSkillIds 本次升级经过的等级配置的全部习得技能 id（按等级顺序）
 */
public record LevelUpResult(int previousLevel, int newLevel, List<LevelBonus> bonuses, boolean fullHeal,
                            List<String> learnedSkillIds) {

    public boolean didLevelUp() {
        return newLevel > previousLevel;
    }

    public int levelsGained() {
        return newLevel - previousLevel;
    }
}
