package com.demo.mota.engine.state.level;

import java.util.List;

/**
 * 一次经验结算的结果。
 *
 * @param bonuses  本次升级（可能跨多级）累计的全部属性加成
 * @param fullHeal 本次升级经过的等级中，是否存在配置了 {@code fullHeal} 的等级；
 *                 为 true 时在所有加成生效后将生命值直接回满
 */
public record LevelUpResult(int previousLevel, int newLevel, List<LevelBonus> bonuses, boolean fullHeal) {

    public boolean didLevelUp() {
        return newLevel > previousLevel;
    }

    public int levelsGained() {
        return newLevel - previousLevel;
    }
}
