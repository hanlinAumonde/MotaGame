package com.demo.mota.engine.state;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import static com.demo.mota.engine.configs.LevelConfigConstatnts.LEVEL_CONFIG_PATH;

public class LevelManager {
    private static final List<LevelData> loadedLevelData;
    private record LevelData(String levelName, int levelNumber, GameNumber maxExperience) implements Serializable {}

    static {
        loadedLevelData = initializeLevelData();
    }

    private String levelName;
    private int levelNumber;
    private GameNumber maxExperienceForCurrentLevel;

    private GameNumber currentExperience;

    LevelManager() {
        this.levelName = loadedLevelData.get(0).levelName;
        this.levelNumber = loadedLevelData.get(0).levelNumber;
        this.maxExperienceForCurrentLevel = loadedLevelData.get(0).maxExperience;
        this.currentExperience = GameNumber.ZERO;
    }

    private static List<LevelData> initializeLevelData() {
        try (InputStream inputStream = LevelManager.class.getResourceAsStream(LEVEL_CONFIG_PATH))
        {
            if (inputStream == null) {
                throw new RuntimeException("Level data file not found: " + LEVEL_CONFIG_PATH);
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(inputStream, new TypeReference<List<LevelData>>(){});
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

    public void cumulateExperience(GameNumber experience) {
        GameNumber cumulatedExp = this.currentExperience.plus(experience);
        if(cumulatedExp.compareTo(maxExperienceForCurrentLevel) >= 0) {
            this.currentExperience = cumulatedExp.minus(maxExperienceForCurrentLevel);
            loadNextLevel();
        } else {
            this.currentExperience = cumulatedExp;
        }
    }

    private void loadNextLevel() {
        if (this.levelNumber < loadedLevelData.size() - 1) {
            LevelData nextLevel = loadedLevelData.get(this.levelNumber + 1);
            this.levelName = nextLevel.levelName;
            this.levelNumber = nextLevel.levelNumber;
            this.maxExperienceForCurrentLevel = nextLevel.maxExperience;
        }
    }
}
