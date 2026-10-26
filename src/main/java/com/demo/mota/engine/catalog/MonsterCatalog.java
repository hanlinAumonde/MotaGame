package com.demo.mota.engine.catalog;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.state.monster.Monster;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 怪物目录：按 id 造出新的怪物实例。
 *
 * <p>造出来的怪物还没有伤害预计算——那要等它落到地图上、与玩家一起推演；
 * 地图装好之后由 {@code BattleHandler.recalculateAllDamage} 统一算（改造前是构造时回头去取玩家先算一遍）。
 */
public final class MonsterCatalog extends Catalog<MonsterDefinition> {

    private final SkillCatalog skills;

    public MonsterCatalog(Collection<MonsterDefinition> definitions, SkillCatalog skills) {
        super("怪物", definitions, MonsterDefinition::id);
        this.skills = skills;
    }

    /** @throws IllegalArgumentException 怪物表里没有这个 id */
    public Monster create(String id) {
        MonsterDefinition d = require(id);
        Map<StateType, GameNumber> stateMap = Map.of(
                StateType.HP, d.health(),
                StateType.ATK, d.attack(),
                StateType.DEF, d.defense());
        return new Monster(d.id(), d.name(), stateMap, Direction.DOWN, d.gold(), d.experience(),
                skills.createAll(d.skills()), d.tags() == null ? List.of() : d.tags());
    }

    @Override
    protected String spriteOf(MonsterDefinition definition) {
        return definition.sprite();
    }
}
