package com.demo.mota.engine.state;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.skill.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCharacterState {
    private final String characterId;
    private final String characterName;

    private final Map<StateType, GameNumber> stateMap;
    private Direction currentDirection;

    /**
     * 持有的技能。玩家与怪物共用同一套技能模型：怪物的技能在构造时由配置带入，
     * 玩家则可以在角色技能系统落地后通过 {@link #learnSkill} 逐步习得。
     */
    private final List<Skill> skills;

    /**
     * 角色标签（如 {@code undead} / {@code beast}），供技能效果按「同类数量」等条件检索。
     *
     * <p>标签是纯字符串而非枚举：种族、阵营、体型这类分类会随内容增加不断长出新值，
     * 用字符串可以只改 {@code monsterList.json} 就引入一个新类别，
     * 技能配置里写同名的 {@code tag} 参数即可与之配对。
     */
    private final Set<String> tags;

    public AbstractCharacterState(String characterId, String characterName, Map<StateType, GameNumber> stateMap, Direction currentDirection) {
        this(characterId, characterName, stateMap, currentDirection, List.of(), List.of());
    }

    public AbstractCharacterState(String characterId, String characterName, Map<StateType, GameNumber> stateMap,
                                  Direction currentDirection, List<Skill> skills) {
        this(characterId, characterName, stateMap, currentDirection, skills, List.of());
    }

    public AbstractCharacterState(String characterId, String characterName, Map<StateType, GameNumber> stateMap,
                                  Direction currentDirection, List<Skill> skills, List<String> tags) {
        this.characterId = characterId;
        this.characterName = characterName;
        this.stateMap = new HashMap<>(stateMap);
        this.currentDirection = currentDirection;
        this.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
        this.tags = new LinkedHashSet<>();
        if (tags != null) {
            tags.stream().filter(tag -> tag != null && !tag.isBlank())
                    .map(String::trim).forEach(this.tags::add);
        }
    }

    public GameNumber getStateValue(StateType stateType) {
        return this.stateMap.get(stateType);
    }

    public String getCharacterId() {
        return characterId;
    }

    public String getCharacterName() {
        return characterName;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public void setCurrentDirection(Direction direction) {
        this.currentDirection = direction;
    }

    public void updateState(StateType stateType, GameNumber value) {
        this.stateMap.put(stateType, value);
    }

    // --- 技能 ---

    public List<Skill> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public boolean hasSkills() {
        return !skills.isEmpty();
    }

    /** 习得技能；同一技能 id 不重复持有 */
    public void learnSkill(Skill skill) {
        if (skill == null) return;
        boolean alreadyOwned = skills.stream()
                .anyMatch(existing -> existing.skillId().equals(skill.skillId()));
        if (!alreadyOwned) {
            skills.add(skill);
        }
    }

    public void forgetSkill(String skillId) {
        skills.removeIf(skill -> skill.skillId().equals(skillId));
    }

    // --- 标签 ---

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public boolean hasTag(String tag) {
        return tag != null && tags.contains(tag.trim());
    }

    public void addTag(String tag) {
        if (tag != null && !tag.isBlank()) {
            tags.add(tag.trim());
        }
    }

    public void removeTag(String tag) {
        if (tag != null) {
            tags.remove(tag.trim());
        }
    }
}
