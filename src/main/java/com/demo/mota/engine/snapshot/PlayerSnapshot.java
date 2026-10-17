package com.demo.mota.engine.snapshot;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.StateType;

import java.util.List;
import java.util.Map;

/**
 * 玩家状态快照。
 *
 * <p>数值一律存成十进制字符串（{@code GameNumber} 底层是 {@code BigInteger}，转 long 会截断）。
 * 集合字段缺省（旧版本存档里没有）时按空处理，见紧凑构造。
 *
 * <p><b>装备的身份</b>：装备没有实例 id，两件同名装备只靠对象同一性区分（技能来源 / 套装 / 槽位都是这个口径）。
 * 快照里以「在 {@link #equipmentsOwned} 中的下标」作为实例句柄：{@link #equipped}、{@link EquipmentSetSnapshot#slots()}、
 * {@link OwnedSkillSnapshot#equipmentSources()} 都写下标，恢复时按下标指回同一个新建的实例，同一性语义完整保留。
 *
 * @param baseStats        基础属性（不含装备加成）
 * @param levelNumber      等级
 * @param experience       当前等级内已累计的经验
 * @param keyCounts        当前钥匙组各颜色的数量
 * @param equipmentsOwned  拿到过的全部装备 id，顺序即实例句柄
 * @param equipped         各槽位穿着的装备下标，空槽为 -1
 * @param skills           技能书条目，按获得顺序（顺序决定被动效果的折叠顺序）
 * @param armedIndex       激活的预设下标，-1 为未激活
 * @param genericItems     通用道具，每个实例一条
 * @param auxiliaryItems   辅助道具 id
 */
public record PlayerSnapshot(
        Map<StateType, String> baseStats,
        Direction direction,
        int levelNumber,
        String experience,
        long gold,
        Map<KeyColor, Integer> keyCounts,
        List<String> equipmentsOwned,
        List<Integer> equipped,
        List<EquipmentSetSnapshot> equipmentSets,
        List<OwnedSkillSnapshot> skills,
        List<PresetSnapshot> presets,
        int armedIndex,
        List<ItemStack> genericItems,
        List<String> auxiliaryItems) {

    /** 「空槽」在下标数组里的写法 */
    public static final int EMPTY_SLOT = -1;

    public PlayerSnapshot {
        baseStats = baseStats == null ? Map.of() : baseStats;
        keyCounts = keyCounts == null ? Map.of() : keyCounts;
        equipmentsOwned = orEmpty(equipmentsOwned);
        equipped = orEmpty(equipped);
        equipmentSets = orEmpty(equipmentSets);
        skills = orEmpty(skills);
        presets = orEmpty(presets);
        genericItems = orEmpty(genericItems);
        auxiliaryItems = orEmpty(auxiliaryItems);
    }

    static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    /** 一套装备套装：是否保存过 + 各槽装备下标（空槽 -1） */
    public record EquipmentSetSnapshot(boolean saved, List<Integer> slots) {
        public EquipmentSetSnapshot {
            slots = orEmpty(slots);
        }
    }

    /**
     * 技能书中的一条。
     *
     * @param learned          是否有「习得」来源
     * @param equipmentSources 提供该技能的装备下标
     * @param enabled          被动开关
     */
    public record OwnedSkillSnapshot(String skillId, boolean learned, List<Integer> equipmentSources, boolean enabled) {
        public OwnedSkillSnapshot {
            equipmentSources = orEmpty(equipmentSources);
        }
    }

    /** 一套技能预设：名称 + 各回合格的技能 id（普攻为 null） */
    public record PresetSnapshot(String name, List<String> slots) {
        public PresetSnapshot {
            slots = orEmpty(slots);
        }
    }

    /** 一个道具实例：id + 数量 */
    public record ItemStack(String itemId, int count) {
    }
}
