package com.demo.mota.engine.skill.effect.builtin;

import com.demo.mota.engine.battle.BattleState;
import com.demo.mota.engine.skill.effect.BattleSide;

/**
 * 回合眩晕：从释放回合起连续 {@code duration} 个回合，怪物不反击。战前（第 0 回合）释放时从第 1 回合算起。
 *
 * <p>战斗流程里只有怪物的反击可以被跳过（{@code shouldMonsterAttack}），
 * 因此本机制只对怪物生效；目标解析为玩家时不产生效果。
 */
public final class RoundStunEffect extends RoundScopedEffect {

    private final int duration;

    /**
     * @param stunned 被眩晕的一方
     */
    public RoundStunEffect(BattleSide stunned, int castRound, int duration) {
        super(stunned, castRound);
        this.duration = Math.max(1, duration);
    }

    @Override
    public boolean shouldMonsterAttack(BattleState state) {
        if (side != BattleSide.MONSTER) {
            return true;
        }
        int from = Math.max(1, castRound);
        int round = state.getRound();
        return round < from || round >= from + duration;
    }
}
