package com.demo.mota.ui;

import com.demo.mota.engine.menu.SkillSetupMenu;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.book.OwnedSkill;
import com.demo.mota.engine.skill.book.SkillBook;
import com.demo.mota.engine.rules.GameRules;
import com.demo.mota.engine.skill.preset.SkillPreset;
import com.demo.mota.engine.skill.preset.SkillPresetBook;
import com.demo.mota.engine.state.PlayerStateManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能设置界面（按 D）的绘制器，只读 {@link SkillSetupMenu} 的状态。
 *
 * <p>上半屏：玩家名 + 头像、可滚动的技能卡片列表；
 * 下半屏：当前激活（即正在编辑）的那套预设——标题旁一排快捷键小方块标出是第几套，
 * 下面一行回合格（第 0 格为「战前」），空格显示「普攻」。
 */
public class SkillSetupRenderer {

    private static final double PADDING = 12;
    private static final double COMBO_HEIGHT = 196;
    private static final Color ARMED_COLOR = Color.web("#7cfc7c");
    private static final Color SLOT_TEXT_COLOR = Color.web("#e8ecff");

    private final ResourceManager resourceManager;
    private final TextPainter painter = new TextPainter();
    private final IconPainter iconPainter;
    private final SkillCardPainter cardPainter;
    /** 各套预设的快捷键（KeyCode 名），取自塔规则，只用来显示 */
    private final List<String> hotkeys;

    public SkillSetupRenderer(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.iconPainter = new IconPainter(resourceManager, painter);
        this.cardPainter = new SkillCardPainter(painter, iconPainter);
        this.hotkeys = GameRules.get().skill().hotkeys();
    }

    public void render(GraphicsContext gc, SkillSetupMenu menu, double width, double height) {
        PanelStyle.drawBackground(gc, width, height);
        double comboY = height - PADDING - COMBO_HEIGHT;
        drawSkillList(gc, menu, width, comboY - PADDING);
        drawCombo(gc, menu, PADDING, comboY, width - 2 * PADDING, COMBO_HEIGHT);
    }

    // ==================== 技能列表 ====================

    private void drawSkillList(GraphicsContext gc, SkillSetupMenu menu, double width, double bottom) {
        PanelStyle.drawPanel(gc, PADDING, PADDING, width - 2 * PADDING, bottom - PADDING, false);
        PlayerStateManager player = menu.getPlayer();

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 34));
        String name = player.getCharacterName() == null ? "勇者" : player.getCharacterName();
        double nameWidth = painter.measureWidth(name, gc.getFont());
        painter.drawCenteredShadowText(gc, name, width / 2, 58, PanelStyle.TITLE_COLOR);
        Image avatar = resourceManager.getPlayerSprite(Direction.DOWN);
        if (avatar != null) {
            gc.drawImage(avatar, width / 2 + nameWidth / 2 + 14, 22, 44, 44);
        }

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
        painter.drawShadowText(gc, "技能列表：", 40, 100, PanelStyle.SECTION_COLOR);

        List<Skill> skills = menu.getSkills();
        double cardX = 40;
        double cardW = width - 80;
        double top = 116;
        double areaBottom = bottom - 12;
        if (skills.isEmpty()) {
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 24));
            painter.drawShadowText(gc, "尚未掌握任何技能", cardX + 20, top + 50, PanelStyle.DISABLED_COLOR);
            return;
        }

        List<List<String>> layouts = new ArrayList<>();
        List<Double> heights = new ArrayList<>();
        for (Skill skill : skills) {
            List<String> lines = cardPainter.layout(skill, cardW);
            layouts.add(lines);
            heights.add(cardPainter.height(lines));
        }

        // 选中项越过可视区底部时才向下滚，保证选中卡片始终完整可见
        int selected = menu.getSelectedSkillIndex();
        int first = 0;
        while (first < selected && spanHeight(heights, first, selected) > areaBottom - top) {
            first++;
        }

        SkillBook book = player.getSkillBook();
        double y = top;
        int last = first;
        for (int i = first; i < skills.size(); i++) {
            double h = heights.get(i);
            if (y + h > areaBottom) break;
            Skill skill = skills.get(i);
            OwnedSkill owned = book.get(skill.skillId());
            boolean fromEquipment = owned != null && owned.isFromEquipment();
            boolean dimmed = owned != null && skill.isPassive() && !owned.isEnabled();
            cardPainter.draw(gc, skill, layouts.get(i), cardX, y, cardW, h,
                    i == selected, cardTags(skill, fromEquipment), dimmed);
            y += h + 10;
            last = i + 1;
        }

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 18));
        if (first > 0) {
            painter.drawCenteredShadowText(gc, "▲", width / 2, top - 2, PanelStyle.HINT_COLOR);
        }
        if (last < skills.size()) {
            painter.drawCenteredShadowText(gc, "▼", width / 2, bottom - 2, PanelStyle.HINT_COLOR);
        }
    }

    /** 卡片标题后的标记：主动技能标每场可释放次数，装备附带的标 [装备] */
    private static String cardTags(Skill skill, boolean fromEquipment) {
        StringBuilder tags = new StringBuilder();
        if (skill.isActive()) {
            tags.append(skill.hasCastLimit() ? "[每战 " + skill.maxCasts() + " 次]" : "[每战不限次]");
        }
        if (fromEquipment) {
            tags.append(tags.isEmpty() ? "" : " ").append("[装备]");
        }
        return tags.toString();
    }

    /** 从 first 到 last（含）的卡片连同间距的总高度 */
    private static double spanHeight(List<Double> heights, int first, int last) {
        double total = 0;
        for (int i = first; i <= last; i++) {
            total += heights.get(i) + 10;
        }
        return total;
    }

    // ==================== 技能组合 ====================

    private void drawCombo(GraphicsContext gc, SkillSetupMenu menu, double x, double y, double w, double h) {
        PanelStyle.drawPanel(gc, x, y, w, h, true);
        PlayerStateManager player = menu.getPlayer();
        SkillPreset preset = menu.getEditingPreset();
        int presetCount = player.getPresetBook().presetCount();

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 24));
        String title = "技能组合 · " + preset.getName();
        painter.drawShadowText(gc, title, x + 20, y + 36, PanelStyle.SECTION_COLOR);
        double titleWidth = painter.measureWidth(title, gc.getFont());
        boolean active = menu.isEditingPresetActive();
        double tabsX = x + 44 + titleWidth;
        drawPresetTabs(gc, player.getPresetBook(), menu.getEditingPresetIndex(), active, tabsX, y + 14);
        if (!active) {
            // 游戏中停用了技能组：只在这种情况下提示，放入技能即重新激活
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 18));
            painter.drawShadowText(gc, "当前未激活（全程普攻）", tabsX + player.getPresetBook().presetCount() * 36 + 12,
                    y + 36, PanelStyle.DISABLED_COLOR);
        }
        if (!menu.getNotice().isEmpty()) {
            painter.drawRightAlignedShadowText(gc, menu.getNotice(), x + w - 20, y + 36, PanelStyle.NOTICE_COLOR);
        } else if (!player.isPresetEditable()) {
            painter.drawRightAlignedShadowText(gc, "本塔的技能组合由规则固定", x + w - 20, y + 36, PanelStyle.HINT_COLOR);
        }

        int slots = preset.slotCount();
        double gap = 8;
        double slotW = (w - 40 - gap * (slots - 1)) / slots;
        double slotH = 72;
        double slotY = y + 72;
        SkillBook book = player.getSkillBook();

        for (int round = 0; round < slots; round++) {
            double sx = x + 20 + round * (slotW + gap);
            gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 15));
            painter.drawCenteredShadowText(gc, round == 0 ? "战前" : "第" + round + "回",
                    sx + slotW / 2, slotY - 8, PanelStyle.HINT_COLOR);

            boolean selected = round == menu.getSelectedRound();
            if (selected) {
                PanelStyle.drawSelection(gc, sx - 3, slotY - 3, slotW + 6, slotH + 6, true);
            }
            drawSlot(gc, book, preset.getSlot(round), round, sx, slotY, slotW, slotH);
        }

        gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 16));
        String hint = player.isPresetEditable()
                ? "↑↓ 选技能   ←→ 选回合   Enter 放入   Delete 卸下   数字键 切换技能组   X 返回游戏"
                : "↑↓ 查看技能   ←→ 查看回合   数字键 切换技能组   X 返回游戏";
        painter.drawShadowText(gc, hint, x + 20, y + h - 18, PanelStyle.HINT_COLOR);
    }

    /**
     * 一排小方块，每块标着该套预设的快捷键（1~9、0），当前那套高亮；
     * 编排过技能的预设底色稍亮，一眼看出哪些键上有东西。
     */
    private void drawPresetTabs(GraphicsContext gc, SkillPresetBook book, int current, boolean active,
                                double x, double y) {
        double size = 30;
        double gap = 6;
        for (int i = 0; i < book.presetCount(); i++) {
            double tx = x + i * (size + gap);
            boolean selected = i == current;
            boolean filled = !book.get(i).isEmpty();
            // 当前那套：激活时绿色，未激活（全程普攻）时只画白框
            gc.setFill(selected && active ? Color.rgb(124, 252, 124, 0.35)
                    : filled ? Color.rgb(255, 255, 255, 0.16) : Color.rgb(255, 255, 255, 0.05));
            gc.fillRect(tx, y, size, size);
            gc.setStroke(selected ? (active ? ARMED_COLOR : Color.WHITE) : PanelStyle.PANEL_BORDER_DIM);
            gc.setLineWidth(selected ? 2 : 1);
            gc.strokeRect(tx, y, size, size);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
            painter.drawCenteredShadowText(gc, keyLabel(i), tx + size / 2, y + 22,
                    selected ? Color.WHITE : filled ? SLOT_TEXT_COLOR : PanelStyle.DISABLED_COLOR);
        }
    }

    /** 第 i 套预设的快捷键显示名：规则里写的是 {@code DIGIT1} 这类 KeyCode 名，去掉前缀只留数字 */
    private String keyLabel(int presetIndex) {
        if (presetIndex >= hotkeys.size()) return "";
        String key = hotkeys.get(presetIndex);
        return key.startsWith("DIGIT") ? key.substring(5) : key;
    }

    private void drawSlot(GraphicsContext gc, SkillBook book, String skillId, int round,
                          double x, double y, double w, double h) {
        if (skillId == null) {
            gc.setFill(Color.rgb(255, 255, 255, 0.06));
            gc.fillRect(x, y, w, h);
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 20));
            // 战前没有「普攻」可言，空着就是什么也不做
            painter.drawCenteredShadowText(gc, round == 0 ? "—" : "普攻", x + w / 2, y + h / 2 + 7, SLOT_TEXT_COLOR);
            return;
        }
        OwnedSkill owned = book.get(skillId);
        if (owned == null) {
            // 预设引用的技能暂时不在手上（例如装备卸下了）：保留占位，战斗中按普攻处理
            gc.setFill(Color.rgb(120, 40, 40, 0.35));
            gc.fillRect(x, y, w, h);
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 16));
            painter.drawCenteredShadowText(gc, "失效", x + w / 2, y + h / 2 + 6, PanelStyle.DISABLED_COLOR);
            return;
        }
        double size = Math.min(w, h) - 8;
        iconPainter.drawSkillIcon(gc, owned.skill(), x + (w - size) / 2, y + (h - size) / 2, size, size);
    }
}
