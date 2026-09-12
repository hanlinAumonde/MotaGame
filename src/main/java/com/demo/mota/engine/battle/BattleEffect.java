package com.demo.mota.engine.battle;

import com.demo.mota.engine.GameNumber;

/**
 * 战斗效果接口：战斗模拟器在固定的几个钩子上回调它，是技能（以及未来的装备 / 地形等）
 * 影响战斗结算的<b>唯一</b>入口。本接口刻意不认识「技能」这个概念——
 * 由 {@code engine.skill.effect} 把技能配置翻译成这里的实现，战斗层保持与技能系统解耦。
 *
 * <p>概率型效果一律按期望值实现（如暴击折算为每回合伤害的加成倍率），
 * 因此所有钩子内的计算都是确定性的，模拟结果唯一。
 *
 * <p>约定：伤害调整类钩子作用于基础伤害（攻击-防御）之上，
 * 基础伤害为0时效果不会凭空创造出伤害。
 *
 * <p>钩子的触发顺序：
 * <ol>
 *   <li>{@link #adjustPlayerStats} / {@link #adjustMonsterStats}（战斗前，按效果列表顺序折叠）</li>
 *   <li>{@link #onBattleStart}（基础伤害已算出并写入 {@link BattleState}）</li>
 *   <li>每回合：{@link #adjustPlayerDamagePerRound} → {@link #adjustMonsterDamagePerRound}
 *       → 玩家攻击 → {@link #shouldMonsterAttack} → 怪物反击 → {@link #onRoundEnd}</li>
 * </ol>
 */
public interface BattleEffect {

    /**
     * 战斗开始前调整玩家属性，返回调整后的快照。
     *
     * <p>多个效果按列表顺序依次折叠，后一个效果看到的是前面已调整过的快照；
     * 基础伤害（攻击-防御）在全部调整完成后才计算，因此这里是实现
     * 「战斗期间攻击力降低 10%」这类增减益的地方。
     */
    default BattleSnapshot adjustPlayerStats(BattleSnapshot player, BattleSnapshot monster) {
        return player;
    }

    /**
     * 战斗开始前调整怪物属性，语义同 {@link #adjustPlayerStats}。
     */
    default BattleSnapshot adjustMonsterStats(BattleSnapshot monster, BattleSnapshot player) {
        return monster;
    }

    /**
     * 战斗开始前触发一次，可用于抢先手等一次性效果。
     * 此时 {@link BattleState} 中的双方属性与基础伤害均已就绪。
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
