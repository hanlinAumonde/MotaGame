package com.demo.mota.ui;

import com.demo.mota.engine.skill.Skill;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能卡片（图标 / 「技能名（类型）」/ 折行说明），敌物资料的技能页与技能设置界面共用。
 */
public class SkillCardPainter {

    public static final double ICON_WIDTH = 104;
    private static final double LINE_HEIGHT = 26;

    private final TextPainter painter;
    private final IconPainter iconPainter;
    private final Font descFont = Font.font("SimHei", FontWeight.NORMAL, 18);

    public SkillCardPainter(TextPainter painter, IconPainter iconPainter) {
        this.painter = painter;
        this.iconPainter = iconPainter;
    }

    /** 先按说明里的手动换行拆行，再按卡片可用宽度折行 */
    public List<String> layout(Skill skill, double cardWidth) {
        double maxWidth = cardWidth - ICON_WIDTH - 56;
        List<String> lines = new ArrayList<>();
        for (String paragraph : skill.descriptionLines()) {
            lines.addAll(painter.wrap(paragraph, descFont, maxWidth));
        }
        return lines;
    }

    public double height(List<String> lines) {
        return Math.max(104, 64 + lines.size() * LINE_HEIGHT);
    }

    /**
     * @param titleSuffix 标题行末尾追加的标记（例如「[装备]」），为空则不画
     * @param dimmed      是否画成未生效的暗色
     */
    public void draw(GraphicsContext gc, Skill skill, List<String> lines,
                     double x, double y, double w, double h,
                     boolean selected, String titleSuffix, boolean dimmed) {
        PanelStyle.drawPanel(gc, x, y, w, h, selected);
        if (selected) {
            gc.setFill(PanelStyle.FOCUS_FILL_DIM);
            gc.fillRect(x, y, w, h);
        }

        iconPainter.drawSkillIcon(gc, skill, x + 14, y + 16, ICON_WIDTH, h - 32, dimmed);

        double textX = x + 14 + ICON_WIDTH + 20;
        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 22));
        String title = skill.skillName() + "（" + skill.skillType().getDisplayName() + "）";
        painter.drawShadowText(gc, title, textX, y + 42,
                dimmed ? PanelStyle.DISABLED_COLOR : PanelStyle.SKILL_TITLE_COLOR);
        if (titleSuffix != null && !titleSuffix.isEmpty()) {
            double titleWidth = painter.measureWidth(title, gc.getFont());
            gc.setFont(Font.font("SimHei", FontWeight.BOLD, 18));
            painter.drawShadowText(gc, titleSuffix, textX + titleWidth + 10, y + 42, Color.web("#d6a2ff"));
        }

        gc.setFont(descFont);
        for (int i = 0; i < lines.size(); i++) {
            painter.drawShadowText(gc, lines.get(i), textX, y + 72 + i * LINE_HEIGHT, PanelStyle.DESC_COLOR);
        }
    }
}
