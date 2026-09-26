package com.demo.mota.engine.skill.cost;

import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.SkillParams;
import com.demo.mota.engine.state.AbstractCharacterState;

/**
 * 判断 / 支付一次技能消耗时可用的上下文。
 *
 * @param skill     被释放的技能
 * @param params    技能配置中 {@code cost.params}，永不为 null
 * @param owner     释放者（消耗从它身上扣）
 * @param castRound 释放回合（0 = 战前）
 */
public record SkillCostContext(Skill skill, SkillParams params,
                               AbstractCharacterState owner, int castRound) {
}
