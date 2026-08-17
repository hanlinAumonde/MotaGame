package com.demo.mota.engine.battle;

import com.demo.mota.engine.state.GameNumber;

public interface BattleEffect {

    default GameNumber adjustPlayerDamage(GameNumber baseDamagePerRound) {
        return baseDamagePerRound;
    }

    default GameNumber adjustMonsterDamage(GameNumber baseDamagePerRound) {
        return baseDamagePerRound;
    }
}
