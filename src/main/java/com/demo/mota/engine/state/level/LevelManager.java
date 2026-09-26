package com.demo.mota.engine.state.level;

import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.GameNumber;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.demo.mota.engine.configs.LevelConfigConstants.LEVEL_CONFIG_PATH;

public class LevelManager {
    private static final LevelConfig loadedConfig;

    private record BonusData(String stat, String type, int value) implements Serializable {}
    /**
     * {@code fullHeal} 为 true 时，升到该等级会直接回满生命值（缺省视为 false）；
     * {@code learnSkills} 为升到该等级时习得的技能 id（缺省为不习得）
     */
    private record LevelData(int levelNumber, String levelName, GameNumber maxExperience,
                             List<BonusData> bonus, Boolean fullHeal,
                             List<String> learnSkills) implements Serializable {}
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

    /**
     * 显式触发等级配置的静态初始化，供启动引导（{@code GameBootstrap}）在加载阶段调用。
     * 本类的配置读取在静态块里，不主动碰一下这个类就要等到第一次升级时才读盘。
     */
    public static void preload() {
        // 触碰静态字段即可保证静态块已执行；取个引用防止被优化掉
        Objects.requireNonNull(loadedConfig, "Level config not loaded");
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
        List<String> learnedSkillIds = new ArrayList<>();
        boolean fullHeal = false;

        GameNumber remainingExp = this.currentExperience.plus(experience);

        while (remainingExp.compareTo(maxExperienceForCurrentLevel) >= 0
                && this.levelNumber < loadedConfig.levels.size()) {
            remainingExp = remainingExp.minus(maxExperienceForCurrentLevel);
            allBonuses.addAll(getBonusesForNextLevel());
            fullHeal |= isNextLevelFullHeal();
            learnedSkillIds.addAll(getSkillsForNextLevel());
            loadNextLevel();
        }

        this.currentExperience = remainingExp;
        return new LevelUpResult(previousLevel, this.levelNumber,
                Collections.unmodifiableList(allBonuses), fullHeal,
                Collections.unmodifiableList(learnedSkillIds));
    }

    /** 即将升入的等级配置的习得技能 */
    private List<String> getSkillsForNextLevel() {
        int nextIndex = this.levelNumber;
        if (nextIndex >= loadedConfig.levels.size()) {
            return List.of();
        }
        List<String> skills = loadedConfig.levels.get(nextIndex).learnSkills;
        return skills == null ? List.of() : skills;
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
