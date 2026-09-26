package com.demo.mota.engine.skill.effect.builtin;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.battle.BattleState;
import com.demo.mota.engine.skill.effect.BattleSide;

/**
 * 回合强化：释放回合内，指定一方的伤害乘以 {@code multiplier}。战前（第 0 回合）释放无效果。
 */
public final class RoundDamageMultiplierEffect extends RoundScopedEffect {

    private final double multiplier;

    public RoundDamageMultiplierEffect(BattleSide side, int castRound, double multiplier) {
        super(side, castRound);
        this.multiplier = multiplier;
    }

    @Override
    public GameNumber adjustPlayerDamagePerRound(BattleState state, GameNumber baseDamagePerRound) {
        return side == BattleSide.PLAYER && isCastRound(state)
                ? baseDamagePerRound.scaledBy(multiplier) : baseDamagePerRound;
    }

    @Override
    public GameNumber adjustMonsterDamagePerRound(BattleState state, GameNumber baseDamagePerRound) {
        return side == BattleSide.MONSTER && isCastRound(state)
                ? baseDamagePerRound.scaledBy(multiplier) : baseDamagePerRound;
    }
}
