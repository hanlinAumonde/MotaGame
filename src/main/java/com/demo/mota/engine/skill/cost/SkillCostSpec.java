package com.demo.mota.engine.skill.cost;

import com.demo.mota.engine.skill.SkillParams;

/**
 * 技能配置中的消耗声明：{@code "cost": { "costId": "...", "params": {...} }}。
 *
 * <p>与 {@code effectId + params} 同一套思路——配置只说「用哪种消耗机制、配什么参数」，
 * 具体怎么扣（魔法、道具、生命……）由 {@link SkillCostRegistry} 里注册的
 * {@link SkillCostHandler} 决定。未声明消耗的技能取 {@link #NONE}。
 *
 * @param costId 消耗机制标识，对应 {@link SkillCostRegistry} 中注册的 handler
 * @param params 消耗参数，永不为 null
 */
public record SkillCostSpec(String costId, SkillParams params) {

    public static final String NONE_ID = "none";
    public static final SkillCostSpec NONE = new SkillCostSpec(NONE_ID, SkillParams.EMPTY);

    public SkillCostSpec {
        costId = costId == null || costId.isBlank() ? NONE_ID : costId.trim();
        params = params == null ? SkillParams.EMPTY : params;
    }

    public boolean isFree() {
        return NONE_ID.equals(costId);
    }
}
