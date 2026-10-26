package com.demo.mota.scene.saveload;

import com.demo.mota.engine.save.SaveMeta;
import com.demo.mota.engine.save.SaveSlotInfo;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.ui.kit.Palette;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * 存读档界面（只读 {@link SaveLoadState}）：标题 + 一列档位 + 底部提示。
 * 每行「档位名 + 摘要（区域 · 楼层 / 等级 / 生命）+ 右侧保存时间」；空档、损坏、版本不兼容的显示状态文字，
 * 存档模式下不可写的自动存档画灰。档位多到放不下时随光标滚动。
 */
public class SaveLoadView {

    private static final double WIDTH = MotaLayout.WIDTH;
    private static final double HEIGHT = MotaLayout.HEIGHT;
    private static final double PADDING = 40;
    private static final double LIST_TOP = 110;
    private static final double ROW_HEIGHT = 58;
    private static final double ROW_GAP = 6;
    private static final double FOOTER_HEIGHT = 80;

    private static final Color PROBLEM_COLOR = Color.web("#ff8a7a");

    public void draw(Sketch s, SaveLoadState state) {
        s.screen(WIDTH, HEIGHT);
        s.text(state.getMode().getDisplayName(), Fonts.bold(34), Palette.TITLE, PADDING, 72);
        s.text(state.getMode() == SaveLoadMode.SAVE
                        ? "选择要写入的档位（自动存档在切换楼层时写入）"
                        : "选择要读取的档位",
                Fonts.regular(18), Palette.HINT, PADDING + 100, 70);
        drawSlots(s, state);
        drawFooter(s, state);
    }

    private void drawSlots(Sketch s, SaveLoadState state) {
        List<SaveSlotInfo> slots = state.getSlots();
        int visible = Math.max(1, (int) ((HEIGHT - LIST_TOP - FOOTER_HEIGHT) / (ROW_HEIGHT + ROW_GAP)));
        int top = Math.max(0, Math.min(state.getSelectedIndex() - visible + 1, slots.size() - visible));
        double rowW = WIDTH - 2 * PADDING;

        for (int i = top; i < Math.min(slots.size(), top + visible); i++) {
            double y = LIST_TOP + (i - top) * (ROW_HEIGHT + ROW_GAP);
            s.panel(PADDING, y, rowW, ROW_HEIGHT, false);
            if (i == state.getSelectedIndex()) {
                s.selection(PADDING, y, rowW, ROW_HEIGHT, true);
            }
            drawSlot(s, slots.get(i), state.isSelectable(i), PADDING, y, rowW);
        }
        if (top > 0) {
            s.centered("▲", Fonts.bold(18), Palette.HINT, WIDTH / 2, LIST_TOP - 8);
        }
        if (top + visible < slots.size()) {
            s.centered("▼", Fonts.bold(18), Palette.HINT, WIDTH / 2, LIST_TOP + visible * (ROW_HEIGHT + ROW_GAP) + 14);
        }
    }

    private void drawSlot(Sketch s, SaveSlotInfo slot, boolean selectable, double x, double y, double w) {
        double baseline = y + ROW_HEIGHT * 0.64;
        s.text(slot.name(), Fonts.bold(24), selectable ? Color.WHITE : Palette.DISABLED, x + 24, baseline);

        double detailX = x + 200;
        switch (slot.status()) {
            case OK -> {
                SaveMeta meta = slot.meta();
                String summary = meta.regionName() + " · 第 " + meta.floor() + " 层"
                        + "      Lv." + meta.level() + "      生命 " + meta.hp();
                s.text(summary, Fonts.regular(20), Palette.VALUE, detailX, baseline);
                s.right(meta.savedAt(), Fonts.regular(20), Palette.HINT, x + w - 24, baseline);
            }
            case EMPTY -> s.text(slot.status().getDisplayText(), Fonts.regular(20), Palette.DISABLED, detailX, baseline);
            case CORRUPTED, TOO_NEW, UNSUPPORTED ->
                    s.text(slot.status().getDisplayText(), Fonts.regular(20), PROBLEM_COLOR, detailX, baseline);
        }
    }

    private void drawFooter(Sketch s, SaveLoadState state) {
        double y = HEIGHT - FOOTER_HEIGHT + 20;
        s.panel(PADDING, y, WIDTH - 2 * PADDING, 46, false);
        String notice = state.getNotice();
        if (notice != null) {
            s.text(notice, Fonts.regular(19), Palette.NOTICE, PADDING + 20, y + 30);
        } else {
            s.text("↑↓ 选择    Enter 确认    Esc 返回", Fonts.regular(19), Palette.HINT, PADDING + 20, y + 30);
        }
    }
}
