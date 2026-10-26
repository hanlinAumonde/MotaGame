package com.demo.mota.hud.side;

import com.demo.mota.app.services.AssetService;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.kit.Icons;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.layout.Pane;
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
public class SidePanelView {

    private static final double PADDING = 16;
    private static final double MIN_GAP = 12;

    private final double width;
    private final double height;
    private final Sketch sketch = new Sketch();
    private final Icons icons;
    private final List<String> sectionIds;

    public SidePanelView(AssetService assets, List<String> sectionIds, double width, double height) {
        this.icons = new Icons(assets);
        this.sectionIds = List.copyOf(sectionIds);
        this.width = width;
        this.height = height;
        sketch.node().setPrefSize(width, height);
        sketch.node().setMinSize(width, height);
        sketch.node().setMaxSize(width, height);
    }

    public Pane node() {
        return sketch.node();
    }

    /** 按玩家当前状态重新摆放（技能组合、被动技能、装备都可能变） */
    public void refresh(PlayerStateManager player) {
        sketch.clear();
        sketch.fill(0, 0, width, height, Color.BLACK);

        SidePanelContext context = new SidePanelContext(sketch, player, icons);
        SidePanelSectionRegistry registry = SidePanelSectionRegistry.getInstance();
        List<SidePanelSection> sections = new ArrayList<>();
        List<Double> heights = new ArrayList<>();
        double total = 0;
        for (String id : sectionIds) {
            SidePanelSection section = registry.get(id);
            if (section == null) continue;
            double sectionHeight = section.height(context, width);
            sections.add(section);
            heights.add(sectionHeight);
            total += sectionHeight;
        }
        if (sections.isEmpty()) return;

        double free = height - 2 * PADDING - total;
        double gap = sections.size() > 1 ? Math.max(MIN_GAP, free / (sections.size() - 1)) : 0;
        double y = PADDING;
        for (int i = 0; i < sections.size(); i++) {
            sections.get(i).render(context, 0, y, width);
            y += heights.get(i) + gap;
        }
    }
}
