package com.demo.mota.engine.factory.skill;

import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.SkillParams;
import com.demo.mota.engine.skill.SkillType;

@FunctionalInterface
public interface SkillCreator {
    Skill createSkill(String skillId, String skillName, SkillType skillType,
                      String description, String resourceId,
                      String effectId, SkillParams params);
}
