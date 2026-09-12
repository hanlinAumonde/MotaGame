package com.demo.mota.engine.skill.effect;

import com.demo.mota.engine.battle.BattleEffect;

/**
 * 技能效果的构造器：把一条技能配置 + 一次具体的战斗上下文，翻译成一个
 * {@link BattleEffect}。一个 provider 对应一种效果<b>机制</b>，
 * 不同参数的同一机制就是不同的技能。
 *
 * <p>这是技能系统对外的扩展点：向
 * {@link SkillEffectRegistry#register(String, SkillEffectProvider)} 注册一个新的
 * effectId 即可让新机制生效，战斗层与展示层都无需改动。将来支持外部脚本时，
 * 脚本加载器要做的也只是把脚本包装成本接口再注册进去。
 */
@FunctionalInterface
public interface SkillEffectProvider {

    /**
     * @return 本次战斗中该技能对应的效果；返回 {@code null} 表示此上下文下不产生效果
     *         （例如依赖地图的效果在没有地图时）
     */
    BattleEffect create(SkillEffectContext context);
}
