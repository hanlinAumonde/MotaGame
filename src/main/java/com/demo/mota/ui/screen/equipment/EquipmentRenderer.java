package com.demo.mota.ui.screen.equipment;

import com.demo.mota.ui.PanelStyle;
import com.demo.mota.ui.TextPainter;
import com.demo.mota.ui.IconPainter;
import com.demo.mota.ui.ValueFormatter;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.Item.Equipment;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.state.PlayerStateManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;

/**
 * 装备界面（按 Q）的绘制器，只读 {@link EquipmentState} 的状态。
 *
 * <p>三栏：左 = 装备列表（只列装备）；中上 = 槽位网格、中下 = 玩家属性；右 = 聚焦装备的详情（属性加成 + 附带技能）。
 */
public class EquipmentRenderer {

    private static final double PADDING = 12;
    private static final double LEFT_WIDTH = 360;
    private static final double RIGHT_WIDTH = 380;
    private static final double LIST_ROW_HEIGHT = 56;
    private static final Color EQUIPPED_COLOR = Color.web("#7cfc7c");
    private static final Color BONUS_COLOR = Color.web("#ffe08a");

    private final ResourceManager resourceManager;
    private final TextPainter painter = new TextPainter();
    private final IconPainter iconPainter;

    public EquipmentRenderer(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.iconPainter = new IconPainter(resourceManager, painter);
    }

    public void render(GraphicsContext gc, EquipmentState menu, double width, double height) {
        PanelStyle.drawBackground(gc, width, height);
        double midX = PADDING * 2 + LEFT_WIDTH;
        double rightX = width - PADDING - RIGHT_WIDTH;
        double midW = rightX - PADDING - midX;
        double slotsH = height * 0.5;

        drawList(gc, menu, PADDING, PADDING, LEFT_WIDTH, height - 2 * PADDING);
        drawSlots(gc, menu, midX, PADDING, midW, slotsH);
        drawStats(gc, menu.getPlayer(), midX, PADDING * 2 + slotsH, midW, height - slotsH - 3 * PADDING);
        drawDetail(gc, menu.getFocusedEquipment(), rightX, PADDING, RIGHT_WIDTH, height - 2 * PADDING);
    }

    // ==================== 左：装备列表 ====================

    private void drawList(GraphicsContext gc, EquipmentState menu, double x, double y, double w, double h) {
        boolean focused = menu.getFocus() == EquipmentState.Focus.LIST;
        PanelStyle.drawPanel(gc, x, y, w, h, focused);
        PlayerStateManager player = menu.getPlayer();
        List<Equipment> equipments = menu.getEquipments();

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 24));
        painter.drawShadowText(gc, "装备", x + 18, y + 38, PanelStyle.SECTION_COLOR);

        double listTop = y + 56;
        double listBottom = y + h - 110;
        if (equipments.isEmpty()) {
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 20));
            painter.drawShadowText(gc, "还没有任何装备", x + 18, listTop + 36, PanelStyle.DISABLED_COLOR);
        } else {
            int visible = Math.max(1, (int) ((listBottom - listTop) / LIST_ROW_HEIGHT));
            int selected = menu.getSelectedListIndex();
            int first = Math.max(0, Math.min(selected - visible + 1, equipments.size() - visible));
            int last = Math.min(equipments.size(), first + visible);
            for (int i = first; i < last; i++) {
                double rowY = listTop + (i - first) * LIST_ROW_HEIGHT;
                Equipment equipment = equipments.get(i);
                if (i == selected) {
                    PanelStyle.drawSelection(gc, x + 8, rowY, w - 16, LIST_ROW_HEIGHT - 6, focused);
                }
                iconPainter.drawEquipmentIcon(gc, equipment, x + 16, rowY + 5, 40, 40);
                gc.setFont(painter.fitFont(equipment.getItemName(), "SimHei", 22, 14, w - 150));
                painter.drawShadowText(gc, equipment.getItemName(), x + 68, rowY + 33, Color.WHITE);
                int slot = player.slotOf(equipment);
                if (slot >= 0) {
                    gc.setFont(Font.font("SimHei", FontWeight.BOLD, 16));
                    painter.drawRightAlignedShadowText(gc, "已装备·" + (slot + 1), x + w - 18, rowY + 32, EQUIPPED_COLOR);
                }
            }
        }

        gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 16));
        String[] hints = focused
                ? new String[]{"↑↓ 选择   Enter 选中并挑槽位", "→ 查看槽位", "X 返回游戏"}
                : new String[]{"方向键 选槽位   Enter " + (menu.getPending() != null ? "装备到此槽" : "卸下"),
                               "Esc 返回装备列表", "X 返回游戏"};
        for (int i = 0; i < hints.length; i++) {
            painter.drawShadowText(gc, hints[i], x + 18, y + h - 78 + i * 26, PanelStyle.HINT_COLOR);
        }
    }

    // ==================== 中上：槽位 ====================

    private void drawSlots(GraphicsContext gc, EquipmentState menu, double x, double y, double w, double h) {
        boolean focused = menu.getFocus() == EquipmentState.Focus.SLOTS;
        PanelStyle.drawPanel(gc, x, y, w, h, focused);
        PlayerStateManager player = menu.getPlayer();

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
        painter.drawShadowText(gc, "当前装备", x + 18, y + 34, PanelStyle.SECTION_COLOR);
        if (!menu.getNotice().isEmpty()) {
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 18));
            painter.drawRightAlignedShadowText(gc, menu.getNotice(), x + w - 18, y + 34, PanelStyle.NOTICE_COLOR);
        }

        int count = player.getEquipSlotCount();
        int cols = EquipmentState.SLOT_COLUMNS;
        int rows = (count + cols - 1) / cols;
        double gridTop = y + 52;
        double cellW = (w - 36) / cols;
        double cellH = Math.min((h - 64) / rows, cellW);
        double iconSize = Math.min(cellW, cellH) - 44;

        for (int i = 0; i < count; i++) {
            double cx = x + 18 + (i % cols) * cellW;
            double cy = gridTop + ((double) i / cols) * cellH;
            if (focused && i == menu.getSelectedSlot()) {
                PanelStyle.drawSelection(gc, cx + 4, cy + 2, cellW - 8, cellH - 6, true);
            }
            double iconX = cx + (cellW - iconSize) / 2;
            Equipment equipment = player.getEquipped(i);
            if (equipment != null) {
                iconPainter.drawEquipmentIcon(gc, equipment, iconX, cy + 8, iconSize, iconSize);
                gc.setFont(painter.fitFont(equipment.getItemName(), "SimHei", 18, 12, cellW - 12));
                painter.drawCenteredShadowText(gc, equipment.getItemName(), cx + cellW / 2, cy + iconSize + 30,
                        Color.web("#ff9ad8"));
            } else {
                gc.setStroke(PanelStyle.PANEL_BORDER_DIM);
                gc.setLineWidth(1);
                gc.setLineDashes(6);
                gc.strokeRect(iconX, cy + 8, iconSize, iconSize);
                gc.setLineDashes(null);
                gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 16));
                painter.drawCenteredShadowText(gc, "槽位 " + (i + 1), cx + cellW / 2, cy + iconSize + 30,
                        PanelStyle.DISABLED_COLOR);
            }
        }
    }

    // ==================== 中下：属性 ====================

    private void drawStats(GraphicsContext gc, PlayerStateManager player, double x, double y, double w, double h) {
        PanelStyle.drawPanel(gc, x, y, w, h, false);
        GameNumber maxHp = player.getMaxHP();

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 24));
        painter.drawShadowText(gc, "等级", x + 24, y + 44, PanelStyle.HINT_COLOR);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 26));
        painter.drawShadowText(gc, String.valueOf(player.getLevelNumber()), x + 110, y + 44, Color.WHITE);
        Image avatar = resourceManager.getPlayerSprite(Direction.DOWN);
        if (avatar != null) {
            gc.drawImage(avatar, x + w - 76, y + 12, 52, 52);
        }

        String[][] rows = {
                {"生命", ValueFormatter.formatScaled(player.getCurrentHP(), maxHp) + " / "
                        + ValueFormatter.formatScaled(maxHp, maxHp)},
                {"攻击", ValueFormatter.formatScaled(player.getEffectiveATK(), maxHp)},
                {"防御", ValueFormatter.formatScaled(player.getEffectiveDEF(), maxHp)},
                {"金币", String.valueOf(player.getCurrentGoldAmount())}
        };
        StateType[] bonusTypes = {StateType.MAX_HP, StateType.ATK, StateType.DEF, null};
        for (int i = 0; i < rows.length; i++) {
            double rowY = y + 96 + i * 46;
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
            painter.drawShadowText(gc, rows[i][0], x + 24, rowY, PanelStyle.LABEL_COLOR);
            gc.setFont(painter.fitFont(rows[i][1], "Consolas", 24, 14, w - 260));
            painter.drawShadowText(gc, rows[i][1], x + 110, rowY, PanelStyle.VALUE_COLOR);
            // 装备带来的加成单独标出，一眼看出穿脱的影响
            if (bonusTypes[i] != null) {
                GameNumber bonus = player.getEffectiveAttr(bonusTypes[i]).minus(player.getStateValue(bonusTypes[i]));
                if (bonus.isPositive()) {
                    gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
                    painter.drawRightAlignedShadowText(gc, "+" + ValueFormatter.formatScaled(bonus, maxHp),
                            x + w - 24, rowY, BONUS_COLOR);
                }
            }
        }
    }

    // ==================== 右：详情 ====================

    private void drawDetail(GraphicsContext gc, Equipment equipment, double x, double y, double w, double h) {
        PanelStyle.drawPanel(gc, x, y, w, h, false);
        if (equipment == null) {
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 20));
            painter.drawShadowText(gc, "（未选中装备）", x + 20, y + 44, PanelStyle.DISABLED_COLOR);
            return;
        }
        double textW = w - 40;
        double cy = y + 42;

        gc.setFont(painter.fitFont(equipment.getItemName(), "SimHei", 28, 16, textW));
        painter.drawShadowText(gc, equipment.getItemName(), x + 20, cy, Color.web("#ff9ad8"));
        cy += 42;

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 20));
        for (Map.Entry<StateType, GameNumber> entry : equipment.getStateEffectMap().entrySet()) {
            painter.drawShadowText(gc, statName(entry.getKey()) + " +" + entry.getValue(), x + 20, cy, Color.WHITE);
            cy += 30;
        }

        Font descFont = Font.font("SimHei", FontWeight.NORMAL, 17);
        cy = drawWrapped(gc, equipment.getItemDescription(), descFont, x + 20, cy + 10, textW,
                Color.web("#ffe08a"), y + h);

        for (Skill skill : equipment.getSkills()) {
            cy += 18;
            if (cy > y + h - 30) break;
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 20));
            painter.drawShadowText(gc, skill.skillName() + "（" + skill.skillType().getDisplayName() + "）",
                    x + 20, cy, skill.isActive() ? PanelStyle.ACTIVE_SKILL_COLOR : PanelStyle.PASSIVE_SKILL_COLOR);
            cy += 8;
            for (String paragraph : skill.descriptionLines()) {
                cy = drawWrapped(gc, paragraph, descFont, x + 20, cy + 18, textW, PanelStyle.DESC_COLOR, y + h);
            }
        }
    }

    /** @return 最后一行的基线 y */
    private double drawWrapped(GraphicsContext gc, String text, Font font, double x, double y, double maxWidth,
                               Color color, double bottom) {
        if (text == null || text.isBlank()) return y - 18;
        gc.setFont(font);
        List<String> lines = painter.wrap(text, font, maxWidth);
        double cy = y;
        for (int i = 0; i < lines.size() && cy < bottom - 12; i++) {
            painter.drawShadowText(gc, lines.get(i), x, cy, color);
            if (i < lines.size() - 1) cy += 24;
        }
        return cy;
    }

    private static String statName(StateType type) {
        return switch (type) {
            case HP -> "生命";
            case MAX_HP -> "生命上限";
            case ATK -> "攻击";
            case DEF -> "防御";
        };
    }
}
