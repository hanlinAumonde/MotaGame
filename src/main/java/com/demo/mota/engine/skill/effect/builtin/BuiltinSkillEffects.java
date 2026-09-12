package com.demo.mota.engine.skill.effect.builtin;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.skill.SkillParams;
import com.demo.mota.engine.skill.effect.BattleSide;
import com.demo.mota.engine.skill.effect.EffectTarget;
import com.demo.mota.engine.skill.effect.SkillEffectContext;
import com.demo.mota.engine.skill.effect.SkillEffectProvider;
import com.demo.mota.engine.state.AbstractCharacterState;

import java.util.function.BiConsumer;

/**
 * 内置技能效果的注册表。每个 effectId 描述一种效果<b>机制</b>，
 * 技能之间的差别由 {@code skillList.json} 里的 {@code params} 表达。
 *
 * <p>新增技能的常规路径是「复用这里的某个 effectId + 换一组参数」；
 * 只有当机制本身是全新的（例如「反弹伤害」「回合内回血」）时，
 * 才需要写一个新的 {@link SkillEffectProvider} 并注册。
 *
 * <h3>已内置的 effectId</h3>
 * <table border="1">
 *   <caption>内置效果与参数</caption>
 *   <tr><th>effectId</th><th>参数</th><th>说明</th></tr>
 *   <tr><td>{@code stat-modifier}</td>
 *       <td>target / stat / multiplier / flat</td>
 *       <td>战前把某一方的某项属性改为 {@code 原值 × multiplier + flat}</td></tr>
 *   <tr><td>{@code tag-count-stat-bonus}</td>
 *       <td>target / stat / tag / ratioPerUnit / flatPerUnit / excludeSelf / maxStacks</td>
 *       <td>按当前地图上带指定标签的怪物数量叠加属性加成</td></tr>
 *   <tr><td>{@code first-strike}</td>
 *       <td>target / times</td>
 *       <td>战斗开始前先手攻击若干次，该阶段不会被反击</td></tr>
 *   <tr><td>{@code periodic-damage-multiplier}</td>
 *       <td>target / interval / multiplier</td>
 *       <td>每满 interval 回合，该方伤害乘以 multiplier</td></tr>
 * </table>
 */
public final class BuiltinSkillEffects {

    public static final String STAT_MODIFIER = "stat-modifier";
    public static final String TAG_COUNT_STAT_BONUS = "tag-count-stat-bonus";
    public static final String FIRST_STRIKE = "first-strike";
    public static final String PERIODIC_DAMAGE_MULTIPLIER = "periodic-damage-multiplier";

    private BuiltinSkillEffects() {}

    /**
     * 由 {@code SkillEffectRegistry} 在初始化时调用，把内置效果逐一交给注册函数。
     */
    public static void registerAll(BiConsumer<String, SkillEffectProvider> registrar) {
        registrar.accept(STAT_MODIFIER, BuiltinSkillEffects::statModifier);
        registrar.accept(TAG_COUNT_STAT_BONUS, BuiltinSkillEffects::tagCountStatBonus);
        registrar.accept(FIRST_STRIKE, BuiltinSkillEffects::firstStrike);
        registrar.accept(PERIODIC_DAMAGE_MULTIPLIER, BuiltinSkillEffects::periodicDamageMultiplier);
    }

    private static StatModifierEffect statModifier(SkillEffectContext context) {
        SkillParams params = context.params();
        return new StatModifierEffect(
                targetSide(context),
                stat(params),
                params.getDouble("multiplier", 1.0),
                GameNumber.of(params.getLong("flat", 0L))
        );
    }

    /**
     * 「同伴越多越强」型效果：数到当前地图上带某个标签的怪物有几只，
     * 折算成一个固定倍率 / 加值后交给 {@link StatModifierEffect} 执行。
     *
     * <p>数数这件事依赖地图，只能在构造效果时做；战斗内部因此仍然是纯数值运算。
     * 地图缺席（怪物刚被工厂创建、还没放进地图）时返回 {@code null}，
     * 待 {@code BattleHandler.recalculateAllDamage} 带着地图重算即可。
     */
    private static StatModifierEffect tagCountStatBonus(SkillEffectContext context) {
        GameMap map = context.map();
        if (map == null) {
            return null;
        }
        SkillParams params = context.params();
        String tag = params.getString("tag", null);
        if (tag == null) {
            return null;
        }
        boolean excludeSelf = params.getBoolean("excludeSelf", true);
        AbstractCharacterState owner = context.owner();

        long count = map.getMonsters().values().stream()
                .filter(monster -> monster.hasTag(tag))
                .filter(monster -> !excludeSelf || monster != owner)
                .count();
        count = Math.min(count, params.getInt("maxStacks", Integer.MAX_VALUE));

        double multiplier = 1.0 + params.getDouble("ratioPerUnit", 0.0) * count;
        long flat = params.getLong("flatPerUnit", 0L) * count;
        return new StatModifierEffect(targetSide(context), stat(params), multiplier, GameNumber.of(flat));
    }

    private static FirstStrikeEffect firstStrike(SkillEffectContext context) {
        return new FirstStrikeEffect(targetSide(context), context.params().getInt("times", 1));
    }

    private static PeriodicDamageMultiplierEffect periodicDamageMultiplier(SkillEffectContext context) {
        SkillParams params = context.params();
        return new PeriodicDamageMultiplierEffect(
                targetSide(context),
                params.getInt("interval", 3),
                params.getDouble("multiplier", 1.0)
        );
    }

    /** {@code target} 参数缺省为 {@code SELF}，即「作用于技能持有者自己」 */
    private static BattleSide targetSide(SkillEffectContext context) {
        return context.params()
                .getEnum(EffectTarget.class, "target", EffectTarget.SELF)
                .resolve(context.ownerSide());
    }

    /** {@code stat} 参数接受 HP / MAX_HP / ATK / DEF，缺省为 ATK */
    private static StateType stat(SkillParams params) {
        return params.getEnum(StateType.class, "stat", StateType.ATK);
    }
}
