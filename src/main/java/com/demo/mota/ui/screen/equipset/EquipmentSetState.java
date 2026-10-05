package com.demo.mota.ui.screen.equipset;

import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.equipset.EquipSetResult;
import com.demo.mota.engine.state.equipset.EquipmentSet;
import com.demo.mota.ui.MenuCommand;

import java.util.List;

/**
 * 套装界面的状态机（按 A 呼出，参照 img_7）。只管状态、不管绘制。
 *
 * <p>套装排成<b>两列</b>：前一半在左列、后一半在右列（10 套时即 1~5 / 6~0，与 W+数字键的编号对应）。
 * ↑↓ 在列内移动（首尾绕回），←→ 在两列间跳；确认 = 换上这套并关闭界面（本界面不设数字快捷键，对局中直接按 W+数字键即可），
 * 换装结果的提示经 {@link #takeMessage()} 交给对局界面显示。未保存过的套装只提示、不关闭。
 */
public class EquipmentSetState {

    private final PlayerStateManager player;

    private boolean open;
    private int index;
    private String notice = "";
    /** 换装成功后留给对局界面的提示；取走即清空 */
    private String message;

    public EquipmentSetState(PlayerStateManager player) {
        this.player = player;
    }

    /** 打开时光标落在当前穿着的那一套上，没有则第一套 */
    public void open() {
        this.open = true;
        this.notice = "";
        this.message = null;
        this.index = 0;
        for (int i = 0; i < getSetCount(); i++) {
            if (player.isWearingSet(i)) {
                this.index = i;
                break;
            }
        }
    }

    public void close() {
        this.open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void handle(MenuCommand command) {
        if (!open || command == null) {
            return;
        }
        notice = "";
        int rows = getRows();
        int column = index / rows;
        int row = index % rows;
        switch (command) {
            case BACK, CLOSE -> close();
            case UP -> index = column * rows + wrap(row - 1, rowsIn(column));
            case DOWN -> index = column * rows + wrap(row + 1, rowsIn(column));
            case LEFT, RIGHT -> {
                int other = (column + 1) % 2;
                if (rowsIn(other) > 0) {
                    index = other * rows + Math.min(row, rowsIn(other) - 1);
                }
            }
            case CONFIRM -> apply(index);
            default -> { }
        }
    }

    private void apply(int setIndex) {
        EquipSetResult result = player.applyEquipmentSet(setIndex);
        String text = describe(player, setIndex, result);
        if (result.isOk()) {
            message = text;
            close();
        } else {
            notice = text;
        }
    }

    /** 换装结果给玩家看的一句话；套装界面与对局中的 W+数字键共用 */
    public static String describe(PlayerStateManager player, int setIndex, EquipSetResult result) {
        return switch (result.status()) {
            case OK -> "已切换到" + nameOf(player, setIndex)
                    + (result.skipped() > 0 ? "（" + result.skipped() + " 件无法装备）" : "");
            case NOT_SAVED -> nameOf(player, setIndex) + "尚未保存（装备界面 W+数字键 保存）";
            case OUT_OF_RANGE -> "没有这一套套装";
        };
    }

    private static String nameOf(PlayerStateManager player, int setIndex) {
        return player.getEquipmentSetBook().get(setIndex).getName();
    }

    /** 每列的行数：前一半放左列 */
    public int getRows() {
        return Math.max(1, (getSetCount() + 1) / 2);
    }

    private int rowsIn(int column) {
        int rows = getRows();
        return Math.max(0, Math.min(rows, getSetCount() - column * rows));
    }

    private static int wrap(int value, int size) {
        if (size <= 0) return 0;
        return ((value % size) + size) % size;
    }

    // ==================== 状态查询 ====================

    public PlayerStateManager getPlayer() {
        return player;
    }

    public List<EquipmentSet> getSets() {
        return player.getEquipmentSetBook().sets();
    }

    public int getSetCount() {
        return player.getEquipmentSetBook().setCount();
    }

    public int getSelectedIndex() {
        return index;
    }

    public String getNotice() {
        return notice;
    }

    /** 取走换装提示（没有换装时为 null） */
    public String takeMessage() {
        String taken = message;
        message = null;
        return taken;
    }
}
