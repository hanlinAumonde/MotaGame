package com.demo.mota.ui.screen.skill;

import com.demo.mota.ui.MenuCommand;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.preset.PresetEditResult;
import com.demo.mota.engine.skill.preset.SkillPreset;
import com.demo.mota.engine.skill.preset.SkillPresetBook;
import com.demo.mota.engine.state.PlayerStateManager;

import java.util.List;

/**
 * 技能设置界面的状态机（按 D 呼出）。与 {@code GameMenuState} 同样只管状态、不管绘制。
 *
 * <p>界面分上下两块：上面是已拥有的技能列表（{@link #UP} / {@link #DOWN} 选择），
 * 下面是正在编辑的那套预设的回合格（{@link #LEFT} / {@link #RIGHT} 选择）。
 * <p><b>有激活的预设时，界面上显示、编辑的就是它</b>；游戏中停用了技能组（全程普攻）时，
 * 显示上次编辑的那套并标明「当前未激活」，往里放技能即重新激活它：
 * <ul>
 *   <li>{@link #selectPreset}（D+数字键）：切到对应预设，同时激活它（这里不做停用——界面是用来编辑的）；</li>
 *   <li>{@link MenuCommand#CONFIRM}：把选中的主动技能排进当前回合格，光标右移一格便于连续编排；
 *       技能在本预设中已排满每场可释放次数时拒绝；</li>
 *   <li>{@link MenuCommand#CLEAR}：当前回合格清回普攻（卸下该技能）；</li>
 *   <li>{@link MenuCommand#BACK} / {@link MenuCommand#CLOSE}：关闭。</li>
 * </ul>
 * 塔规则不允许编辑预设时，确认与清除不生效，界面只供查看（仍可用 D+数字键切换）。
 */
public class SkillSetupState {
    private final PlayerStateManager player;

    private boolean open;
    private int skillIndex;
    private int roundIndex;
    /** 上次编辑的预设；没有激活的预设时界面显示这一套 */
    private int lastEditedPreset;
    /** 最近一次操作的提示（例如「被动技能无需编排」），渲染层显示在组合栏标题旁 */
    private String notice = "";

    public SkillSetupState(PlayerStateManager player) {
        this.player = player;
    }

    public void open() {
        this.open = true;
        this.skillIndex = 0;
        this.roundIndex = 1;
        this.notice = "";
        int armed = player.getPresetBook().getArmedIndex();
        if (armed != SkillPresetBook.NONE) {
            this.lastEditedPreset = armed;
        }
    }

    /**
     * 切到第 {@code presetIndex} 套预设（同时激活）。
     *
     * @return 是否切换成功（下标越界时 false）
     */
    public boolean selectPreset(int presetIndex) {
        if (!open) return false;
        notice = "";
        if (!player.activatePreset(presetIndex)) return false;
        lastEditedPreset = presetIndex;
        return true;
    }

    public void close() {
        this.open = false;
    }

    public boolean isOpen() {
        return open;
    }

    /** 处理一次操作。预设改动造成的伤害变化由对局界面在回到游戏时统一重算 */
    public void handle(MenuCommand command) {
        if (!open || command == null) {
            return;
        }
        notice = "";
        SkillPresetBook book = player.getPresetBook();
        switch (command) {
            case BACK, CLOSE -> close();
            case UP -> skillIndex = wrap(skillIndex - 1, getSkills().size());
            case DOWN -> skillIndex = wrap(skillIndex + 1, getSkills().size());
            case LEFT -> roundIndex = wrap(roundIndex - 1, book.slotCount());
            case RIGHT -> roundIndex = wrap(roundIndex + 1, book.slotCount());
            case CONFIRM -> assignSelected();
            case CLEAR -> {
                if (getEditingPreset().getSlot(roundIndex) != null) {
                    notice = player.clearPresetSlot(getEditingPresetIndex(), roundIndex).getMessage();
                }
            }
            default -> { }
        }
    }

    /** 放进当前激活的预设，光标右移一格便于连续编排 */
    private void assignSelected() {
        Skill skill = getSelectedSkill();
        if (skill == null) {
            return;
        }
        int presetIndex = getEditingPresetIndex();
        PresetEditResult result = player.assignPresetSlot(presetIndex, roundIndex, skill.skillId());
        lastEditedPreset = presetIndex;
        notice = result.getMessage();
        if (result.isOk()) {
            roundIndex = wrap(roundIndex + 1, player.getPresetBook().slotCount());
        }
    }

    private static int wrap(int index, int size) {
        if (size <= 0) return 0;
        return ((index % size) + size) % size;
    }

    // ==================== 状态查询 ====================

    public PlayerStateManager getPlayer() {
        return player;
    }

    public List<Skill> getSkills() {
        return player.getSkills();
    }

    public int getSelectedSkillIndex() {
        return Math.min(skillIndex, Math.max(0, getSkills().size() - 1));
    }

    public Skill getSelectedSkill() {
        List<Skill> skills = getSkills();
        return skills.isEmpty() ? null : skills.get(getSelectedSkillIndex());
    }

    public int getSelectedRound() {
        return roundIndex;
    }

    /** 正在编辑的预设：有激活的就是它，没有则是上次编辑的那套 */
    public int getEditingPresetIndex() {
        int armed = player.getPresetBook().getArmedIndex();
        return armed == SkillPresetBook.NONE ? lastEditedPreset : armed;
    }

    /** 正在编辑的预设是否处于激活状态（游戏中停用了技能组时为 false） */
    public boolean isEditingPresetActive() {
        return player.getPresetBook().getArmedIndex() != SkillPresetBook.NONE;
    }

    public SkillPreset getEditingPreset() {
        return player.getPresetBook().get(getEditingPresetIndex());
    }

    public String getNotice() {
        return notice;
    }
}
