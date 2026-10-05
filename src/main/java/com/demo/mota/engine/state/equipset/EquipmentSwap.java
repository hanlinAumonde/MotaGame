package com.demo.mota.engine.state.equipset;

import com.demo.mota.engine.Item.Equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * 换上一套装备的<b>差异</b>：哪些件被换下、哪些件新穿上、最终各槽是什么。
 *
 * <p>之所以算差异而不是「全卸再全穿」：卸光会先把当前生命钳到无装备时的上限，
 * 再穿回来上限是回去了、血却回不去；同一件装备只是换了槽位时，附带技能的来源也不该被撤销再授予。
 * 本类只做计算、不碰玩家状态，由 {@code PlayerStateManager} 据此落地。
 *
 * @param result  换装后的各槽装备（下标即槽位，空槽 null）
 * @param removed 被换下的装备（需撤销附带技能）
 * @param added   新穿上的装备（需授予附带技能）
 * @param skipped 套装里因不在背包 / 不满足槽位规则而没有穿上的件数
 */
public record EquipmentSwap(Equipment[] result, List<Equipment> removed, List<Equipment> added, int skipped) {

    /**
     * @param current  当前各槽装备
     * @param target   套装各槽装备（长度可与 current 不同，多余部分忽略）
     * @param owned    该装备是否还在背包里
     * @param canEquip 该装备能否放进该槽位
     */
    public static EquipmentSwap plan(Equipment[] current, Equipment[] target,
                                     Predicate<Equipment> owned, BiPredicate<Equipment, Integer> canEquip) {
        Equipment[] result = new Equipment[current.length];
        Set<Equipment> placed = Collections.newSetFromMap(new IdentityHashMap<>());
        int skipped = 0;
        for (int slot = 0; slot < result.length && slot < target.length; slot++) {
            Equipment equipment = target[slot];
            if (equipment == null) continue;
            if (!owned.test(equipment) || !canEquip.test(equipment, slot) || !placed.add(equipment)) {
                skipped++;
                continue;
            }
            result[slot] = equipment;
        }

        Set<Equipment> before = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Equipment> removed = new ArrayList<>();
        for (Equipment equipment : current) {
            if (equipment == null) continue;
            before.add(equipment);
            if (!placed.contains(equipment)) removed.add(equipment);
        }
        List<Equipment> added = new ArrayList<>();
        for (Equipment equipment : result) {
            if (equipment != null && !before.contains(equipment)) added.add(equipment);
        }
        return new EquipmentSwap(result, List.copyOf(removed), List.copyOf(added), skipped);
    }
}
