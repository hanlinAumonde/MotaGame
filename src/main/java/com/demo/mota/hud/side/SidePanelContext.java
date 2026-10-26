package com.demo.mota.hud.side;

import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.kit.Icons;
import com.demo.mota.ui.kit.Sketch;

/**
 * 分区摆放时可用的全部东西：画板、玩家状态、图标工具。
 */
public record SidePanelContext(Sketch sketch, PlayerStateManager player, Icons icons) {
}
