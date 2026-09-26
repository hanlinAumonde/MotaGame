package com.demo.mota.engine.skill.preset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家的全部技能预设 + 当前<b>就绪</b>的那一套（即「已就绪技能表」）。
 *
 * <p>同一时刻至多一套预设就绪，它排的主动技能会参与之后的每一场战斗（包括地图上的伤害预览），
 * 直到玩家切换或取消。{@code armedIndex = -1} 表示没有就绪的预设，战斗全程普攻。
 *
 * <p>本类只管「存了什么、哪套就绪」，不校验技能是否已拥有、是否是主动技能——
 * 那些规则依赖技能书，由 {@code PlayerStateManager} 在写入前把关。
 */
public final class SkillPresetBook {

    public static final int NONE = -1;

    private final List<SkillPreset> presets;
    private final int slotCount;
    private int armedIndex = NONE;

    /**
     * @param presetCount 预设套数
     * @param slotCount   每套的格数（= 最大回合 + 1，第 0 格为战前）
     */
    public SkillPresetBook(int presetCount, int slotCount) {
        this.slotCount = Math.max(1, slotCount);
        this.presets = new ArrayList<>();
        for (int i = 0; i < Math.max(1, presetCount); i++) {
            presets.add(new SkillPreset("预设" + (i + 1), this.slotCount));
        }
    }

    /** 用给定的预设（通常来自塔的规则配置）覆盖前几套 */
    public void load(List<SkillPreset> fixed) {
        for (int i = 0; i < Math.min(fixed.size(), presets.size()); i++) {
            presets.set(i, fixed.get(i));
        }
    }

    public int presetCount() {
        return presets.size();
    }

    public int slotCount() {
        return slotCount;
    }

    public SkillPreset get(int index) {
        return presets.get(index);
    }

    public List<SkillPreset> presets() {
        return Collections.unmodifiableList(presets);
    }

    public int getArmedIndex() {
        return armedIndex;
    }

    /** @return 当前就绪的预设，没有时为 null */
    public SkillPreset getArmed() {
        return armedIndex == NONE ? null : presets.get(armedIndex);
    }

    /**
     * 切换就绪预设：对已就绪的那一套再调用一次即为取消。
     *
     * @return 调用后的 {@link #getArmedIndex()}
     */
    public int toggleArmed(int index) {
        if (index < 0 || index >= presets.size()) return armedIndex;
        armedIndex = armedIndex == index ? NONE : index;
        return armedIndex;
    }

    /** 直接让某套预设就绪（已就绪时不变，不像 {@link #toggleArmed} 那样取消） */
    public void arm(int index) {
        if (index >= 0 && index < presets.size()) {
            armedIndex = index;
        }
    }

    public void disarm() {
        armedIndex = NONE;
    }

    public void assign(int presetIndex, int round, String skillId) {
        presets.get(presetIndex).setSlot(round, skillId);
    }

    public void clear(int presetIndex, int round) {
        presets.get(presetIndex).setSlot(round, null);
    }
}
