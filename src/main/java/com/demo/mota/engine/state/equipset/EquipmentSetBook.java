package com.demo.mota.engine.state.equipset;

import com.demo.mota.engine.Item.Equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家的全部装备套装（与 {@code SkillPresetBook} 同构）。
 *
 * <p>本类只管「存了什么」，不校验装备是否在背包里、能不能进那个槽——
 * 那些规则在换装时由 {@code PlayerStateManager} 经 {@link EquipmentSwap} 把关。
 */
public final class EquipmentSetBook {

    private final List<EquipmentSet> sets;

    /**
     * @param setCount  套装数量
     * @param slotCount 每套的槽位数（= 装备槽数）
     */
    public EquipmentSetBook(int setCount, int slotCount) {
        this.sets = new ArrayList<>();
        for (int i = 0; i < Math.max(1, setCount); i++) {
            sets.add(new EquipmentSet(nameOf(i, setCount), slotCount));
        }
    }

    /** 与数字键对应：第 10 套叫「第0号套装」（键盘上 0 在 9 之后） */
    private static String nameOf(int index, int setCount) {
        int number = index + 1;
        return "第" + (number == 10 && setCount <= 10 ? 0 : number) + "号套装";
    }

    public int setCount() {
        return sets.size();
    }

    public EquipmentSet get(int index) {
        return sets.get(index);
    }

    public List<EquipmentSet> sets() {
        return Collections.unmodifiableList(sets);
    }

    /** @return 越界时返回 false，不保存 */
    public boolean save(int index, Equipment[] equipped) {
        if (index < 0 || index >= sets.size()) return false;
        sets.get(index).snapshot(equipped);
        return true;
    }
}
