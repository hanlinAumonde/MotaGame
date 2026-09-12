package com.demo.mota.engine.skill.effect;

import com.demo.mota.engine.battle.BattleEffect;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.state.AbstractCharacterState;

import java.util.ArrayList;
import java.util.List;

/**
 * 把一场战斗中双方持有的技能，翻译成 {@code BattleSimulator} 需要的效果列表。
 *
 * <p>这是「技能」与「战斗」两个模块之间唯一的桥：战斗侧只认识 {@link BattleEffect}，
 * 技能侧只负责产出它。玩家与怪物走的是同一条路径（技能模型本就不区分持有者），
 * 因此角色技能系统落地后，这里不需要任何改动。
 *
 * <p>效果顺序为「玩家技能 → 怪物技能」，且按各自的持有顺序排列。
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
        if (!player.hasSkills() && !monster.hasSkills()) {
            return List.of();
        }
        List<BattleEffect> effects = new ArrayList<>();
        collect(effects, player, monster, BattleSide.PLAYER, map);
        collect(effects, monster, player, BattleSide.MONSTER, map);
        return effects;
    }

    private static void collect(List<BattleEffect> effects, AbstractCharacterState owner,
                                AbstractCharacterState opponent, BattleSide ownerSide, GameMap map) {
        for (Skill skill : owner.getSkills()) {
            SkillEffectContext context = new SkillEffectContext(
                    skill, skill.params(), ownerSide, owner, opponent, map);
            BattleEffect effect = SkillEffectRegistry.getInstance().create(context);
            if (effect != null) {
                effects.add(effect);
            }
        }
    }
}
