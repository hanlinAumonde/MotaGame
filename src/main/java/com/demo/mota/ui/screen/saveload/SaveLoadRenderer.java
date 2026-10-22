package com.demo.mota.ui.screen.saveload;

import com.demo.mota.engine.save.SaveMeta;
import com.demo.mota.engine.save.SaveSlotInfo;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.PanelStyle;
import com.demo.mota.ui.TextPainter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * 存读档界面绘制（只读 {@link SaveLoadState}）：标题 + 一列档位 + 底部提示。
 * 每行「档位名 + 摘要（区域 · 楼层 / 等级 / 生命）+ 右侧保存时间」；空档、损坏、版本不兼容的显示状态文字，
 * 存档模式下不可写的自动存档画灰。档位多到放不下时随光标滚动。
 */
public class SaveLoadRenderer {

    private static final double PADDING = 40;
    private static final double LIST_TOP = 110;
    private static final double ROW_HEIGHT = 58;
    private static final double ROW_GAP = 6;
    private static final double FOOTER_HEIGHT = 80;

    private static final Color PROBLEM_COLOR = Color.web("#ff8a7a");

    private final TextPainter painter = new TextPainter();

    public void render(GraphicsContext gc, SaveLoadState state, double width, double height) {
        PanelStyle.drawBackground(gc, width, height);

        gc.setFont(Fonts.bold(34));
        painter.drawShadowText(gc, state.getMode().getDisplayName(), PADDING, 72, PanelStyle.TITLE_COLOR);
        gc.setFont(Fonts.regular(18));
        painter.drawShadowText(gc, state.getMode() == SaveLoadMode.SAVE
                        ? "选择要写入的档位（自动存档在切换楼层时写入）"
                        : "选择要读取的档位",
                PADDING + 100, 70, PanelStyle.HINT_COLOR);

        drawSlots(gc, state, width, height);
        drawFooter(gc, state, width, height);
    }

    private void drawSlots(GraphicsContext gc, SaveLoadState state, double width, double height) {
        List<SaveSlotInfo> slots = state.getSlots();
        int visible = Math.max(1, (int) ((height - LIST_TOP - FOOTER_HEIGHT) / (ROW_HEIGHT + ROW_GAP)));
        int top = Math.max(0, Math.min(state.getSelectedIndex() - visible + 1, slots.size() - visible));
        double rowW = width - 2 * PADDING;

        for (int i = top; i < Math.min(slots.size(), top + visible); i++) {
            double y = LIST_TOP + (i - top) * (ROW_HEIGHT + ROW_GAP);
            boolean selected = i == state.getSelectedIndex();
            PanelStyle.drawPanel(gc, PADDING, y, rowW, ROW_HEIGHT, false);
            if (selected) {
                PanelStyle.drawSelection(gc, PADDING, y, rowW, ROW_HEIGHT, true);
            }
            drawSlot(gc, slots.get(i), state.isSelectable(i), PADDING, y, rowW);
        }
        gc.setFont(Fonts.bold(18));
        if (top > 0) {
            painter.drawCenteredShadowText(gc, "▲", width / 2, LIST_TOP - 8, PanelStyle.HINT_COLOR);
        }
        if (top + visible < slots.size()) {
            double bottom = LIST_TOP + visible * (ROW_HEIGHT + ROW_GAP) + 14;
            painter.drawCenteredShadowText(gc, "▼", width / 2, bottom, PanelStyle.HINT_COLOR);
        }
    }

    private void drawSlot(GraphicsContext gc, SaveSlotInfo slot, boolean selectable, double x, double y, double w) {
        double baseline = y + ROW_HEIGHT * 0.64;
        gc.setFont(Fonts.bold(24));
        painter.drawShadowText(gc, slot.name(), x + 24, baseline,
                selectable ? Color.WHITE : PanelStyle.DISABLED_COLOR);

        gc.setFont(Fonts.regular(20));
        double detailX = x + 200;
        switch (slot.status()) {
            case OK -> {
                SaveMeta meta = slot.meta();
                String summary = meta.regionName() + " · 第 " + meta.floor() + " 层"
                        + "      Lv." + meta.level() + "      生命 " + meta.hp();
                painter.drawShadowText(gc, summary, detailX, baseline, PanelStyle.VALUE_COLOR);
                painter.drawRightAlignedShadowText(gc, meta.savedAt(), x + w - 24, baseline, PanelStyle.HINT_COLOR);
            }
            case EMPTY -> painter.drawShadowText(gc, slot.status().getDisplayText(), detailX, baseline,
                    PanelStyle.DISABLED_COLOR);
            case CORRUPTED, TOO_NEW, UNSUPPORTED -> painter.drawShadowText(gc, slot.status().getDisplayText(),
                    detailX, baseline, PROBLEM_COLOR);
        }
    }

    private void drawFooter(GraphicsContext gc, SaveLoadState state, double width, double height) {
        double y = height - FOOTER_HEIGHT + 20;
        PanelStyle.drawPanel(gc, PADDING, y, width - 2 * PADDING, 46, false);
        gc.setFont(Fonts.regular(19));
        String notice = state.getNotice();
        if (notice != null) {
            painter.drawShadowText(gc, notice, PADDING + 20, y + 30, PanelStyle.NOTICE_COLOR);
        } else {
            painter.drawShadowText(gc, "↑↓ 选择    Enter 确认    Esc 返回", PADDING + 20, y + 30, PanelStyle.HINT_COLOR);
        }
    }
}
