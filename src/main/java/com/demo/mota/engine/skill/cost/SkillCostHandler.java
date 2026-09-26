package com.demo.mota.engine.skill.cost;

/**
 * 一种技能消耗<b>机制</b>（魔法、道具、生命、能量……），与 {@code SkillEffectProvider} 对位。
 *
 * <p>调用约定：
 * <ul>
 *   <li>{@link #canAfford}：解析战斗技能时调用，返回 false 的释放按普攻处理。
 *       伤害预览与实际结算都走这一步，所以两者始终一致；</li>
 *   <li>{@link #pay}：战斗胜利后，只对<b>实际打到了</b>的回合调用（回合数不够、没来得及放的不扣）。</li>
 * </ul>
 */
public interface SkillCostHandler {

    boolean canAfford(SkillCostContext context);

    void pay(SkillCostContext context);
}
