package com.demo.mota.scene.equipment;

import com.demo.mota.app.services.AssetService;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.item.Equipment;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.ui.ValueFormatter;
import com.demo.mota.ui.kit.Icons;
import com.demo.mota.ui.kit.Palette;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;
import java.util.Map;

/**
 * 装备界面（按 Q），只读 {@link EquipmentState}。
 *
 * <p>三栏：左 = 装备列表（只列装备）；中上 = 槽位网格、中下 = 玩家属性；右 = 聚焦装备的详情（属性加成 + 附带技能）。
 */
public class EquipmentView {

    private static final double WIDTH = MotaLayout.WIDTH;
    private static final double HEIGHT = MotaLayout.HEIGHT;
    private static final double PADDING = 12;
    private static final double LEFT_WIDTH = 360;
    private static final double RIGHT_WIDTH = 380;
    private static final double LIST_ROW_HEIGHT = 56;
    private static final Color EQUIPPED_COLOR = Color.web("#7cfc7c");
    private static final Color BONUS_COLOR = Color.web("#ffe08a");
    private static final Color NAME_COLOR = Color.web("#ff9ad8");

    private final AssetService assets;
    private final Icons icons;

    public EquipmentView(AssetService assets) {
        this.assets = assets;
        this.icons = new Icons(assets);
    }

    public void draw(Sketch s, EquipmentState menu) {
        s.screen(WIDTH, HEIGHT);
        double midX = PADDING * 2 + LEFT_WIDTH;
        double rightX = WIDTH - PADDING - RIGHT_WIDTH;
        double midW = rightX - PADDING - midX;
        double slotsH = HEIGHT * 0.5;

        drawList(s, menu, PADDING, PADDING, LEFT_WIDTH, HEIGHT - 2 * PADDING);
        drawSlots(s, menu, midX, PADDING, midW, slotsH);
        drawStats(s, menu.getPlayer(), midX, PADDING * 2 + slotsH, midW, HEIGHT - slotsH - 3 * PADDING);
        drawDetail(s, menu.getFocusedEquipment(), rightX, PADDING, RIGHT_WIDTH, HEIGHT - 2 * PADDING);
    }

    // ==================== 左：装备列表 ====================

    private void drawList(Sketch s, EquipmentState menu, double x, double y, double w, double h) {
        boolean focused = menu.getFocus() == EquipmentState.Focus.LIST;
        s.panel(x, y, w, h, focused);
        PlayerStateManager player = menu.getPlayer();
        List<Equipment> equipments = menu.getEquipments();

        s.text("装备", Fonts.bold(24), Palette.SECTION, x + 18, y + 38);

        double listTop = y + 56;
        double listBottom = y + h - 136;
        if (equipments.isEmpty()) {
            s.text("还没有任何装备", Fonts.bold(20), Palette.DISABLED, x + 18, listTop + 36);
        } else {
            int visible = Math.max(1, (int) ((listBottom - listTop) / LIST_ROW_HEIGHT));
            int selected = menu.getSelectedListIndex();
            int first = Math.max(0, Math.min(selected - visible + 1, equipments.size() - visible));
            int last = Math.min(equipments.size(), first + visible);
            for (int i = first; i < last; i++) {
                double rowY = listTop + (i - first) * LIST_ROW_HEIGHT;
                Equipment equipment = equipments.get(i);
                if (i == selected) {
                    s.selection(x + 8, rowY, w - 16, LIST_ROW_HEIGHT - 6, focused);
                }
                icons.equipment(s, equipment, x + 16, rowY + 5, 40, 40);
                s.fitted(equipment.getItemName(), Fonts::bold, 22, 14, w - 150, Color.WHITE, x + 68, rowY + 33);
                int slot = player.slotOf(equipment);
                if (slot >= 0) {
                    s.right("已装备·" + (slot + 1), Fonts.bold(16), EQUIPPED_COLOR, x + w - 18, rowY + 32);
                }
            }
        }

        String[] hints = focused
                ? new String[]{"↑↓ 选择   Enter 选中并挑槽位", "→ 查看槽位", "X 返回游戏"}
                : new String[]{"方向键 选槽位   Enter " + (menu.getPending() != null ? "装备到此槽" : "卸下"),
                               "Esc 返回装备列表", "X 返回游戏"};
        for (int i = 0; i < hints.length; i++) {
            s.text(hints[i], Fonts.regular(16), Palette.HINT, x + 18, y + h - 104 + i * 26);
        }
        // 套装保存提示（img_4）：按下 W 等数字键期间高亮
        s.text(menu.isSaveArmed() ? "W+数字键 保存套装（请按数字键）" : "W+数字键 保存套装", Fonts.bold(18),
                menu.isSaveArmed() ? Palette.NOTICE : Palette.SECTION, x + 18, y + h - 22);
    }

    // ==================== 中上：槽位 ====================

    private void drawSlots(Sketch s, EquipmentState menu, double x, double y, double w, double h) {
        boolean focused = menu.getFocus() == EquipmentState.Focus.SLOTS;
        s.panel(x, y, w, h, focused);
        PlayerStateManager player = menu.getPlayer();

        s.text("当前装备", Fonts.bold(22), Palette.SECTION, x + 18, y + 34);
        if (!menu.getNotice().isEmpty()) {
            s.right(menu.getNotice(), Fonts.bold(18), Palette.NOTICE, x + w - 18, y + 34);
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
            // 行号必须是整除：写成 (double) i / cols 会让同一行的第 2、3 格各自往下错开 1/3、2/3 行
            double cy = gridTop + (i / cols) * cellH;
            if (focused && i == menu.getSelectedSlot()) {
                s.selection(cx + 4, cy + 2, cellW - 8, cellH - 6, true);
            }
            double iconX = cx + (cellW - iconSize) / 2;
            Equipment equipment = player.getEquipped(i);
            if (equipment != null) {
                icons.equipment(s, equipment, iconX, cy + 8, iconSize, iconSize);
                Font font = s.metrics().fit(equipment.getItemName(), Fonts::bold, 18, 12, cellW - 12);
                s.centered(equipment.getItemName(), font, NAME_COLOR, cx + cellW / 2, cy + iconSize + 30);
            } else {
                s.dashed(iconX, cy + 8, iconSize, iconSize, Palette.BORDER_DIM, 1, 6);
                s.centered("槽位 " + (i + 1), Fonts.regular(16), Palette.DISABLED, cx + cellW / 2, cy + iconSize + 30);
            }
        }
    }

    // ==================== 中下：属性 ====================

    private void drawStats(Sketch s, PlayerStateManager player, double x, double y, double w, double h) {
        s.panel(x, y, w, h, false);
        GameNumber maxHp = player.getMaxHP();

        s.text("等级", Fonts.bold(24), Palette.HINT, x + 24, y + 44);
        s.text(String.valueOf(player.getLevelNumber()), Fonts.mono(26), Color.WHITE, x + 110, y + 44);
        Image avatar = assets.idleFrame(AssetService.PLAYER, Direction.DOWN);
        if (avatar != null) {
            s.image(avatar, x + w - 76, y + 12, 52, 52);
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
            s.text(rows[i][0], Fonts.bold(22), Palette.LABEL, x + 24, rowY);
            s.fitted(rows[i][1], Fonts::mono, 24, 14, w - 260, Palette.VALUE, x + 110, rowY);
            // 装备带来的加成单独标出，一眼看出穿脱的影响
            if (bonusTypes[i] != null) {
                GameNumber bonus = player.getEffectiveAttr(bonusTypes[i]).minus(player.getStateValue(bonusTypes[i]));
                if (bonus.isPositive()) {
                    s.right("+" + ValueFormatter.formatScaled(bonus, maxHp), Fonts.mono(18), BONUS_COLOR,
                            x + w - 24, rowY);
                }
            }
        }
    }

    // ==================== 右：详情 ====================

    private void drawDetail(Sketch s, Equipment equipment, double x, double y, double w, double h) {
        s.panel(x, y, w, h, false);
        if (equipment == null) {
            s.text("（未选中装备）", Fonts.bold(20), Palette.DISABLED, x + 20, y + 44);
            return;
        }
        double textW = w - 40;
        double cy = y + 42;

        s.fitted(equipment.getItemName(), Fonts::bold, 28, 16, textW, NAME_COLOR, x + 20, cy);
        cy += 42;

        for (Map.Entry<StateType, GameNumber> entry : equipment.getStateEffectMap().entrySet()) {
            s.text(statName(entry.getKey()) + " +" + entry.getValue(), Fonts.bold(20), Color.WHITE, x + 20, cy);
            cy += 30;
        }

        Font descFont = Fonts.regular(17);
        cy = drawWrapped(s, equipment.getItemDescription(), descFont, x + 20, cy + 10, textW,
                Color.web("#ffe08a"), y + h);

        for (Skill skill : equipment.getSkills()) {
            // 上一行是正文（17 号）的基线，技能标题（20 号）要再往下一整行，原来只隔 18 像素、两行字压在一起
            cy += 36;
            if (cy > y + h - 30) break;
            s.text(skill.skillName() + "（" + skill.skillType().getDisplayName() + "）", Fonts.bold(20),
                    skill.isActive() ? Palette.ACTIVE_SKILL : Palette.PASSIVE_SKILL, x + 20, cy);
            cy += 8;
            for (String paragraph : skill.descriptionLines()) {
                cy = drawWrapped(s, paragraph, descFont, x + 20, cy + 18, textW, Palette.DESC, y + h);
            }
        }
    }

    /** @return 最后一行的基线 y */
    private static double drawWrapped(Sketch s, String text, Font font, double x, double y, double maxWidth,
                                      Color color, double bottom) {
        if (text == null || text.isBlank()) return y - 18;
        List<String> lines = s.metrics().wrap(text, font, maxWidth);
        double cy = y;
        for (int i = 0; i < lines.size() && cy < bottom - 12; i++) {
            s.text(lines.get(i), font, color, x, cy);
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
