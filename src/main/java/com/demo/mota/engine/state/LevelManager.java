package com.demo.mota.engine.state;

import com.demo.mota.engine.enums.StateType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.demo.mota.engine.configs.LevelConfigConstatnts.LEVEL_CONFIG_PATH;

public class LevelManager {
    private static final LevelConfig loadedConfig;

    private record BonusData(String stat, String type, int value) implements Serializable {}
    private record LevelData(int levelNumber, String levelName, GameNumber maxExperience,
                             List<BonusData> bonus) implements Serializable {}
    private record LevelConfig(List<BonusData> defaultBonus,
                               List<LevelData> levels) implements Serializable {}

    static {
        loadedConfig = initializeConfig();
    }

    private String levelName;
    private int levelNumber;
    private GameNumber maxExperienceForCurrentLevel;
    private GameNumber currentExperience;

    LevelManager() {
        LevelData first = loadedConfig.levels.get(0);
        this.levelName = first.levelName;
        this.levelNumber = first.levelNumber;
        this.maxExperienceForCurrentLevel = first.maxExperience;
        this.currentExperience = GameNumber.ZERO;
    }

    private static LevelConfig initializeConfig() {
        try (InputStream inputStream = LevelManager.class.getResourceAsStream(LEVEL_CONFIG_PATH)) {
            if (inputStream == null) {
                throw new RuntimeException("Level data file not found: " + LEVEL_CONFIG_PATH);
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(inputStream, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse level data", e);
        }
    }

    public String getLevelName() {
        return levelName;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public GameNumber getMaxExperienceForCurrentLevel() {
        return maxExperienceForCurrentLevel;
    }

    public GameNumber getCurrentExperience() {
        return currentExperience;
    }

    public LevelUpResult cumulateExperience(GameNumber experience) {
        int previousLevel = this.levelNumber;
        List<LevelBonus> allBonuses = new ArrayList<>();

        GameNumber remainingExp = this.currentExperience.plus(experience);

        while (remainingExp.compareTo(maxExperienceForCurrentLevel) >= 0
                && this.levelNumber < loadedConfig.levels.size()) {
            remainingExp = remainingExp.minus(maxExperienceForCurrentLevel);
            allBonuses.addAll(getBonusesForNextLevel());
            loadNextLevel();
        }

        this.currentExperience = remainingExp;
        return new LevelUpResult(previousLevel, this.levelNumber, Collections.unmodifiableList(allBonuses));
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
