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
 */
public final class BattleSimulator {

    /** 回合数上限，防止效果组合导致无法结束的战斗 */
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
        if (baseMonsterDamage.isNonPositive()) {
            return BattleResult.noDamage();
        }

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
        DamageRange range = classifyDamage(totalDamage, player.hp());
        return new BattleResult(totalDamage, range);
    }

    public static DamageRange classifyDamage(GameNumber damage, GameNumber playerHp) {
        double ratio = damage.dividedBy(playerHp);
        if (ratio <= 0) return DamageRange.NONE;
        if (ratio <= 0.3) return DamageRange.LOW;
        if (ratio <= 0.6) return DamageRange.MEDIUM;
        if (ratio < 1.0) return DamageRange.HIGH;
        return DamageRange.DEATH;
    }
}
