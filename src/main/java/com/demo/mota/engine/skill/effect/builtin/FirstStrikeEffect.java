package com.demo.mota.engine.skill.effect.builtin;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.battle.BattleEffect;
import com.demo.mota.engine.battle.BattleState;
import com.demo.mota.engine.skill.effect.BattleSide;

/**
 * 先手攻击：战斗进入第一回合之前，指定一方先白打 {@code times} 次基础伤害，
 * 对方在这几次攻击中无法反击。
 *
 * <p>基础伤害取 {@link BattleState#getBaseMonsterDamage()} / {@link BattleState#getBasePlayerDamage()}，
 * 即已经计入战前增减益的那份；基础伤害为 0 时先手自然也打不出伤害，
 * 符合「效果不凭空创造伤害」的约定。
 */
public final class FirstStrikeEffect implements BattleEffect {

    private final BattleSide attacker;
    private final int times;

    public FirstStrikeEffect(BattleSide attacker, int times) {
        this.attacker = attacker;
        this.times = Math.max(0, times);
    }

    @Override
    public void onBattleStart(BattleState state) {
        if (times == 0) {
            return;
        }
        GameNumber multiplier = GameNumber.of(times);
        if (attacker == BattleSide.MONSTER) {
            GameNumber damage = state.getBaseMonsterDamage().times(multiplier);
            state.setPlayerHp(state.getPlayerHp().minus(damage));
        } else {
            GameNumber damage = state.getBasePlayerDamage().times(multiplier);
            state.setMonsterHp(state.getMonsterHp().minus(damage));
        }
    }
}
