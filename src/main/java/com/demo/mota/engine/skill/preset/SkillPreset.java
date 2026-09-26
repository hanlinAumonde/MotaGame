package com.demo.mota.engine.skill.preset;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 一套主动技能的释放编排：第 {@code r} 格写的技能在第 {@code r} 回合释放。
 *
 * <p>第 0 格是<b>战前</b>（进入第一回合之前），其余格子对应回合 1..N；
 * 格子为 {@code null} 表示该回合普攻（第 0 格为空则战前什么也不做）。
 * 预设里只存技能 id，不存技能本体——引用的技能暂时不再拥有时（例如装备卸下了）
 * 解析阶段会跳过它，但预设本身不动，重新获得后自动恢复。
 */
public final class SkillPreset {
    private String name;
    private final String[] slots;

    public SkillPreset(String name, int slotCount) {
        this.name = name;
        this.slots = new String[Math.max(1, slotCount)];
    }

    /** 从配置构造（规则里的固定预设）；超出格数的部分丢弃，空串视为普攻 */
    public static SkillPreset of(String name, int slotCount, List<String> slotIds) {
        SkillPreset preset = new SkillPreset(name, slotCount);
        if (slotIds != null) {
            for (int i = 0; i < Math.min(slotIds.size(), preset.slots.length); i++) {
                String id = slotIds.get(i);
                preset.slots[i] = id == null || id.isBlank() ? null : id.trim();
            }
        }
        return preset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int slotCount() {
        return slots.length;
    }

    /** @return 该回合排的技能 id，普攻 / 越界时为 null */
    public String getSlot(int round) {
        return round >= 0 && round < slots.length ? slots[round] : null;
    }

    void setSlot(int round, String skillId) {
        if (round >= 0 && round < slots.length) {
            slots[round] = skillId;
        }
    }

    /** 该技能在本预设中排了几次，不计 {@code excludeRound} 那一格（传 -1 则全部计入） */
    public int countOf(String skillId, int excludeRound) {
        int count = 0;
        for (int round = 0; round < slots.length; round++) {
            if (round != excludeRound && skillId != null && skillId.equals(slots[round])) {
                count++;
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return Arrays.stream(slots).allMatch(Objects::isNull);
    }

    public List<String> slots() {
        return Arrays.asList(slots.clone());
    }
}
