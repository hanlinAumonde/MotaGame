package com.demo.mota.engine.factory.monster;

import com.demo.mota.engine.state.GameNumber;
import com.demo.mota.engine.state.monster.Monster;

@FunctionalInterface
public interface MonsterCreator {
    Monster createMonster(String monnsterId, String monsterName,
                          GameNumber monsterHealth, GameNumber monsterAttack,
                          GameNumber monsterDefense,
                          long monsterGoldReward, GameNumber monsterExperienceReward);
}
