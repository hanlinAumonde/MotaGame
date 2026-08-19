package com.demo.mota.engine.battle;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.state.monster.DamageRange;

public record BattleResult(GameNumber totalDamage, DamageRange damageRange) {

    public boolean canWin() {
        return damageRange != DamageRange.DEATH && damageRange != DamageRange.OVER_KILL;
    }

    static BattleResult cannotWin() {
        return new BattleResult(GameNumber.ZERO, DamageRange.OVER_KILL);
    }

    static BattleResult noDamage() {
        return new BattleResult(GameNumber.ZERO, DamageRange.NONE);
    }
}
