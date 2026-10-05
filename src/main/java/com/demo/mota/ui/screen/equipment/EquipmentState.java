package com.demo.mota.ui.screen.equipment;

import com.demo.mota.ui.MenuCommand;
import com.demo.mota.engine.Item.Equipment;
import com.demo.mota.engine.state.PlayerStateManager;

import java.util.List;

/**
 * 装备界面的状态机（按 Q 呼出）。与 {@code GameMenuState} 同样只管状态、不管绘制。
 *
 * <p>焦点在两处之间切换：
 * <ul>
 *   <li><b>装备列表</b>（左列，只列装备）：↑↓ 选择；确认 = 选中这件装备并<b>把焦点移到槽位网格</b>
 *       （此时还没有装备上）；→ 直接进入槽位网格（不带装备，用于查看 / 卸下）。</li>
 *   <li><b>槽位网格</b>：方向键移动；确认 = 有待穿装备时穿到此槽（替换原有；若正是它自己所在的槽则卸下），
 *       没有待穿装备时卸下此槽装备；返回 = 回到装备列表。</li>
 * </ul>
 * 带着装备进入网格时，光标落在它当前所在的槽，没穿上的落在第一个空槽（都没有则第一格）。
 *
 * <p><b>保存套装</b>（按住 W 再按数字键）：按下 W 时 {@link #armSave()} 给出提示，
 * 数字键 {@link #saveSet(int)} 把当前穿戴存进对应套装，松开 W 时 {@link #cancelSave()} 撤掉未用上的提示。
 * 「是否按住」由界面层的 {@code ChordKey} 判断，本类只管提示与保存。
 */
public class EquipmentState {

    /** 槽位网格每行的格数，渲染层按同一列数排布 */
    public static final int SLOT_COLUMNS = 3;

    public enum Focus { LIST, SLOTS }

    private final PlayerStateManager player;

    private boolean open;
    private Focus focus = Focus.LIST;
    private int listIndex;
    private int slotIndex;
    /** 从列表选中、等着挑槽位的装备；为 null 表示在槽位网格里只是浏览 / 卸下 */
    private Equipment pending;
    /** 按住了 W、还没按数字键（界面据此高亮保存提示） */
    private boolean saveArmed;
    private String notice = "";

    public EquipmentState(PlayerStateManager player) {
        this.player = player;
    }

    public void open() {
        this.open = true;
        this.focus = Focus.LIST;
        this.listIndex = 0;
        this.slotIndex = 0;
        this.pending = null;
        this.saveArmed = false;
        this.notice = "";
    }

    public void close() {
        this.open = false;
        this.pending = null;
        this.saveArmed = false;
    }

    public boolean isOpen() {
        return open;
    }

    /** 处理一次操作。穿脱造成的伤害变化由对局界面在回到游戏时统一重算 */
    public void handle(MenuCommand command) {
        if (!open || command == null) {
            return;
        }
        notice = "";
        saveArmed = false;
        if (focus == Focus.LIST) {
            handleList(command);
        } else {
            handleSlots(command);
        }
    }

    private void handleList(MenuCommand command) {
        List<Equipment> owned = getEquipments();
        switch (command) {
            case BACK, CLOSE -> close();
            case UP -> listIndex = wrap(listIndex - 1, owned.size());
            case DOWN -> listIndex = wrap(listIndex + 1, owned.size());
            case RIGHT -> {
                focus = Focus.SLOTS;
                pending = null;
            }
            case CONFIRM -> {
                Equipment selected = getSelectedEquipment();
                if (selected == null) return;
                // 只是选中，真正装备要等玩家在槽位网格里挑好位置
                pending = selected;
                focus = Focus.SLOTS;
                slotIndex = initialSlotFor(selected);
                notice = player.isEquipped(selected) ? "选择新槽位，选原槽位则卸下" : "选择要装备到的槽位";
            }
            default -> { }
        }
    }

    private int initialSlotFor(Equipment equipment) {
        int current = player.slotOf(equipment);
        if (current >= 0) return current;
        int empty = player.firstEmptySlot();
        return Math.max(empty, 0);
    }

    private void handleSlots(MenuCommand command) {
        int count = player.getEquipSlotCount();
        switch (command) {
            case CLOSE -> close();
            case BACK -> {
                focus = Focus.LIST;
                pending = null;
            }
            case LEFT -> slotIndex = wrap(slotIndex - 1, count);
            case RIGHT -> slotIndex = wrap(slotIndex + 1, count);
            case UP -> slotIndex = wrap(slotIndex - SLOT_COLUMNS, count);
            case DOWN -> slotIndex = wrap(slotIndex + SLOT_COLUMNS, count);
            case CONFIRM -> {
                if (pending != null) {
                    Equipment equipment = pending;
                    pending = null;
                    focus = Focus.LIST;
                    // 选的正是它自己所在的槽：视为卸下
                    if (player.slotOf(equipment) == slotIndex) {
                        player.unequip(slotIndex);
                        notice = "卸下了 " + equipment.getItemName();
                        return;
                    }
                    equip(equipment, slotIndex);
                    return;
                }
                Equipment removed = player.unequip(slotIndex);
                if (removed != null) {
                    notice = "卸下了 " + removed.getItemName();
                }
            }
            default -> { }
        }
    }

    private void equip(Equipment equipment, int slot) {
        notice = player.equip(equipment, slot)
                ? "装备了 " + equipment.getItemName()
                : equipment.getItemName() + " 不能装备在这个槽位";
    }

    // ==================== 保存套装 ====================

    /** 刚按下 W：提示玩家接着按数字键 */
    public void armSave() {
        if (!open) return;
        saveArmed = true;
        notice = "按数字键选择要保存到的套装";
    }

    public boolean isSaveArmed() {
        return saveArmed;
    }

    /** W+数字键：把当前穿戴存进第 setIndex 套（覆盖原有） */
    public void saveSet(int setIndex) {
        if (!open) return;
        saveArmed = false;
        notice = player.saveEquipmentSet(setIndex)
                ? "已保存为" + player.getEquipmentSetBook().get(setIndex).getName()
                : "没有这一套套装";
    }

    /** 松开 W 而没有保存：撤掉提示 */
    public void cancelSave() {
        if (saveArmed) {
            saveArmed = false;
            notice = "";
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

    public Focus getFocus() {
        return focus;
    }

    public List<Equipment> getEquipments() {
        return player.getEquipmentsOwned();
    }

    public int getSelectedListIndex() {
        return Math.min(listIndex, Math.max(0, getEquipments().size() - 1));
    }

    public Equipment getSelectedEquipment() {
        List<Equipment> owned = getEquipments();
        return owned.isEmpty() ? null : owned.get(getSelectedListIndex());
    }

    public int getSelectedSlot() {
        return slotIndex;
    }

    public Equipment getPending() {
        return pending;
    }

    /** 右侧详情栏展示的装备：列表焦点取选中项，槽位焦点取待穿装备或该槽上的装备 */
    public Equipment getFocusedEquipment() {
        if (focus == Focus.LIST) {
            return getSelectedEquipment();
        }
        return pending != null ? pending : player.getEquipped(slotIndex);
    }

    public String getNotice() {
        return notice;
    }
}
