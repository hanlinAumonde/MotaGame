package com.demo.mota.ui.kit;

import com.demo.mota.engine.skill.Skill;
import com.demo.mota.ui.Fonts;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能卡片（图标 / 「技能名（类型）」/ 折行说明，原 {@code SkillCardPainter}），敌物资料的技能页与技能设置界面共用。
 */
public final class SkillCards {

    public static final double ICON_WIDTH = 104;
    private static final double LINE_HEIGHT = 26;

    private final Icons icons;
    private final TextMetrics metrics = new TextMetrics();

    public SkillCards(Icons icons) {
        this.icons = icons;
    }

    private static Font descFont() {
        return Fonts.regular(18);
    }

    /** 先按说明里的手动换行拆行，再按卡片可用宽度折行 */
    public List<String> layout(Skill skill, double cardWidth) {
        double maxWidth = cardWidth - ICON_WIDTH - 56;
        List<String> lines = new ArrayList<>();
        for (String paragraph : skill.descriptionLines()) {
            lines.addAll(metrics.wrap(paragraph, descFont(), maxWidth));
        }
        return lines;
    }

    public double height(List<String> lines) {
        return Math.max(104, 64 + lines.size() * LINE_HEIGHT);
    }

    /**
     * @param titleSuffix 标题行末尾追加的标记（例如「[装备]」），为空则不显示
     * @param dimmed      是否显示成未生效的暗色
     */
    public void card(Sketch sketch, Skill skill, List<String> lines, double x, double y, double w, double h,
                     boolean selected, String titleSuffix, boolean dimmed) {
        sketch.panel(x, y, w, h, selected);
        if (selected) {
            sketch.fill(x, y, w, h, Color.rgb(255, 255, 255, 0.08));
        }

        icons.skill(sketch, skill, x + 14, y + 16, ICON_WIDTH, h - 32, dimmed);

        double textX = x + 14 + ICON_WIDTH + 20;
        Font titleFont = Fonts.bold(22);
        String title = skill.skillName() + "（" + skill.skillType().getDisplayName() + "）";
        sketch.text(title, titleFont, dimmed ? Palette.DISABLED : Palette.SKILL_TITLE, textX, y + 42);
        if (titleSuffix != null && !titleSuffix.isEmpty()) {
            sketch.text(titleSuffix, Fonts.bold(18), Color.web("#d6a2ff"),
                    textX + sketch.width(title, titleFont) + 10, y + 42);
        }

        Font descFont = descFont();
        for (int i = 0; i < lines.size(); i++) {
            sketch.text(lines.get(i), descFont, Palette.DESC, textX, y + 72 + i * LINE_HEIGHT);
        }
    }
}
