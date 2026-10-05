package com.demo.mota.ui.screen.equipset;

import com.demo.mota.engine.Item.Equipment;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.state.equipset.EquipmentSet;
import com.demo.mota.ui.IconPainter;
import com.demo.mota.ui.PanelStyle;
import com.demo.mota.ui.TextPainter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * 套装界面（按 A）的绘制器，只读 {@link EquipmentSetState}（参照 img_7）。
 *
 * <p>一块大面板，两列排布：每套一格，「第N号套装」标题 + 一排装备图标（空槽画虚线框）。
 * 未保存的套装标题画灰并标「（未保存）」，与当前穿戴完全一致的那套标「● 当前」。
 */
public class EquipmentSetRenderer {

    private static final double PADDING = 12;
    private static final double HEADER_HEIGHT = 64;
    private static final double FOOTER_HEIGHT = 64;
    private static final double MAX_ICON_SIZE = 64;
    private static final double ICON_GAP = 10;
    private static final Color CURRENT_COLOR = Color.web("#7cfc7c");

    private final TextPainter painter = new TextPainter();
    private final IconPainter iconPainter;

    public EquipmentSetRenderer(ResourceManager resourceManager) {
        this.iconPainter = new IconPainter(resourceManager, painter);
    }

    public void render(GraphicsContext gc, EquipmentSetState state, double width, double height) {
        PanelStyle.drawBackground(gc, width, height);
        double x = PADDING;
        double y = PADDING;
        double w = width - 2 * PADDING;
        double h = height - 2 * PADDING;
        PanelStyle.drawPanel(gc, x, y, w, h, true);

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 28));
        painter.drawShadowText(gc, "套装设定", x + 24, y + 44, PanelStyle.TITLE_COLOR);

        List<EquipmentSet> sets = state.getSets();
        int rows = state.getRows();
        double gridTop = y + HEADER_HEIGHT;
        double columnW = (w - 3 * PADDING) / 2;
        double cellH = (h - HEADER_HEIGHT - FOOTER_HEIGHT) / rows;
        for (int i = 0; i < sets.size(); i++) {
            double cx = x + PADDING + (i / rows) * (columnW + PADDING);
            double cy = gridTop + (i % rows) * cellH;
            drawSet(gc, state, i, sets.get(i), cx, cy, columnW, cellH);
        }

        drawFooter(gc, state, x, y + h - FOOTER_HEIGHT, w);
    }

    private void drawSet(GraphicsContext gc, EquipmentSetState state, int index, EquipmentSet set,
                         double x, double y, double w, double h) {
        if (index == state.getSelectedIndex()) {
            PanelStyle.drawSelection(gc, x, y + 4, w, h - 8, true);
        }
        boolean saved = set.isSaved();
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 26));
        painter.drawShadowText(gc, set.getName(), x + 16, y + 38, saved ? PanelStyle.HINT_COLOR : PanelStyle.DISABLED_COLOR);
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 18));
        if (!saved) {
            painter.drawShadowText(gc, "（未保存）", x + 170, y + 36, PanelStyle.DISABLED_COLOR);
        } else if (state.getPlayer().isWearingSet(index)) {
            painter.drawShadowText(gc, "● 当前", x + 170, y + 36, CURRENT_COLOR);
        }

        int slotCount = set.slotCount();
        double iconSize = Math.min(MAX_ICON_SIZE,
                Math.min((w - 32 - (slotCount - 1) * ICON_GAP) / slotCount, h - 66));
        double iconTop = y + 52;
        for (int slot = 0; slot < slotCount; slot++) {
            double ix = x + 16 + slot * (iconSize + ICON_GAP);
            Equipment equipment = set.getSlot(slot);
            if (equipment != null) {
                iconPainter.drawEquipmentIcon(gc, equipment, ix, iconTop, iconSize, iconSize);
            } else {
                gc.setStroke(PanelStyle.PANEL_BORDER_DIM);
                gc.setLineWidth(1);
                gc.setLineDashes(5);
                gc.strokeRect(ix + 0.5, iconTop + 0.5, iconSize - 1, iconSize - 1);
                gc.setLineDashes(null);
            }
        }
    }

    private void drawFooter(GraphicsContext gc, EquipmentSetState state, double x, double y, double w) {
        gc.setStroke(PanelStyle.PANEL_BORDER_DIM);
        gc.setLineWidth(1);
        gc.strokeLine(x + 16, y + 0.5, x + w - 16, y + 0.5);
        gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 18));
        painter.drawShadowText(gc, "方向键 选择   Enter 换上并返回游戏   Esc / X 返回   （游戏中可直接按 W+数字键 换装）",
                x + 24, y + 38, PanelStyle.HINT_COLOR);
        if (!state.getNotice().isEmpty()) {
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 18));
            painter.drawRightAlignedShadowText(gc, state.getNotice(), x + w - 24, y + 38, PanelStyle.NOTICE_COLOR);
        }
    }
}
