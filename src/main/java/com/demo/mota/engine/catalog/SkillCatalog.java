package com.demo.mota.engine.catalog;

import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.SkillParams;
import com.demo.mota.engine.skill.SkillType;
import com.demo.mota.engine.skill.cost.SkillCostSpec;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能目录：按 id 产出 {@link Skill}。{@code Skill} 是不可变值对象，因此每个 id 只造一次。
 *
 * <p><b>不校验 effectId 是否已注册</b>：未注册的技能仍能正常展示，只是不产生战斗效果（见 {@code SkillEffectRegistry}）。
 */
public final class SkillCatalog extends Catalog<SkillDefinition> {

    private final Map<String, Skill> skills = new LinkedHashMap<>();

    public SkillCatalog(Collection<SkillDefinition> definitions) {
        super("技能", definitions, SkillDefinition::id);
        for (SkillDefinition definition : definitions) {
            skills.put(definition.id(), toSkill(definition));
        }
    }

    /** @throws IllegalArgumentException 技能表里没有这个 id */
    public Skill create(String id) {
        require(id);
        return skills.get(id);
    }

    /** 不存在时为 {@code null}（快照恢复、预设引用了已删除的技能时用） */
    public Skill findSkill(String id) {
        return skills.get(id);
    }

    /** 按 id 列表批量取技能；列表为空 / 为 null 时返回空列表 */
    public List<Skill> createAll(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().map(this::create).toList();
    }

    @Override
    protected String spriteOf(SkillDefinition definition) {
        return definition.sprite();
    }

    private static Skill toSkill(SkillDefinition d) {
        SkillCostSpec cost = d.cost() == null
                ? SkillCostSpec.NONE
                : new SkillCostSpec(d.cost().costId(), new SkillParams(d.cost().params()));
        return new Skill(d.id(), d.name(), SkillType.fromString(d.skillType()), d.description(), d.sprite(),
                d.effectId(), new SkillParams(d.params()), cost,
                d.maxCasts() == null ? Skill.DEFAULT_MAX_CASTS : d.maxCasts());
    }
}
