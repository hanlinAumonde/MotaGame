package com.demo.mota.engine.skill.effect.builtin;

import com.demo.mota.engine.battle.BattleEffect;
import com.demo.mota.engine.battle.BattleState;
import com.demo.mota.engine.skill.effect.BattleSide;

/**
 * 主动技能效果的基类：只在<b>释放回合</b>生效。
 *
 * <p>回合编号与 {@link BattleState#getRound()} 一致（从 1 开始）；
 * {@code castRound == 0} 表示战前，只有「战前能做的事」（例如 {@link ExtraStrikeEffect} 的抢攻）
 * 会在 {@link #onBattleStart} 里生效，纯粹调整回合伤害的机制在第 0 回合没有意义、自然不生效。
 *
 * <p>模拟器本身对此一无所知——它照常在每个钩子上回调，由本类在钩子里对照回合号过滤。
 */
public abstract class RoundScopedEffect implements BattleEffect {

    protected final BattleSide side;
    protected final int castRound;

    protected RoundScopedEffect(BattleSide side, int castRound) {
        this.side = side;
        this.castRound = castRound;
    }

    public int getCastRound() {
        return castRound;
    }

    protected boolean isPreBattle() {
        return castRound == 0;
    }

    protected boolean isCastRound(BattleState state) {
        return castRound > 0 && state.getRound() == castRound;
    }
}
