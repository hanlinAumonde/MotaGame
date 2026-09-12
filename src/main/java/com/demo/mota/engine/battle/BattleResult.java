package com.demo.mota.engine.battle;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.state.monster.DamageRange;

/**
 * 战斗模拟结果。
 *
 * @param totalDamage 击败该怪物玩家将承受的总伤害
 * @param damageRange 伤害分级（以玩家生命上限为基准）
 * @param rounds      击杀实际用掉的回合数（由 {@code BattleSimulator} 逐回合推演得出）；
 *                    无法击败（{@link DamageRange#OVER_KILL}）时为 0，
 *                    战前效果（如玩家先手）就把怪物打死时同样为 0
 */
public record BattleResult(GameNumber totalDamage, DamageRange damageRange, int rounds) {

    /** 满血状态下能否战胜（伤害分级以生命上限为基准，故与当前生命值无关） */
    public boolean canWin() {
        return damageRange != DamageRange.DEATH && damageRange != DamageRange.OVER_KILL;
    }

    static BattleResult cannotWin() {
        return new BattleResult(GameNumber.ZERO, DamageRange.OVER_KILL, 0);
    }
}
