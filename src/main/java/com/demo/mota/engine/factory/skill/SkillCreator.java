package com.demo.mota.engine.factory.skill;

import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.SkillParams;
import com.demo.mota.engine.skill.SkillType;
import com.demo.mota.engine.skill.cost.SkillCostSpec;

@FunctionalInterface
public interface SkillCreator {
    Skill createSkill(String skillId, String skillName, SkillType skillType,
                      String description, String resourceId,
                      String effectId, SkillParams params, SkillCostSpec cost, int maxCasts);
}
