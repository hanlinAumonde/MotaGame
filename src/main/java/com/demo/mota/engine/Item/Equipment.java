package com.demo.mota.engine.Item;

import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.skill.Skill;

import java.util.List;
import java.util.Map;

/**
 * 装备：<b>只有穿上才生效</b>——属性加成计入 {@code PlayerStateManager.getEffectiveXxx}，
 * 附带的技能在穿上时授予、卸下时撤销（见 {@code PlayerStateManager.equip / unequip}）。
 *
 * <p>穿在哪个槽由玩家状态记录，装备本身不记，避免两边状态不一致。
 *
 * <p>{@code slotType} 是给「某类装备只能进特定槽位」预留的标记，当前不参与校验，
 * 见 {@code engine.rules.EquipSlotRule}。
 */
public final class Equipment extends Item {
    private final Map<StateType, GameNumber> stateEffectMap;
    private final List<Skill> skills;
    private final String slotType;

    public Equipment(String itemId, String itemName, String itemDescription,
                     long itemPrice, int itemCount,
                     boolean isStorable, boolean isConsumable,
                     Map<StateType, GameNumber> stateMap,
                     List<Skill> skills, String slotType) {
        super(itemId, itemName, itemDescription, itemPrice, itemCount, isStorable, isConsumable);
        this.stateEffectMap = stateMap == null ? Map.of() : Map.copyOf(stateMap);
        this.skills = skills == null ? List.of() : List.copyOf(skills);
        this.slotType = slotType == null || slotType.isBlank() ? null : slotType.trim();
    }

    public Map<StateType, GameNumber> getStateEffectMap() {
        return stateEffectMap;
    }

    /** 装备附带的技能（主动 / 被动均可），穿上时授予玩家 */
    public List<Skill> getSkills() {
        return skills;
    }

    public boolean hasSkills() {
        return !skills.isEmpty();
    }

    /** @return 槽位类型标记，未配置时为 null */
    public String getSlotType() {
        return slotType;
    }
}
