package com.demo.mota.ui.side;

import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.rules.GameRules;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.IconPainter;
import com.demo.mota.ui.TextPainter;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * 右侧技能 / 装备栏：纯黑底，按塔规则 {@code sidePanel.sections} 的顺序纵向排布各分区。
 *
 * <p>排布方式为「两端对齐」：先量出每个分区的高度，剩余空间均分到分区之间，
 * 首个分区贴顶、末个分区贴底——默认配置里装备栏排在最后，因此始终位于底部。
 * 内容多到放不下时间距退到 {@link #MIN_GAP}。
 */
public class SidePanelRenderer {

    private static final double PADDING = 16;
    private static final double MIN_GAP = 12;

    private final TextPainter painter = new TextPainter();
    private final IconPainter iconPainter;
    private final List<String> sectionIds;

    public SidePanelRenderer(ResourceManager resourceManager) {
        this(resourceManager, GameRules.get().sidePanel().sections());
    }

    public SidePanelRenderer(ResourceManager resourceManager, List<String> sectionIds) {
        this.iconPainter = new IconPainter(resourceManager, painter);
        this.sectionIds = List.copyOf(sectionIds);
    }

    public void render(Canvas canvas, PlayerStateManager player) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        SidePanelContext context = new SidePanelContext(gc, player, painter, iconPainter);
        SidePanelSectionRegistry registry = SidePanelSectionRegistry.getInstance();
        List<SidePanelSection> sections = new ArrayList<>();
        List<Double> heights = new ArrayList<>();
        double total = 0;
        for (String id : sectionIds) {
            SidePanelSection section = registry.get(id);
            if (section == null) continue;
            double sectionHeight = section.height(context, w);
            sections.add(section);
            heights.add(sectionHeight);
            total += sectionHeight;
        }
        if (sections.isEmpty()) return;

        double free = h - 2 * PADDING - total;
        double gap = sections.size() > 1 ? Math.max(MIN_GAP, free / (sections.size() - 1)) : 0;
        double y = PADDING;
        for (int i = 0; i < sections.size(); i++) {
            sections.get(i).render(context, 0, y, w);
            y += heights.get(i) + gap;
        }
    }
}
