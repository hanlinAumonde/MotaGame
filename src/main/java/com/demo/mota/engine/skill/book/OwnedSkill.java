package com.demo.mota.engine.skill.book;

import com.demo.mota.engine.skill.Skill;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 技能书中的一条：技能本体 + 来源集合 + 开关状态。
 *
 * <p>{@code enabled} 只对被动技能有意义：关掉的被动技能不参与战斗，但仍然留在技能书里。
 * 主动技能是否生效由预设决定，与本开关无关。
 */
public final class OwnedSkill {
    private final Skill skill;
    private final Set<SkillSource> sources = new LinkedHashSet<>();
    private boolean enabled = true;

    OwnedSkill(Skill skill) {
        this.skill = skill;
    }

    public Skill skill() {
        return skill;
    }

    public Set<SkillSource> sources() {
        return Collections.unmodifiableSet(sources);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** 是否来自装备（任何一件） */
    public boolean isFromEquipment() {
        return sources.stream().anyMatch(source -> source instanceof SkillSource.FromEquipment);
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    boolean addSource(SkillSource source) {
        return sources.add(source);
    }

    boolean removeSource(SkillSource source) {
        return sources.remove(source);
    }

    boolean hasNoSource() {
        return sources.isEmpty();
    }
}
