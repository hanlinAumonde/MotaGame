package com.demo.mota.engine.skill.effect;

import com.demo.mota.engine.battle.BattleEffect;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.skill.SkillCast;
import com.demo.mota.engine.skill.cost.SkillCostRegistry;
import com.demo.mota.engine.state.AbstractCharacterState;

import java.util.ArrayList;
import java.util.List;

/**
 * 把一场战斗中双方的技能释放，翻译成 {@code BattleSimulator} 需要的效果列表。
 *
 * <p>这是「技能」与「战斗」两个模块之间唯一的桥：战斗侧只认识 {@link BattleEffect}，
 * 技能侧只负责产出它。取数口是 {@link AbstractCharacterState#getBattleSkillCasts()}——
 * 怪物把全部技能当被动整场生效，玩家则是「开启的被动 + 就绪预设里排定回合的主动」，
 * 两者在这里走的是同一条路径。
 *
 * <p>主动技能在这里过一遍消耗检查：付不起的释放直接跳过（该回合按普攻），
 * 因此地图上的伤害预览与实际结算永远一致。
 *
 * <p>效果顺序为「玩家 → 怪物」，且按各自的释放列表顺序排列。
 * 战前属性调整是按此顺序依次折叠的，顺序会影响多个百分比增减益的复合结果。
 */
public final class SkillEffectResolver {

    private SkillEffectResolver() {}

    /**
     * @param player 玩家状态
     * @param monster 参战怪物
     * @param map    战斗发生的地图，允许为 null（怪物尚未落图时）
     */
    public static List<BattleEffect> resolve(AbstractCharacterState player,
                                             AbstractCharacterState monster,
                                             GameMap map) {
        List<SkillCast> playerCasts = player.getBattleSkillCasts();
        List<SkillCast> monsterCasts = monster.getBattleSkillCasts();
        if (playerCasts.isEmpty() && monsterCasts.isEmpty()) {
            return List.of();
        }
        List<BattleEffect> effects = new ArrayList<>();
        collect(effects, playerCasts, player, monster, BattleSide.PLAYER, map);
        collect(effects, monsterCasts, monster, player, BattleSide.MONSTER, map);
        return effects;
    }

    /**
     * 持有者本场战斗中<b>真正会生效</b>的技能释放：{@code getBattleSkillCasts} 再滤掉付不起消耗的。
     * 战后结算消耗（{@code BattleHandler}）用它，保证扣的和算进伤害的是同一批。
     */
    public static List<SkillCast> affordableCasts(AbstractCharacterState owner) {
        SkillCostRegistry costs = SkillCostRegistry.getInstance();
        return owner.getBattleSkillCasts().stream()
                .filter(cast -> costs.canAfford(cast, owner))
                .toList();
    }

    private static void collect(List<BattleEffect> effects, List<SkillCast> casts,
                                AbstractCharacterState owner, AbstractCharacterState opponent,
                                BattleSide ownerSide, GameMap map) {
        SkillCostRegistry costs = SkillCostRegistry.getInstance();
        for (SkillCast cast : casts) {
            if (!costs.canAfford(cast, owner)) {
                continue;
            }
            SkillEffectContext context = new SkillEffectContext(
                    cast.skill(), cast.skill().params(), ownerSide, owner, opponent, map, cast.castRound());
            BattleEffect effect = SkillEffectRegistry.getInstance().create(context);
            if (effect != null) {
                effects.add(effect);
            }
        }
    }
}
