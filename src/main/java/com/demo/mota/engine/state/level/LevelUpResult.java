package com.demo.mota.engine.state.level;

import java.util.List;

public record LevelUpResult(int previousLevel, int newLevel, List<LevelBonus> bonuses) {

    public boolean didLevelUp() {
        return newLevel > previousLevel;
    }

    public int levelsGained() {
        return newLevel - previousLevel;
    }
}
