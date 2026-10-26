package com.demo.mota.engine.state.level;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.catalog.LevelTable;
import com.demo.mota.engine.catalog.LevelTable.BonusDefinition;
import com.demo.mota.engine.catalog.LevelTable.LevelDefinition;
import com.demo.mota.engine.enums.StateType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一个角色的等级与经验，按 {@link LevelTable} 累计经验、结算升级。 */
public class LevelManager {

    private final List<LevelDefinition> levels;
    private final List<BonusDefinition> defaultBonus;

    private String levelName;
    private int levelNumber;
    private GameNumber maxExperienceForCurrentLevel;
    private GameNumber currentExperience;

    public LevelManager(LevelTable table) {
        this.levels = table.levels();
        this.defaultBonus = table.defaultBonus();
        LevelDefinition first = levels.get(0);
        this.levelName = first.levelName();
        this.levelNumber = first.levelNumber();
        this.maxExperienceForCurrentLevel = first.maxExperience();
        this.currentExperience = GameNumber.ZERO;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public GameNumber getCurrentExperience() {
        return currentExperience;
    }

    /**
     * 直接置为某个等级与经验（读档 / 撤销用），不触发任何升级加成——那些加成早已体现在快照的属性里。
     * 等级表里已没有该等级时落到最接近的一级。
     */
    public void restore(int levelNumber, GameNumber experience) {
        LevelDefinition target = levels.get(levels.size() - 1);
        for (LevelDefinition level : levels) {
            if (level.levelNumber() >= levelNumber) {
                target = level;
                break;
            }
        }
        this.levelName = target.levelName();
        this.levelNumber = target.levelNumber();
        this.maxExperienceForCurrentLevel = target.maxExperience();
        this.currentExperience = experience;
    }

    public LevelUpResult cumulateExperience(GameNumber experience) {
        int previousLevel = this.levelNumber;
        List<LevelBonus> allBonuses = new ArrayList<>();
        List<String> learnedSkillIds = new ArrayList<>();
        boolean fullHeal = false;

        GameNumber remainingExp = this.currentExperience.plus(experience);

        while (remainingExp.compareTo(maxExperienceForCurrentLevel) >= 0
                && this.levelNumber < levels.size()) {
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

    /** 即将升入的等级（levelNumber 从 1 开始，下标 = levelNumber 即为下一级）；已到顶时为 null */
    private LevelDefinition nextLevel() {
        int nextIndex = this.levelNumber;
        return nextIndex < levels.size() ? levels.get(nextIndex) : null;
    }

    /** 即将升入的等级配置的习得技能 */
    private List<String> getSkillsForNextLevel() {
        LevelDefinition next = nextLevel();
        return next == null || next.learnSkills() == null ? List.of() : next.learnSkills();
    }

    /** 即将升入的等级是否配置了回满生命值 */
    private boolean isNextLevelFullHeal() {
        LevelDefinition next = nextLevel();
        return next != null && Boolean.TRUE.equals(next.fullHeal());
    }

    private List<LevelBonus> getBonusesForNextLevel() {
        LevelDefinition next = nextLevel();
        if (next == null) {
            return List.of();
        }
        List<BonusDefinition> rawBonuses = next.bonus() != null ? next.bonus() : defaultBonus;
        return rawBonuses.stream()
                .map(b -> new LevelBonus(
                        StateType.fromString(b.stat()),
                        LevelBonus.BonusType.fromString(b.type()),
                        b.value()))
                .toList();
    }

    private void loadNextLevel() {
        LevelDefinition next = nextLevel();
        if (next != null) {
            this.levelName = next.levelName();
            this.levelNumber = next.levelNumber();
            this.maxExperienceForCurrentLevel = next.maxExperience();
        }
    }
}
