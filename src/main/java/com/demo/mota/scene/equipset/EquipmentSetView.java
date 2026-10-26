package com.demo.mota.scene.equipset;

import com.demo.mota.app.services.AssetService;
import com.demo.mota.engine.item.Equipment;
import com.demo.mota.engine.state.equipset.EquipmentSet;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.ui.kit.Icons;
import com.demo.mota.ui.kit.Palette;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * 套装界面（按 A，参照 img_7），只读 {@link EquipmentSetState}。
 *
 * <p>一块大面板，两列排布：每套一格，「第N号套装」标题 + 一排装备图标（空槽画虚线框）。
 * 未保存的套装标题画灰并标「（未保存）」，与当前穿戴完全一致的那套标「● 当前」。
 */
public class EquipmentSetView {

    private static final double WIDTH = MotaLayout.WIDTH;
    private static final double HEIGHT = MotaLayout.HEIGHT;
    private static final double PADDING = 12;
    private static final double HEADER_HEIGHT = 64;
    private static final double FOOTER_HEIGHT = 64;
    private static final double MAX_ICON_SIZE = 64;
    private static final double ICON_GAP = 10;
    private static final Color CURRENT_COLOR = Color.web("#7cfc7c");

    private final Icons icons;

    public EquipmentSetView(AssetService assets) {
        this.icons = new Icons(assets);
    }

    public void draw(Sketch s, EquipmentSetState state) {
        s.screen(WIDTH, HEIGHT);
        double x = PADDING;
        double y = PADDING;
        double w = WIDTH - 2 * PADDING;
        double h = HEIGHT - 2 * PADDING;
        s.panel(x, y, w, h, true);
        s.text("套装设定", Fonts.bold(28), Palette.TITLE, x + 24, y + 44);

        List<EquipmentSet> sets = state.getSets();
        int rows = state.getRows();
        double gridTop = y + HEADER_HEIGHT;
        double columnW = (w - 3 * PADDING) / 2;
        double cellH = (h - HEADER_HEIGHT - FOOTER_HEIGHT) / rows;
        for (int i = 0; i < sets.size(); i++) {
            double cx = x + PADDING + (i / rows) * (columnW + PADDING);
            double cy = gridTop + (i % rows) * cellH;
            drawSet(s, state, i, sets.get(i), cx, cy, columnW, cellH);
        }
        drawFooter(s, state, x, y + h - FOOTER_HEIGHT, w);
    }

    private void drawSet(Sketch s, EquipmentSetState state, int index, EquipmentSet set,
                         double x, double y, double w, double h) {
        if (index == state.getSelectedIndex()) {
            s.selection(x, y + 4, w, h - 8, true);
        }
        boolean saved = set.isSaved();
        s.text(set.getName(), Fonts.bold(26), saved ? Palette.HINT : Palette.DISABLED, x + 16, y + 38);
        if (!saved) {
            s.text("（未保存）", Fonts.bold(18), Palette.DISABLED, x + 170, y + 36);
        } else if (state.getPlayer().isWearingSet(index)) {
            s.text("● 当前", Fonts.bold(18), CURRENT_COLOR, x + 170, y + 36);
        }

        int slotCount = set.slotCount();
        double iconSize = Math.min(MAX_ICON_SIZE,
                Math.min((w - 32 - (slotCount - 1) * ICON_GAP) / slotCount, h - 66));
        double iconTop = y + 52;
        for (int slot = 0; slot < slotCount; slot++) {
            double ix = x + 16 + slot * (iconSize + ICON_GAP);
            Equipment equipment = set.getSlot(slot);
            if (equipment != null) {
                icons.equipment(s, equipment, ix, iconTop, iconSize, iconSize);
            } else {
                s.dashed(ix + 0.5, iconTop + 0.5, iconSize - 1, iconSize - 1, Palette.BORDER_DIM, 1, 5);
            }
        }
    }

    private void drawFooter(Sketch s, EquipmentSetState state, double x, double y, double w) {
        s.line(x + 16, y + 0.5, x + w - 16, y + 0.5, Palette.BORDER_DIM, 1);
        s.text("方向键 选择   Enter 换上并返回游戏   Esc / X 返回   （游戏中可直接按 W+数字键 换装）",
                Fonts.regular(18), Palette.HINT, x + 24, y + 38);
        if (!state.getNotice().isEmpty()) {
            s.right(state.getNotice(), Fonts.bold(18), Palette.NOTICE, x + w - 24, y + 38);
        }
    }
}
