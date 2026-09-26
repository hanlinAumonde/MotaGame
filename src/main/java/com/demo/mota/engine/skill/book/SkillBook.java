package com.demo.mota.engine.skill.book;

import com.demo.mota.engine.skill.Skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家的技能书：记录每个已拥有技能的<b>来源</b>与<b>开关状态</b>。
 *
 * <p>怪物的技能是配置带入的固定列表，不需要这些，仍由 {@code AbstractCharacterState} 直接持有；
 * 玩家的技能会随升级、穿脱装备而增减，因此单独用本类管理。
 *
 * <p>按获得顺序排列（{@link LinkedHashMap}），这个顺序同时决定了被动技能在战斗效果列表中的顺序。
 */
public final class SkillBook {

    private final Map<String, OwnedSkill> entries = new LinkedHashMap<>();

    /**
     * 以某个来源授予技能；已拥有时只追加来源。
     *
     * @return 这次调用是否让玩家<b>新</b>获得了这个技能
     */
    public boolean grant(Skill skill, SkillSource source) {
        if (skill == null || source == null) return false;
        OwnedSkill owned = entries.get(skill.skillId());
        boolean isNew = owned == null;
        if (isNew) {
            owned = new OwnedSkill(skill);
            entries.put(skill.skillId(), owned);
        }
        owned.addSource(source);
        return isNew;
    }

    /**
     * 撤销某个来源授予的<b>全部</b>技能；技能的来源清空时才真正移除。
     *
     * @return 因此被真正移除的技能 id
     */
    public List<String> revoke(SkillSource source) {
        List<String> removed = new ArrayList<>();
        entries.values().removeIf(owned -> {
            if (owned.removeSource(source) && owned.hasNoSource()) {
                removed.add(owned.skill().skillId());
                return true;
            }
            return false;
        });
        return removed;
    }

    /** 撤销某个来源对<b>单个</b>技能的授予 */
    public boolean revoke(String skillId, SkillSource source) {
        OwnedSkill owned = entries.get(skillId);
        if (owned == null || !owned.removeSource(source)) return false;
        if (owned.hasNoSource()) {
            entries.remove(skillId);
        }
        return true;
    }

    public boolean has(String skillId) {
        return skillId != null && entries.containsKey(skillId);
    }

    /** @return 对应条目，未拥有时为 null */
    public OwnedSkill get(String skillId) {
        return skillId == null ? null : entries.get(skillId);
    }

    public Collection<OwnedSkill> entries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public List<Skill> skills() {
        return entries.values().stream().map(OwnedSkill::skill).toList();
    }

    public List<Skill> activeSkills() {
        return entries.values().stream().map(OwnedSkill::skill).filter(Skill::isActive).toList();
    }

    public List<Skill> passiveSkills() {
        return entries.values().stream().map(OwnedSkill::skill).filter(Skill::isPassive).toList();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * 开关被动技能。目前 UI 层不提供入口、默认全部开启，只把接口留出来。
     *
     * @return 技能存在且为被动技能时返回 true
     */
    public boolean setPassiveEnabled(String skillId, boolean enabled) {
        OwnedSkill owned = entries.get(skillId);
        if (owned == null || !owned.skill().isPassive()) return false;
        owned.setEnabled(enabled);
        return true;
    }
}
