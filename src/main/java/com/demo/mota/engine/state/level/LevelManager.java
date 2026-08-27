package com.demo.mota.engine.state.level;

import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.GameNumber;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.demo.mota.engine.configs.LevelConfigConstatnts.LEVEL_CONFIG_PATH;

public class LevelManager {
    private static final LevelConfig loadedConfig;

    private record BonusData(String stat, String type, int value) implements Serializable {}
    /** {@code fullHeal} 为 true 时，升到该等级会直接回满生命值（缺省视为 false） */
    private record LevelData(int levelNumber, String levelName, GameNumber maxExperience,
                             List<BonusData> bonus, Boolean fullHeal) implements Serializable {}
    private record LevelConfig(List<BonusData> defaultBonus,
                               List<LevelData> levels) implements Serializable {}

    static {
        loadedConfig = initializeConfig();
    }

    private String levelName;
    private int levelNumber;
    private GameNumber maxExperienceForCurrentLevel;
    private GameNumber currentExperience;

    public LevelManager() {
        LevelData first = loadedConfig.levels.get(0);
        this.levelName = first.levelName;
        this.levelNumber = first.levelNumber;
        this.maxExperienceForCurrentLevel = first.maxExperience;
        this.currentExperience = GameNumber.ZERO;
    }

    private static LevelConfig initializeConfig() {
        return ResourceManager.getInstance().loadJsonResource(LEVEL_CONFIG_PATH, new TypeReference<>() {});
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public GameNumber getCurrentExperience() {
        return currentExperience;
    }

    public LevelUpResult cumulateExperience(GameNumber experience) {
        int previousLevel = this.levelNumber;
        List<LevelBonus> allBonuses = new ArrayList<>();
        boolean fullHeal = false;

        GameNumber remainingExp = this.currentExperience.plus(experience);

        while (remainingExp.compareTo(maxExperienceForCurrentLevel) >= 0
                && this.levelNumber < loadedConfig.levels.size()) {
            remainingExp = remainingExp.minus(maxExperienceForCurrentLevel);
            allBonuses.addAll(getBonusesForNextLevel());
            fullHeal |= isNextLevelFullHeal();
            loadNextLevel();
        }

        this.currentExperience = remainingExp;
        return new LevelUpResult(previousLevel, this.levelNumber,
                Collections.unmodifiableList(allBonuses), fullHeal);
    }

    /** 即将升入的等级是否配置了回满生命值 */
    private boolean isNextLevelFullHeal() {
        int nextIndex = this.levelNumber; // levelNumber 从 1 开始，索引 = levelNumber 即为下一级
        if (nextIndex >= loadedConfig.levels.size()) {
            return false;
        }
        return Boolean.TRUE.equals(loadedConfig.levels.get(nextIndex).fullHeal);
    }

    private List<LevelBonus> getBonusesForNextLevel() {
        int nextIndex = this.levelNumber; // levelNumber is 1-based, so index = levelNumber points to the next level
        if (nextIndex >= loadedConfig.levels.size()) {
            return List.of();
        }

        LevelData nextLevel = loadedConfig.levels.get(nextIndex);
        List<BonusData> rawBonuses = nextLevel.bonus != null ? nextLevel.bonus : loadedConfig.defaultBonus;

        if (rawBonuses == null) return List.of();

        return rawBonuses.stream()
                .map(b -> new LevelBonus(
                        StateType.fromString(b.stat),
                        LevelBonus.BonusType.fromString(b.type),
                        b.value))
                .toList();
    }

    private void loadNextLevel() {
        int nextIndex = this.levelNumber; // current levelNumber is 1-based
        if (nextIndex < loadedConfig.levels.size()) {
            LevelData nextLevel = loadedConfig.levels.get(nextIndex);
            this.levelName = nextLevel.levelName;
            this.levelNumber = nextLevel.levelNumber;
            this.maxExperienceForCurrentLevel = nextLevel.maxExperience;
        }
    }
}
