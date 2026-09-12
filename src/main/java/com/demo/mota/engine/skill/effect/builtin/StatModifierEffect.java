package com.demo.mota.engine.skill.effect.builtin;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.battle.BattleEffect;
import com.demo.mota.engine.battle.BattleSnapshot;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.skill.effect.BattleSide;

/**
 * 战前属性增减益：把某一方的某项属性改成 {@code 原值 × multiplier + flatDelta}（结果不低于 0）。
 *
 * <p>「战斗期间攻击力降低 10%」「生命提升 20%」「防御 +5」这类效果全部落在这里——
 * 倍率与加值都是参数，因此绝大多数数值型技能不需要新的效果实现，
 * 改 {@code skillList.json} 即可。倍率由调用方算好传入，
 * 像「每有一只同类就 +20%」这种依赖地图的倍率在 {@code Provider} 侧折算。
 */
public final class StatModifierEffect implements BattleEffect {

    private final BattleSide target;
    private final StateType stat;
    private final double multiplier;
    private final GameNumber flatDelta;

    public StatModifierEffect(BattleSide target, StateType stat, double multiplier, GameNumber flatDelta) {
        this.target = target;
        this.stat = stat;
        this.multiplier = multiplier;
        this.flatDelta = flatDelta == null ? GameNumber.ZERO : flatDelta;
    }

    @Override
    public BattleSnapshot adjustPlayerStats(BattleSnapshot player, BattleSnapshot monster) {
        return target == BattleSide.PLAYER ? apply(player) : player;
    }

    @Override
    public BattleSnapshot adjustMonsterStats(BattleSnapshot monster, BattleSnapshot player) {
        return target == BattleSide.MONSTER ? apply(monster) : monster;
    }

    private BattleSnapshot apply(BattleSnapshot snapshot) {
        return switch (stat) {
            case HP -> {
                GameNumber hp = modify(snapshot.hp());
                // 怪物没有独立的生命上限概念（快照里 maxHp 恒等于 hp），改血量时一并同步；
                // 玩家的上限是独立属性，只能由 MAX_HP 显式修改，否则会让伤害分级的基准漂移
                yield target == BattleSide.MONSTER ? snapshot.withHp(hp).withMaxHp(hp) : snapshot.withHp(hp);
            }
            case MAX_HP -> snapshot.withMaxHp(modify(snapshot.maxHp()));
            case ATK -> snapshot.withAtk(modify(snapshot.atk()));
            case DEF -> snapshot.withDef(modify(snapshot.def()));
        };
    }

    private GameNumber modify(GameNumber base) {
        return base.scaledBy(multiplier).plus(flatDelta).clampMin(GameNumber.ZERO);
    }
}
