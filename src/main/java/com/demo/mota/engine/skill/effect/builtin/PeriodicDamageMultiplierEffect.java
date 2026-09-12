package com.demo.mota.engine.skill.effect.builtin;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.battle.BattleEffect;
import com.demo.mota.engine.battle.BattleState;
import com.demo.mota.engine.skill.effect.BattleSide;

/**
 * 周期性重击：每满 {@code interval} 个回合，指定一方该回合的伤害乘以 {@code multiplier}。
 *
 * <p>触发回合为 {@code round % interval == 0}，即间隔 3 时在第 3、6、9…… 回合生效。
 * 蓄力型、狂暴型的伤害爆发都可以用这一组参数描述。
 */
public final class PeriodicDamageMultiplierEffect implements BattleEffect {

    private final BattleSide attacker;
    private final int interval;
    private final double multiplier;

    public PeriodicDamageMultiplierEffect(BattleSide attacker, int interval, double multiplier) {
        this.attacker = attacker;
        this.interval = Math.max(1, interval);
        this.multiplier = multiplier;
    }

    @Override
    public GameNumber adjustPlayerDamagePerRound(BattleState state, GameNumber baseDamagePerRound) {
        return attacker == BattleSide.PLAYER && isBurstRound(state)
                ? baseDamagePerRound.scaledBy(multiplier) : baseDamagePerRound;
    }

    @Override
    public GameNumber adjustMonsterDamagePerRound(BattleState state, GameNumber baseDamagePerRound) {
        return attacker == BattleSide.MONSTER && isBurstRound(state)
                ? baseDamagePerRound.scaledBy(multiplier) : baseDamagePerRound;
    }

    private boolean isBurstRound(BattleState state) {
        return state.getRound() % interval == 0;
    }
}
