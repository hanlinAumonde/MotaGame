package com.demo.mota.engine.battle;

import com.demo.mota.engine.GameNumber;

/**
 * 战斗效果接口。
 *
 * <p>概率型效果一律按期望值实现（如暴击折算为每回合伤害的加成倍率），
 * 因此所有钩子内的计算都是确定性的，模拟结果唯一。
 *
 * <p>约定：伤害调整类钩子作用于基础伤害（攻击-防御）之上，
 * 基础伤害为0时效果不会凭空创造出伤害。
 */
public interface BattleEffect {

    /**
     * 战斗开始前触发一次，可用于抢先手等一次性效果。
     */
    default void onBattleStart(BattleState state) {}

    /**
     * 调整玩家本回合对怪物造成的伤害（期望化）。
     */
    default GameNumber adjustPlayerDamagePerRound(BattleState state, GameNumber baseDamagePerRound) {
        return baseDamagePerRound;
    }

    /**
     * 调整怪物本回合对玩家造成的伤害（期望化）。
     */
    default GameNumber adjustMonsterDamagePerRound(BattleState state, GameNumber baseDamagePerRound) {
        return baseDamagePerRound;
    }

    /**
     * 判断怪物本回合是否能够反击，返回false表示跳过反击（如眩晕）。
     * 所有效果均返回true时怪物才发动攻击。
     */
    default boolean shouldMonsterAttack(BattleState state) {
        return true;
    }

    /**
     * 回合结束后触发，可用于效果持续回合数递减等。
     */
    default void onRoundEnd(BattleState state) {}
}
