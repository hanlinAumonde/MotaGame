package com.demo.mota.ui.screen.gamemenu.option;

import java.util.List;

/**
 * 「游戏选项」弹出小窗的选择状态。由 {@code GameMenuState} 持有，弹出时 {@link #reset()}。
 */
public class GameOptionPopup {

    private int selectedIndex;

    public void reset() {
        selectedIndex = 0;
    }

    /** 上下循环选择 */
    public void move(int delta) {
        int size = GameOption.values().length;
        selectedIndex = ((selectedIndex + delta) % size + size) % size;
    }

    public List<GameOption> getOptions() {
        return List.of(GameOption.values());
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public GameOption getSelected() {
        return GameOption.values()[selectedIndex];
    }
}
