package com.demo.mota.ui.screen.inventory;

import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.ui.IconPainter;
import com.demo.mota.ui.PanelStyle;
import com.demo.mota.ui.TextPainter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * 物品栏的绘制器（参照 img_6）：左上标题、左侧分类、右侧道具列表、底部说明栏。
 * 只读 {@link InventoryState} 的状态；只能查看的道具被选中时叠一层半透明灰。
 */
public class InventoryRenderer {

    // --- 布局 ---
    private static final double PADDING = 12;
    private static final double LEFT_WIDTH = 236;
    private static final double RIGHT_X = PADDING + LEFT_WIDTH + PADDING;
    private static final double CATEGORY_HEIGHT = 46;
    private static final double TITLE_HEIGHT = 80;
    private static final double DESC_HEIGHT = 130;
    private static final double ROW_HEIGHT = 56;

    // --- 配色 ---
    private static final Color HINT_COLOR = PanelStyle.HINT_COLOR;
    private static final Color TITLE_COLOR = PanelStyle.TITLE_COLOR;
    private static final Color DESC_COLOR = PanelStyle.DESC_COLOR;
    private static final Color VALUE_COLOR = PanelStyle.VALUE_COLOR;
    private static final Color EMPTY_COLOR = Color.web("#9aa5b1");
    /** 只能查看的道具被选中时叠的半透明灰层 */
    private static final Color VIEW_ONLY_MASK = Color.rgb(128, 128, 128, 0.45);

    private final TextPainter painter = new TextPainter();
    private final IconPainter iconPainter;

    public InventoryRenderer(ResourceManager resourceManager) {
        this.iconPainter = new IconPainter(resourceManager, painter);
    }

    public void render(GraphicsContext gc, InventoryState menu, double width, double height) {
        PanelStyle.drawBackground(gc, width, height);

        boolean itemsFocused = menu.getFocus() == InventoryState.Focus.ITEMS;
        double descY = height - PADDING - DESC_HEIGHT;
        double bodyY = PADDING + TITLE_HEIGHT + PADDING;
        double bodyH = descY - PADDING - bodyY;

        drawTitle(gc);
        drawCategories(gc, menu, !itemsFocused, bodyY, bodyH);

        double listX = RIGHT_X;
        double listW = width - RIGHT_X - PADDING;
        double listH = descY - 2 * PADDING;
        PanelStyle.drawPanel(gc, listX, PADDING, listW, listH, itemsFocused);
        List<InventoryEntry> entries = menu.getEntries();
        if (entries.isEmpty()) {
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 24));
            painter.drawShadowText(gc, "（暂无物品）", listX + 28, PADDING + 50, EMPTY_COLOR);
        } else {
            drawRows(gc, menu, entries, itemsFocused, listX, listW, listH);
        }

        PanelStyle.drawPanel(gc, PADDING, descY, width - 2 * PADDING, DESC_HEIGHT, false);
        drawDescription(gc, menu, itemsFocused, PADDING, descY, width - 2 * PADDING);
    }

    private void drawTitle(GraphicsContext gc) {
        PanelStyle.drawPanel(gc, PADDING, PADDING, LEFT_WIDTH, TITLE_HEIGHT, false);
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 28));
        painter.drawCenteredShadowText(gc, "物品栏", PADDING + LEFT_WIDTH / 2,
                PADDING + TITLE_HEIGHT / 2 + 10, Color.WHITE);
    }

    private void drawCategories(GraphicsContext gc, InventoryState menu, boolean focused, double y, double h) {
        PanelStyle.drawPanel(gc, PADDING, y, LEFT_WIDTH, h, focused);
        List<InventoryCategory> categories = menu.getCategories();
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 24));
        for (int i = 0; i < categories.size(); i++) {
            double itemY = y + 12 + i * CATEGORY_HEIGHT;
            if (i == menu.getSelectedCategoryIndex()) {
                // 焦点移到道具列表后，分类选中项保留一个暗一些的高亮
                PanelStyle.drawSelection(gc, PADDING + 8, itemY, LEFT_WIDTH - 16, CATEGORY_HEIGHT - 4, focused);
            }
            painter.drawShadowText(gc, categories.get(i).getDisplayName(),
                    PADDING + 22, itemY + CATEGORY_HEIGHT * 0.68);
        }
    }

    private void drawRows(GraphicsContext gc, InventoryState menu, List<InventoryEntry> entries,
                          boolean focused, double listX, double listW, double listH) {
        int visible = Math.max(1, (int) ((listH - 16) / ROW_HEIGHT));
        int selected = menu.getSelectedItemIndex();
        // 选中项越过可视区底部时才滚动；焦点在分类上时总是从头预览
        int first = focused ? Math.max(0, Math.min(selected - visible + 1, entries.size() - visible)) : 0;
        int last = Math.min(entries.size(), first + visible);

        for (int i = first; i < last; i++) {
            InventoryEntry entry = entries.get(i);
            double rowX = listX + 10;
            double rowY = PADDING + 8 + (i - first) * ROW_HEIGHT;
            double rowW = listW - 20;
            double rowH = ROW_HEIGHT - 6;
            boolean isSelected = focused && i == selected;

            if (isSelected) {
                PanelStyle.drawSelection(gc, rowX, rowY, rowW, rowH, true);
            }
            double iconSize = rowH - 8;
            iconPainter.drawItemIcon(gc, entry.item(), rowX + 8, rowY + 4, iconSize, iconSize);

            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 24));
            painter.drawShadowText(gc, entry.item().getItemName(), rowX + iconSize + 24, rowY + rowH * 0.68,
                    entry.usable() ? Color.WHITE : DESC_COLOR);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 22));
            painter.drawRightAlignedShadowText(gc, "×" + entry.count(), rowX + rowW - 16, rowY + rowH * 0.68,
                    VALUE_COLOR);

            if (isSelected && !entry.usable()) {
                gc.setFill(VIEW_ONLY_MASK);
                gc.fillRect(rowX, rowY, rowW, rowH);
            }
        }

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 20));
        if (first > 0) {
            painter.drawCenteredShadowText(gc, "▲", listX + listW / 2, PADDING + 4, HINT_COLOR);
        }
        if (last < entries.size()) {
            painter.drawCenteredShadowText(gc, "▼", listX + listW / 2, PADDING + listH - 4, HINT_COLOR);
        }
    }

    /** 底部说明栏：焦点在道具上时显示其说明，否则显示分类名与按键提示 */
    private void drawDescription(GraphicsContext gc, InventoryState menu, boolean itemsFocused,
                                 double x, double y, double w) {
        InventoryEntry entry = itemsFocused ? menu.getSelectedEntry() : null;
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 26));
        if (entry == null) {
            painter.drawShadowText(gc, menu.getSelectedCategory().getDisplayName(), x + 24, y + 44, Color.WHITE);
            gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 17));
            painter.drawShadowText(gc, "↑ ↓ 选择分类    Enter 查看道具    Esc / X 返回游戏",
                    x + 24, y + 96, HINT_COLOR);
            return;
        }

        String title = entry.item().getItemName() + (entry.usable() ? "" : "（仅可查看）");
        painter.drawShadowText(gc, title, x + 24, y + 40, TITLE_COLOR);
        Font descFont = Font.font("SimHei", FontWeight.NORMAL, 20);
        gc.setFont(descFont);
        String description = entry.item().getItemDescription() == null ? "" : entry.item().getItemDescription();
        List<String> lines = painter.wrap(description, descFont, w - 48);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            painter.drawShadowText(gc, lines.get(i), x + 24, y + 74 + i * 26, DESC_COLOR);
        }

        String notice = menu.getNotice();
        if (notice != null && !notice.isEmpty()) {
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 19));
            painter.drawRightAlignedShadowText(gc, notice, x + w - 24, y + 40, PanelStyle.NOTICE_COLOR);
        }
    }
}
