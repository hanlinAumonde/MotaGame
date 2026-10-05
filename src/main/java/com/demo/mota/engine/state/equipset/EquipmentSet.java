package com.demo.mota.engine.state.equipset;

import com.demo.mota.engine.Item.Equipment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 一套保存下来的装备穿戴（「第N号套装」）：下标即槽位，空槽为 null。
 *
 * <p>存的是装备<b>实例</b>引用而不是 id——两件同名装备是两件东西，与 {@code SkillSource.FromEquipment} 的口径一致。
 * 从未保存过的套装 {@link #isSaved()} 为 false，界面据此画灰、切换时给出提示。
 */
public final class EquipmentSet {

    private final String name;
    private final Equipment[] slots;
    private boolean saved;

    public EquipmentSet(String name, int slotCount) {
        this.name = name;
        this.slots = new Equipment[Math.max(1, slotCount)];
    }

    public String getName() {
        return name;
    }

    public int slotCount() {
        return slots.length;
    }

    public boolean isSaved() {
        return saved;
    }

    /** 复制一份当前穿戴存下来；之后再穿脱不影响已存的套装。长度不一致时多余部分丢弃、不足补空 */
    void snapshot(Equipment[] equipped) {
        Arrays.fill(slots, null);
        System.arraycopy(equipped, 0, slots, 0, Math.min(equipped.length, slots.length));
        saved = true;
    }

    /** @return 该槽位存的装备，空槽 / 越界时为 null */
    public Equipment getSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < slots.length ? slots[slotIndex] : null;
    }

    public List<Equipment> slots() {
        return Collections.unmodifiableList(Arrays.asList(slots.clone()));
    }

    /** 与给定的穿戴逐槽完全一致（同一实例）时为 true；未保存的套装恒为 false */
    public boolean matches(Equipment[] equipped) {
        if (!saved) return false;
        for (int i = 0; i < Math.max(slots.length, equipped.length); i++) {
            Equipment mine = i < slots.length ? slots[i] : null;
            Equipment theirs = i < equipped.length ? equipped[i] : null;
            if (mine != theirs) return false;
        }
        return true;
    }
}
