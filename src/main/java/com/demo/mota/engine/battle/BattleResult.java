package com.demo.mota.engine.battle;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.state.monster.DamageRange;

public record BattleResult(GameNumber totalDamage, DamageRange damageRange) {

    /** 满血状态下能否战胜（伤害分级以生命上限为基准，故与当前生命值无关） */
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
