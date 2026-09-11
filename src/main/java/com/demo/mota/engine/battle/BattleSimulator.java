package com.demo.mota.engine.battle;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.state.monster.DamageRange;

import java.util.List;

/**
 * 战斗模拟引擎：以逐回合推演的方式计算战斗结果。
 *
 * <p>玩家每回合先手攻击，怪物在其死亡回合不反击。
 * 每回合根据当前战斗状态重新计算双方有效伤害，
 * 使效果能够实现眩晕、先手、血量阈值触发等状态型机制。
 *
 * <p>总伤害与回合数<b>都由这一次推演产出</b>（回合数取 {@link BattleState#getRound()}）：
 * 效果可以让每回合伤害逐轮变化，任何「血量 ÷ 每回合伤害」式的闭式换算都只在
 * 无效果的特例下成立，故不再对任何分支做此类简化。
 */
public final class BattleSimulator {

    /**
     * 回合数上限，防止效果组合导致无法结束的战斗；超限一律判 {@code OVER_KILL}。
     *
     * <p>该上限对所有战斗一视同仁——包括「打不痛玩家但血厚到打不完」的怪物。
     * 若日后确实需要这类耐久怪，调高本常量即可（示例塔当前最长的战斗为 900 回合）。
     */
    private static final int MAX_ROUNDS = 1000;

    private BattleSimulator() {}

    public static BattleResult simulate(BattleSnapshot player, BattleSnapshot monster) {
        return simulate(player, monster, List.of());
    }

    public static BattleResult simulate(BattleSnapshot player, BattleSnapshot monster, List<BattleEffect> effects) {
        GameNumber basePlayerDamage = player.atk().minus(monster.def()).clampMin(GameNumber.ZERO);
        if (basePlayerDamage.isNonPositive()) {
            return BattleResult.cannotWin();
        }

        GameNumber baseMonsterDamage = monster.atk().minus(player.def()).clampMin(GameNumber.ZERO);

        BattleState state = new BattleState(player.hp(), monster.hp());
        for (BattleEffect effect : effects) {
            effect.onBattleStart(state);
        }

        while (state.getMonsterHp().isPositive()) {
            if (state.getRound() >= MAX_ROUNDS) {
                return BattleResult.cannotWin();
            }
            state.incrementRound();

            GameNumber playerDamage = basePlayerDamage;
            GameNumber monsterDamage = baseMonsterDamage;
            for (BattleEffect effect : effects) {
                playerDamage = effect.adjustPlayerDamagePerRound(state, playerDamage);
                monsterDamage = effect.adjustMonsterDamagePerRound(state, monsterDamage);
            }
            state.setPlayerDamagePerRound(playerDamage.clampMin(GameNumber.ZERO));
            state.setMonsterDamagePerRound(monsterDamage.clampMin(GameNumber.ZERO));

            // 玩家先手攻击
            state.setMonsterHp(state.getMonsterHp().minus(state.getPlayerDamagePerRound()));
            if (state.getMonsterHp().isNonPositive()) {
                break; // 击杀回合，怪物不反击
            }

            // 怪物反击，任一效果返回false（如眩晕）则跳过
            boolean monsterAttacks = true;
            for (BattleEffect effect : effects) {
                if (!effect.shouldMonsterAttack(state)) {
                    monsterAttacks = false;
                    break;
                }
            }
            if (monsterAttacks) {
                state.setPlayerHp(state.getPlayerHp().minus(state.getMonsterDamagePerRound()));
            }

            for (BattleEffect effect : effects) {
                effect.onRoundEnd(state);
            }
        }

        GameNumber totalDamage = player.hp().minus(state.getPlayerHp()).clampMin(GameNumber.ZERO);
        DamageRange range = classifyDamage(totalDamage, player.maxHp());
        return new BattleResult(totalDamage, range, state.getRound());
    }

    /**
     * 按伤害占玩家<b>生命上限</b>的比例分级。
     *
     * <p>以上限而非当前生命值为基准，怪物的伤害等级只在玩家属性（含生命上限）
     * 真正变化时才改变，不会因为掉血而让同一只怪物忽高忽低。
     * 本回合是否致命由 {@code BattleHandler} 用当前生命值单独判断。
     */
    public static DamageRange classifyDamage(GameNumber damage, GameNumber playerMaxHp) {
        double ratio = damage.dividedBy(playerMaxHp);
        if (ratio <= 0) return DamageRange.NONE;
        if (ratio <= 0.3) return DamageRange.LOW;
        if (ratio <= 0.6) return DamageRange.MEDIUM;
        if (ratio < 1.0) return DamageRange.HIGH;
        return DamageRange.DEATH;
    }
}
