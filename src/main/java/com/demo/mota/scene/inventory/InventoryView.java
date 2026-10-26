package com.demo.mota.scene.inventory;

import com.demo.mota.app.services.AssetService;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.ui.kit.Icons;
import com.demo.mota.ui.kit.Palette;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * 物品栏（参照 img_6）：左上标题、左侧分类、右侧道具列表、底部说明栏。
 * 只读 {@link InventoryState}；只能查看的道具被选中时叠一层半透明灰。
 */
public class InventoryView {

    private static final double WIDTH = MotaLayout.WIDTH;
    private static final double HEIGHT = MotaLayout.HEIGHT;
    private static final double PADDING = 12;
    private static final double LEFT_WIDTH = 236;
    private static final double RIGHT_X = PADDING + LEFT_WIDTH + PADDING;
    private static final double CATEGORY_HEIGHT = 46;
    private static final double TITLE_HEIGHT = 80;
    private static final double DESC_HEIGHT = 130;
    private static final double ROW_HEIGHT = 56;

    private static final Color EMPTY_COLOR = Color.web("#9aa5b1");
    /** 只能查看的道具被选中时叠的半透明灰层 */
    private static final Color VIEW_ONLY_MASK = Color.rgb(128, 128, 128, 0.45);

    private final Icons icons;

    public InventoryView(AssetService assets) {
        this.icons = new Icons(assets);
    }

    public void draw(Sketch s, InventoryState menu) {
        s.screen(WIDTH, HEIGHT);

        boolean itemsFocused = menu.getFocus() == InventoryState.Focus.ITEMS;
        double descY = HEIGHT - PADDING - DESC_HEIGHT;
        double bodyY = PADDING + TITLE_HEIGHT + PADDING;
        double bodyH = descY - PADDING - bodyY;

        s.panel(PADDING, PADDING, LEFT_WIDTH, TITLE_HEIGHT, false);
        s.centered("物品栏", Fonts.bold(28), Color.WHITE, PADDING + LEFT_WIDTH / 2, PADDING + TITLE_HEIGHT / 2 + 10);
        drawCategories(s, menu, !itemsFocused, bodyY, bodyH);

        double listW = WIDTH - RIGHT_X - PADDING;
        double listH = descY - 2 * PADDING;
        s.panel(RIGHT_X, PADDING, listW, listH, itemsFocused);
        List<InventoryEntry> entries = menu.getEntries();
        if (entries.isEmpty()) {
            s.text("（暂无物品）", Fonts.bold(24), EMPTY_COLOR, RIGHT_X + 28, PADDING + 50);
        } else {
            drawRows(s, menu, entries, itemsFocused, RIGHT_X, listW, listH);
        }

        s.panel(PADDING, descY, WIDTH - 2 * PADDING, DESC_HEIGHT, false);
        drawDescription(s, menu, itemsFocused, PADDING, descY, WIDTH - 2 * PADDING);
    }

    private void drawCategories(Sketch s, InventoryState menu, boolean focused, double y, double h) {
        s.panel(PADDING, y, LEFT_WIDTH, h, focused);
        List<InventoryCategory> categories = menu.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            double itemY = y + 12 + i * CATEGORY_HEIGHT;
            if (i == menu.getSelectedCategoryIndex()) {
                // 焦点移到道具列表后，分类选中项保留一个暗一些的高亮
                s.selection(PADDING + 8, itemY, LEFT_WIDTH - 16, CATEGORY_HEIGHT - 4, focused);
            }
            s.text(categories.get(i).getDisplayName(), Fonts.bold(24), Color.WHITE,
                    PADDING + 22, itemY + CATEGORY_HEIGHT * 0.68);
        }
    }

    private void drawRows(Sketch s, InventoryState menu, List<InventoryEntry> entries,
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
                s.selection(rowX, rowY, rowW, rowH, true);
            }
            double iconSize = rowH - 8;
            icons.item(s, entry.item(), rowX + 8, rowY + 4, iconSize, iconSize);
            s.text(entry.item().getItemName(), Fonts.bold(24), entry.usable() ? Color.WHITE : Palette.DESC,
                    rowX + iconSize + 24, rowY + rowH * 0.68);
            s.right("×" + entry.count(), Fonts.mono(22), Palette.VALUE, rowX + rowW - 16, rowY + rowH * 0.68);

            if (isSelected && !entry.usable()) {
                s.fill(rowX, rowY, rowW, rowH, VIEW_ONLY_MASK);
            }
        }
        if (first > 0) {
            s.centered("▲", Fonts.bold(20), Palette.HINT, listX + listW / 2, PADDING + 4);
        }
        if (last < entries.size()) {
            s.centered("▼", Fonts.bold(20), Palette.HINT, listX + listW / 2, PADDING + listH - 4);
        }
    }

    /** 底部说明栏：焦点在道具上时显示其说明，否则显示分类名与按键提示 */
    private void drawDescription(Sketch s, InventoryState menu, boolean itemsFocused, double x, double y, double w) {
        InventoryEntry entry = itemsFocused ? menu.getSelectedEntry() : null;
        if (entry == null) {
            s.text(menu.getSelectedCategory().getDisplayName(), Fonts.bold(26), Color.WHITE, x + 24, y + 44);
            s.text("↑ ↓ 选择分类    Enter 查看道具    Esc / X 返回游戏", Fonts.regular(17), Palette.HINT,
                    x + 24, y + 96);
            return;
        }

        String title = entry.item().getItemName() + (entry.usable() ? "" : "（仅可查看）");
        s.text(title, Fonts.bold(26), Palette.TITLE, x + 24, y + 40);
        Font descFont = Fonts.regular(20);
        String description = entry.item().getItemDescription() == null ? "" : entry.item().getItemDescription();
        List<String> lines = s.metrics().wrap(description, descFont, w - 48);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            s.text(lines.get(i), descFont, Palette.DESC, x + 24, y + 74 + i * 26);
        }

        String notice = menu.getNotice();
        if (notice != null && !notice.isEmpty()) {
            s.right(notice, Fonts.bold(19), Palette.NOTICE, x + w - 24, y + 40);
        }
    }
}
