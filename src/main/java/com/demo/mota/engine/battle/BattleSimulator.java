package com.demo.mota.engine.battle;

import com.demo.mota.engine.state.GameNumber;
import com.demo.mota.engine.state.monster.DamageRange;

import java.util.List;

public final class BattleSimulator {

    private BattleSimulator() {}

    public static BattleResult simulate(BattleSnapshot player, BattleSnapshot monster) {
        return simulate(player, monster, List.of());
    }

    public static BattleResult simulate(BattleSnapshot player, BattleSnapshot monster, List<BattleEffect> effects) {
        GameNumber playerDmgPerRound = player.atk().minus(monster.def()).clampMin(GameNumber.ZERO);

        if (playerDmgPerRound.isNonPositive()) {
            return BattleResult.cannotWin();
        }

        GameNumber monsterDmgPerRound = monster.atk().minus(player.def()).clampMin(GameNumber.ZERO);

        for (BattleEffect effect : effects) {
            playerDmgPerRound = effect.adjustPlayerDamage(playerDmgPerRound);
            monsterDmgPerRound = effect.adjustMonsterDamage(monsterDmgPerRound);
        }

        if (monsterDmgPerRound.isNonPositive()) {
            return BattleResult.noDamage();
        }

        GameNumber roundsToKill = monster.hp().dividedBy(playerDmgPerRound, false);
        GameNumber monsterAttackRounds = roundsToKill.minus(GameNumber.of(1));
        GameNumber totalDamage = monsterDmgPerRound.times(monsterAttackRounds);

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
