package com.demo.mota.ui.side;

import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.IconPainter;
import com.demo.mota.ui.TextPainter;
import javafx.scene.canvas.GraphicsContext;

/**
 * 分区绘制时可用的全部东西：画布、玩家状态、文字与图标工具。
 */
public record SidePanelContext(GraphicsContext gc, PlayerStateManager player,
                               TextPainter painter, IconPainter icons) {
}
