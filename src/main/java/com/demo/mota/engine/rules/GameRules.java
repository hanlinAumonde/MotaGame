package com.demo.mota.engine.rules;

import java.util.List;

/**
 * 塔级规则配置（{@code assets/json/rules.json}）。
 *
 * <p>凡是「不同的塔可能想要不同答案」的设定都收在这里，而不是写死在代码里——
 * 技能预设能不能由玩家编辑、装备有几个槽、右侧栏显示哪些内容……
 * 将来的造塔器只需要产出这份文件。
 *
 * <p>任何字段缺省都回退到 {@link #DEFAULT} 中的对应值，因此配置文件可以只写想改的部分。
 * 由内容层读取，作为 {@code TowerContent} 的一部分。
 */
public record GameRules(SkillRules skill, EquipmentRules equipment, SidePanelRules sidePanel, UndoRules undo,
                        SaveRules save) {

    public static final GameRules DEFAULT = new GameRules(null, null, null, null, null);

    public GameRules {
        skill = skill == null ? SkillRules.DEFAULT : skill;
        equipment = equipment == null ? EquipmentRules.DEFAULT : equipment;
        sidePanel = sidePanel == null ? SidePanelRules.DEFAULT : sidePanel;
        undo = undo == null ? UndoRules.DEFAULT : undo;
        save = save == null ? SaveRules.DEFAULT : save;
    }

    /**
     * 技能规则。
     *
     * @param maxRound       预设可排到的最大回合（格子数 = maxRound + 1，第 0 格为战前）
     * @param presetCount    预设套数
     * @param presetEditable 玩家能否编辑预设；为 false 时技能界面只读，预设取自 {@code fixedPresets}
     * @param fixedPresets   塔作者预先写好的预设，按顺序覆盖前几套
     * @param hotkeys        激活预设的按键（JavaFX {@code KeyCode} 名），第 i 个对应第 i 套预设；
     *                       按住 D 再按：游戏中激活 / 停用该套，技能设置界面中切到该套编辑（同时激活）
     */
    public record SkillRules(Integer maxRound, Integer presetCount, Boolean presetEditable,
                             List<PresetData> fixedPresets, List<String> hotkeys) {

        /** 数字键 1~9、0 依次对应第 1~10 套预设 */
        public static final List<String> DEFAULT_HOTKEYS = List.of(
                "DIGIT1", "DIGIT2", "DIGIT3", "DIGIT4", "DIGIT5",
                "DIGIT6", "DIGIT7", "DIGIT8", "DIGIT9", "DIGIT0");

        public static final SkillRules DEFAULT = new SkillRules(null, null, null, null, null);

        public SkillRules {
            maxRound = maxRound == null || maxRound < 0 ? 10 : maxRound;
            presetCount = presetCount == null || presetCount < 1 ? 10 : presetCount;
            presetEditable = presetEditable == null || presetEditable;
            fixedPresets = fixedPresets == null ? List.of() : List.copyOf(fixedPresets);
            hotkeys = hotkeys == null ? DEFAULT_HOTKEYS : List.copyOf(hotkeys);
        }

        public int slotCount() {
            return maxRound + 1;
        }
    }

    /** @param slots 每回合的技能 id，下标 = 回合（0 为战前），空串 / null 为普攻 */
    public record PresetData(String name, List<String> slots) {}

    /**
     * @param slotCount  装备槽数量
     * @param setCount   装备套装数量
     * @param setHotkeys 套装对应的按键（JavaFX {@code KeyCode} 名），第 i 个对应第 i 套；
     *                   按住 W 再按：游戏中换上该套，装备界面中把当前穿戴存为该套
     */
    public record EquipmentRules(Integer slotCount, Integer setCount, List<String> setHotkeys) {

        public static final EquipmentRules DEFAULT = new EquipmentRules(null, null, null);

        public EquipmentRules {
            slotCount = slotCount == null || slotCount < 1 ? 6 : slotCount;
            setCount = setCount == null || setCount < 1 ? 10 : setCount;
            setHotkeys = setHotkeys == null ? SkillRules.DEFAULT_HOTKEYS : List.copyOf(setHotkeys);
        }
    }

    /** @param sections 右侧栏自上而下的分区 id，对应 {@code SidePanelSectionRegistry} 中的注册项 */
    public record SidePanelRules(List<String> sections) {

        public static final SidePanelRules DEFAULT = new SidePanelRules(null);

        public SidePanelRules {
            sections = sections == null ? List.of("skillCombo", "passiveSkills", "equipment") : List.copyOf(sections);
        }
    }

    /**
     * @param maxSteps 撤销（Backspace）最多能回退几步有意义的行动；0 = 本塔不允许撤销
     */
    public record UndoRules(Integer maxSteps) {

        public static final UndoRules DEFAULT = new UndoRules(null);

        public UndoRules {
            maxSteps = maxSteps == null || maxSteps < 0 ? 20 : maxSteps;
        }
    }

    /** @param slotCount 手动存档的档位数（另有一个自动存档） */
    public record SaveRules(Integer slotCount) {

        public static final SaveRules DEFAULT = new SaveRules(null);

        public SaveRules {
            slotCount = slotCount == null || slotCount < 1 ? 10 : slotCount;
        }
    }
}
