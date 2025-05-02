package com.demo.mota.engine.factory.monster;

import com.demo.mota.engine.state.monster.Monster;

import java.math.BigInteger;

@FunctionalInterface
public interface MonsterCreator {
    Monster createMonster(String monnsterId, String monsterName,
                          BigInteger monsterHealth, BigInteger monsterAttack,
                          BigInteger monsterDefense,
                          long monsterGoldReward, BigInteger monsterExperienceReward);
}
