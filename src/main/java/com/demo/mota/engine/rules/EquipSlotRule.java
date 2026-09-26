package com.demo.mota.engine.rules;

import com.demo.mota.engine.Item.Equipment;

/**
 * 装备能否放进某个槽位的规则。
 *
 * <p>当前默认 {@link #ALLOW_ALL}：任何装备可放任何槽。将来要做「武器只能进武器槽」时，
 * 给装备配 {@code slotType}、实现一条按槽位类型比对的规则，
 * 再经 {@code PlayerStateManager.setEquipSlotRule} 换上即可，穿脱逻辑与界面都不必改。
 */
@FunctionalInterface
public interface EquipSlotRule {

    EquipSlotRule ALLOW_ALL = (equipment, slotIndex) -> true;

    boolean canEquip(Equipment equipment, int slotIndex);
}
