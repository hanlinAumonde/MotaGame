package com.demo.mota.engine.factory.monster;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.engine.skill.Skill;

import java.util.List;

@FunctionalInterface
public interface MonsterCreator {
    Monster createMonster(String monnsterId, String monsterName,
                          GameNumber monsterHealth, GameNumber monsterAttack,
                          GameNumber monsterDefense,
                          long monsterGoldReward, GameNumber monsterExperienceReward,
                          List<Skill> monsterSkills);
}
