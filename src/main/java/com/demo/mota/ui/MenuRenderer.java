package com.demo.mota.ui;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.StateType;
import com.demo.mota.engine.menu.GameMenu;
import com.demo.mota.engine.menu.MenuEntry;
import com.demo.mota.engine.menu.MenuPage;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.state.monster.Monster;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏内菜单的绘制器：把 {@link GameMenu} 的状态画到覆盖全窗口的菜单画布上。
 *
 * <p>只读取菜单状态与游戏数据，不修改任何状态。左侧菜单项 + 右侧敌物列表共用一屏
 * （{@link MenuPage#ROOT} 与 {@link MenuPage#MONSTER_LIST} 的区别只在焦点落于哪一侧），
 * 技能详情（{@link MenuPage#MONSTER_SKILL}）则整屏替换。
 */
public class MenuRenderer {

    // --- 布局 ---
    private static final double PADDING = 12;
    private static final double LEFT_WIDTH = 236;
    private static final double LEFT_ITEM_HEIGHT = 46;
    private static final double RIGHT_X = PADDING + LEFT_WIDTH + PADDING;

    /** 敌物列表每行高度与行距，以及一屏可见的行数 */
    private static final double ROW_HEIGHT = 140;
    private static final double ROW_GAP = 10;
    private static final int VISIBLE_ROWS = 5;

    // --- 配色 ---
    private static final Color PANEL_FILL = Color.rgb(24, 28, 62, 0.86);
    private static final Color PANEL_BORDER = Color.web("#aab6ff");
    private static final Color PANEL_BORDER_DIM = Color.rgb(170, 182, 255, 0.45);
    private static final Color FOCUS_FILL = Color.rgb(255, 255, 255, 0.20);
    private static final Color FOCUS_FILL_DIM = Color.rgb(255, 255, 255, 0.08);
    private static final Color LABEL_COLOR = Color.web("#4cf04c");
    private static final Color VALUE_COLOR = Color.web("#9fe8ff");
    private static final Color GOLD_COLOR = Color.web("#ffd95a");
    private static final Color EXP_COLOR = Color.web("#b9ff7a");
    private static final Color HINT_COLOR = Color.web("#c6ccf5");
    private static final Color PASSIVE_SKILL_COLOR = Color.web("#ff9ad8");
    private static final Color ACTIVE_SKILL_COLOR = Color.web("#ff7a6b");
    private static final Color PLAIN_MONSTER_COLOR = Color.web("#9aa5b1");
    private static final Color TITLE_COLOR = Color.web("#ffb144");
    /** 技能详情页的技能名统一用绿色，技能类型仅在列表标签上以颜色区分 */
    private static final Color SKILL_TITLE_COLOR = Color.web("#7cfc7c");
    private static final Color SECTION_COLOR = Color.web("#7fdfff");
    private static final Color DESC_COLOR = Color.web("#bfe9ff");

    private final ResourceManager resourceManager;
    private final TextPainter painter = new TextPainter();

    public MenuRenderer(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void render(GraphicsContext gc, GameMenu menu, PlayerStateManager player,
                       double width, double height) {
        drawBackground(gc, width, height);
        if (menu.getPage() == MenuPage.MONSTER_SKILL) {
            drawSkillPage(gc, menu, width, height);
        } else {
            drawLeftPanel(gc, menu);
            drawMonsterList(gc, menu, player, width, height);
        }
    }

    private void drawBackground(GraphicsContext gc, double width, double height) {
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2e3566")), new Stop(1, Color.web("#171a33"))));
        gc.fillRect(0, 0, width, height);
    }

    // ==================== 左侧菜单项 ====================

    private void drawLeftPanel(GraphicsContext gc, GameMenu menu) {
        List<MenuEntry> entries = menu.getEntries();
        double boxH = 20 + entries.size() * LEFT_ITEM_HEIGHT;
        drawPanel(gc, PADDING, PADDING, LEFT_WIDTH, boxH, menu.getPage() == MenuPage.ROOT);

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 26));
        for (int i = 0; i < entries.size(); i++) {
            double itemY = PADDING + 10 + i * LEFT_ITEM_HEIGHT;
            if (i == menu.getSelectedEntryIndex()) {
                // 焦点移到右侧内容后，左侧选中项保留一个暗一些的高亮，提示所处分类
                boolean focused = menu.getPage() == MenuPage.ROOT;
                gc.setFill(focused ? FOCUS_FILL : FOCUS_FILL_DIM);
                gc.fillRect(PADDING + 8, itemY, LEFT_WIDTH - 16, LEFT_ITEM_HEIGHT - 4);
                gc.setStroke(focused ? PANEL_BORDER : PANEL_BORDER_DIM);
                gc.setLineWidth(2);
                gc.strokeRect(PADDING + 8, itemY, LEFT_WIDTH - 16, LEFT_ITEM_HEIGHT - 4);
            }
            painter.drawShadowText(gc, entries.get(i).getDisplayName(),
                    PADDING + 22, itemY + LEFT_ITEM_HEIGHT * 0.68);
        }

        double infoY = PADDING + boxH + 14;
        drawPanel(gc, PADDING, infoY, LEFT_WIDTH, 92, false);
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
        painter.drawShadowText(gc, "当前楼层", PADDING + 18, infoY + 34);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 28));
        painter.drawShadowText(gc, "第 " + menu.getFloorNumber() + " 层", PADDING + 18, infoY + 74);

        double hintY = infoY + 92 + 14;
        drawPanel(gc, PADDING, hintY, LEFT_WIDTH, 150, false);
        gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 17));
        String[] hints = {"↑ ↓ / W S   选择", "Enter / Space   确认", "Esc   返回上一层", "X   关闭菜单"};
        for (int i = 0; i < hints.length; i++) {
            painter.drawShadowText(gc, hints[i], PADDING + 16, hintY + 34 + i * 30, HINT_COLOR);
        }
    }

    // ==================== 右侧敌物列表 ====================

    private void drawMonsterList(GraphicsContext gc, GameMenu menu, PlayerStateManager player,
                                 double width, double height) {
        double listX = RIGHT_X;
        double listW = width - RIGHT_X - PADDING;
        List<Monster> monsters = menu.getMonsters();

        if (monsters.isEmpty()) {
            drawPanel(gc, listX, PADDING, listW, 110, false);
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 26));
            painter.drawShadowText(gc, "本层没有敌物", listX + 28, PADDING + 66, PLAIN_MONSTER_COLOR);
            return;
        }

        boolean listFocused = menu.getPage() == MenuPage.MONSTER_LIST;
        int selected = menu.getSelectedMonsterIndex();
        // 选中项越过可视区底部时才滚动，保证选中行始终可见
        int first = Math.max(0, Math.min(selected - VISIBLE_ROWS + 1, monsters.size() - VISIBLE_ROWS));
        int last = Math.min(monsters.size(), first + VISIBLE_ROWS);

        for (int i = first; i < last; i++) {
            double rowY = PADDING + 8 + (i - first) * (ROW_HEIGHT + ROW_GAP);
            drawMonsterRow(gc, monsters.get(i), player, listX, rowY, listW,
                    listFocused && i == selected);
        }

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 20));
        if (first > 0) {
            painter.drawCenteredShadowText(gc, "▲", listX + listW / 2, PADDING + 4, HINT_COLOR);
        }
        if (last < monsters.size()) {
            painter.drawCenteredShadowText(gc, "▼", listX + listW / 2, height - 10, HINT_COLOR);
        }
    }

    private void drawMonsterRow(GraphicsContext gc, Monster monster, PlayerStateManager player,
                                double x, double y, double w, boolean selected) {
        drawPanel(gc, x, y, w, ROW_HEIGHT, selected);
        if (selected) {
            gc.setFill(FOCUS_FILL);
            gc.fillRect(x, y, w, ROW_HEIGHT);
        }

        // --- 头像与回合数 ---
        double iconSize = 60;
        double iconCenterX = x + 18 + iconSize / 2;
        Image icon = resourceManager.getMonsterImage(monster.getCharacterId());
        if (icon != null) {
            gc.drawImage(icon, x + 18, y + 14, iconSize, iconSize);
        } else {
            gc.setFill(Color.web("#cc0000"));
            gc.fillOval(x + 18, y + 14, iconSize, iconSize);
        }
        gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 15));
        painter.drawCenteredShadowText(gc, "回合数", iconCenterX, y + 96, HINT_COLOR);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 22));
        painter.drawCenteredShadowText(gc, roundsText(monster), iconCenterX, y + 124, VALUE_COLOR);

        // --- 名称与技能标签 ---
        double nameCenterX = x + 190;
        // 名称栏宽度有限（右侧紧接属性网格），过长的名字按可用宽度缩字号
        gc.setFont(painter.fitFont(monster.getCharacterName(), "SimHei", 26, 16, 220));
        painter.drawCenteredShadowText(gc, monster.getCharacterName(), nameCenterX, y + 52, Color.WHITE);
        drawSkillTags(gc, monster, nameCenterX, y + 84);

        // --- 属性网格：两列三行 ---
        GameNumber maxHp = player.getMaxHP();
        double col1Label = x + 320;
        double col1Value = x + 470;
        double col2Label = x + 520;
        double col2Value = x + w - 26;
        double[] lineY = {y + 46, y + 88, y + 128};

        drawStat(gc, "生命", ValueFormatter.formatScaled(monster.getStateValue(StateType.HP), maxHp),
                col1Label, col1Value, lineY[0], VALUE_COLOR);
        drawStat(gc, "攻击", ValueFormatter.formatScaled(monster.getStateValue(StateType.ATK), maxHp),
                col2Label, col2Value, lineY[0], VALUE_COLOR);
        drawStat(gc, "防御", ValueFormatter.formatScaled(monster.getStateValue(StateType.DEF), maxHp),
                col1Label, col1Value, lineY[1], VALUE_COLOR);
        drawStat(gc, "伤害", damageText(monster, maxHp),
                col2Label, col2Value, lineY[1],
                DamagePalette.colorOnDark(monster.getCurrentDamageRange(), monster.isLethalTo(player)));
        drawStat(gc, "金币", String.valueOf(monster.getGoldReward()),
                col1Label, col1Value, lineY[2], GOLD_COLOR);
        drawStat(gc, "经验", ValueFormatter.formatScaled(monster.getExperienceReward(), maxHp),
                col2Label, col2Value, lineY[2], EXP_COLOR);
    }

    /** 一格「绿色标签 + 右对齐数值」，数值过长时自动缩字号 */
    private void drawStat(GraphicsContext gc, String label, String value,
                          double labelX, double valueRightX, double baselineY, Color valueColor) {
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
        painter.drawShadowText(gc, label, labelX, baselineY, LABEL_COLOR);

        double maxWidth = valueRightX - (labelX + 70);
        gc.setFont(painter.fitFont(value, "Consolas", 22, 14, maxWidth));
        painter.drawRightAlignedShadowText(gc, value, valueRightX, baselineY, valueColor);
    }

    /** 技能名标签；没有技能的怪物标注「普通」，与原版手册保持一致 */
    private void drawSkillTags(GraphicsContext gc, Monster monster, double centerX, double baselineY) {
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 16));
        if (!monster.hasSkills()) {
            painter.drawCenteredShadowText(gc, "普通", centerX, baselineY, PLAIN_MONSTER_COLOR);
            return;
        }
        List<Skill> skills = monster.getSkills();
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            painter.drawCenteredShadowText(gc, skill.skillName(), centerX, baselineY + i * 22,
                    skillColor(skill));
        }
    }

    private static Color skillColor(Skill skill) {
        return switch (skill.skillType()) {
            case PASSIVE -> PASSIVE_SKILL_COLOR;
            case ACTIVE -> ACTIVE_SKILL_COLOR;
        };
    }

    private static String roundsText(Monster monster) {
        return monster.getCurrentDamageRange() == DamageRange.OVER_KILL
                ? "-" : String.valueOf(monster.getCurrentRounds());
    }

    private static String damageText(Monster monster, GameNumber maxHp) {
        if (monster.getCurrentDamageRange() == DamageRange.OVER_KILL) {
            return "???";
        }
        if (monster.getCurrentDamage() == null) {
            return "?";
        }
        return ValueFormatter.formatScaled(monster.getCurrentDamage(), maxHp);
    }

    // ==================== 技能详情页 ====================

    private void drawSkillPage(GraphicsContext gc, GameMenu menu, double width, double height) {
        Monster monster = menu.getSelectedMonster();
        if (monster == null) return;

        // 标题：怪物名居中，右侧跟一个小头像
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 36));
        double nameWidth = painter.measureWidth(monster.getCharacterName(), gc.getFont());
        painter.drawCenteredShadowText(gc, monster.getCharacterName(), width / 2, 66, TITLE_COLOR);
        Image icon = resourceManager.getMonsterImage(monster.getCharacterId());
        if (icon != null) {
            gc.drawImage(icon, width / 2 + nameWidth / 2 + 16, 30, 44, 44);
        }

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
        painter.drawShadowText(gc, "技能列表：", 40, 120, SECTION_COLOR);

        double cardX = 40;
        double cardW = width - 80;
        double cardY = 140;
        Font descFont = Font.font("SimHei", FontWeight.NORMAL, 18);
        List<Skill> skills = monster.getSkills();

        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            List<String> lines = layoutDescription(skill, descFont, cardW - 160);
            double cardH = Math.max(104, 64 + lines.size() * 26);
            if (cardY + cardH > height - 60) break;

            drawSkillCard(gc, skill, lines, descFont, cardX, cardY, cardW, cardH,
                    i == menu.getSelectedSkillIndex());
            cardY += cardH + 12;
        }

        gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 17));
        painter.drawShadowText(gc, "↑ ↓ 切换技能    Esc 返回敌物列表    X 关闭菜单",
                40, height - 28, HINT_COLOR);
    }

    private void drawSkillCard(GraphicsContext gc, Skill skill, List<String> lines, Font descFont,
                               double x, double y, double w, double h, boolean selected) {
        drawPanel(gc, x, y, w, h, selected);
        if (selected) {
            gc.setFill(FOCUS_FILL_DIM);
            gc.fillRect(x, y, w, h);
        }

        // 图标：技能美术资源缺失时用带首字的色块占位
        double iconW = 104;
        double iconH = h - 32;
        Image skillIcon = resourceManager.getSkillImage(skill.skillId());
        if (skillIcon != null) {
            gc.drawImage(skillIcon, x + 14, y + 16, iconW, iconH);
        } else {
            gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#6a4b2a")), new Stop(1, Color.web("#2d1e10"))));
            gc.fillRect(x + 14, y + 16, iconW, iconH);
            gc.setStroke(PANEL_BORDER_DIM);
            gc.setLineWidth(1);
            gc.strokeRect(x + 14, y + 16, iconW, iconH);
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 34));
            painter.drawCenteredShadowText(gc, skill.skillName().substring(0, 1),
                    x + 14 + iconW / 2, y + 16 + iconH / 2 + 12, Color.web("#ffcf8a"));
        }

        double textX = x + 14 + iconW + 20;
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
        painter.drawShadowText(gc, skill.skillName() + "（" + skill.skillType().getDisplayName() + "）",
                textX, y + 42, SKILL_TITLE_COLOR);

        gc.setFont(descFont);
        for (int i = 0; i < lines.size(); i++) {
            painter.drawShadowText(gc, lines.get(i), textX, y + 72 + i * 26, DESC_COLOR);
        }
    }

    /** 先按说明里的手动换行拆行，再按卡片可用宽度折行 */
    private List<String> layoutDescription(Skill skill, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : skill.descriptionLines()) {
            lines.addAll(painter.wrap(paragraph, font, maxWidth));
        }
        return lines;
    }

    // ==================== 通用面板 ====================

    private void drawPanel(GraphicsContext gc, double x, double y, double w, double h, boolean focused) {
        gc.setFill(PANEL_FILL);
        gc.fillRect(x, y, w, h);
        gc.setStroke(focused ? PANEL_BORDER : PANEL_BORDER_DIM);
        gc.setLineWidth(focused ? 3 : 1.5);
        gc.strokeRect(x, y, w, h);
    }
}
