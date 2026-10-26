package com.demo.mota.scene.saveload;

import com.demo.mota.engine.save.SaveSlotInfo;
import com.demo.mota.ui.MenuCommand;

import java.util.List;

/**
 * 存读档界面的状态机：档位列表 + 光标 + 提示，不依赖 JavaFX，也不碰磁盘。
 *
 * <p>确认键只回答「要对哪个档位动手」，真正的读写由 {@link SaveLoadSubScene} 交给会话服务完成，结果再经 {@link #setNotice} 写回来。
 * <ul>
 *   <li>存档：自动存档档位不可写（光标跳过）；已有内容的档位由场景弹对话框确认覆盖</li>
 *   <li>读档：所有档位都能选中，空档 / 损坏 / 版本不兼容的只给出提示</li>
 * </ul>
 */
public final class SaveLoadState {

    private SaveLoadMode mode = SaveLoadMode.LOAD;
    private List<SaveSlotInfo> slots = List.of();
    private int selected;
    /** 最近一次操作留给玩家的提示，下一次操作即清除 */
    private String notice;

    public void open(SaveLoadMode mode, List<SaveSlotInfo> slots) {
        this.mode = mode;
        this.slots = List.copyOf(slots);
        this.notice = null;
        this.selected = initialSelection();
    }

    /** 读写之后刷新档位（光标不动） */
    public void updateSlots(List<SaveSlotInfo> slots) {
        this.slots = List.copyOf(slots);
        if (selected >= this.slots.size()) {
            selected = initialSelection();
        }
    }

    /**
     * 处理一次操作。
     *
     * @return 确认后要动手的档位 id；其余情况为 {@code null}
     */
    public String handle(MenuCommand command) {
        if (command == null || slots.isEmpty()) return null;
        notice = null;
        switch (command) {
            case UP -> move(-1);
            case DOWN -> move(1);
            case CONFIRM -> {
                return confirm();
            }
            default -> { /* 返回由场景处理，其余操作本界面不响应 */ }
        }
        return null;
    }

    private String confirm() {
        SaveSlotInfo slot = getSelectedSlot();
        if (mode == SaveLoadMode.SAVE) {
            return slot.slotId();
        }
        if (slot.isLoadable()) {
            return slot.slotId();
        }
        notice = slot.isEmpty() ? slot.name() + " 还没有存档" : slot.name() + "：" + slot.status().getDisplayText();
        return null;
    }

    /** 循环移动并跳过不可选的档位 */
    private void move(int delta) {
        int size = slots.size();
        for (int step = 1; step <= size; step++) {
            int candidate = Math.floorMod(selected + delta * step, size);
            if (isSelectable(candidate)) {
                selected = candidate;
                return;
            }
        }
    }

    /** 读档时落在第一个能读的档位上（没有就停在第一个）；存档时落在第一个可写的档位上 */
    private int initialSelection() {
        if (mode == SaveLoadMode.LOAD) {
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).isLoadable()) return i;
            }
        }
        for (int i = 0; i < slots.size(); i++) {
            if (isSelectable(i)) return i;
        }
        return 0;
    }

    // ==================== 状态查询 ====================

    /** 存档时自动存档档位不可写 */
    public boolean isSelectable(int index) {
        return mode == SaveLoadMode.LOAD || !slots.get(index).isAuto();
    }

    public SaveLoadMode getMode() {
        return mode;
    }

    public List<SaveSlotInfo> getSlots() {
        return slots;
    }

    public int getSelectedIndex() {
        return selected;
    }

    public SaveSlotInfo getSelectedSlot() {
        return slots.get(selected);
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }
}
