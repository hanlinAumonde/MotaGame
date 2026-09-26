package com.demo.mota.engine.skill.effect.builtin;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.battle.BattleState;
import com.demo.mota.engine.skill.effect.BattleSide;

/**
 * 追加攻击：释放回合内，指定一方额外打出 {@code times} 次「基础伤害 × {@code multiplier}」。
 *
 * <ul>
 *   <li>第 1..N 回合释放：追加伤害并入该回合的伤害，与普攻一起结算；</li>
 *   <li>战前（第 0 回合）释放：在 {@code onBattleStart} 直接打出，对方无法反击——等同于先手。</li>
 * </ul>
 * 基础伤害为 0 时追加攻击同样打不出伤害。
 */
public final class ExtraStrikeEffect extends RoundScopedEffect {

    private final int times;
    private final double multiplier;

    public ExtraStrikeEffect(BattleSide attacker, int castRound, int times, double multiplier) {
        super(attacker, castRound);
        this.times = Math.max(0, times);
        this.multiplier = multiplier;
    }

    @Override
    public void onBattleStart(BattleState state) {
        if (!isPreBattle() || times == 0) {
            return;
        }
        if (side == BattleSide.PLAYER) {
            state.setMonsterHp(state.getMonsterHp().minus(extra(state.getBasePlayerDamage())));
        } else {
            state.setPlayerHp(state.getPlayerHp().minus(extra(state.getBaseMonsterDamage())));
        }
    }

    @Override
    public GameNumber adjustPlayerDamagePerRound(BattleState state, GameNumber baseDamagePerRound) {
        return side == BattleSide.PLAYER && isCastRound(state)
                ? baseDamagePerRound.plus(extra(state.getBasePlayerDamage())) : baseDamagePerRound;
    }

    @Override
    public GameNumber adjustMonsterDamagePerRound(BattleState state, GameNumber baseDamagePerRound) {
        return side == BattleSide.MONSTER && isCastRound(state)
                ? baseDamagePerRound.plus(extra(state.getBaseMonsterDamage())) : baseDamagePerRound;
    }

    private GameNumber extra(GameNumber baseDamage) {
        return baseDamage.scaledBy(multiplier).times(GameNumber.of(times));
    }
}
