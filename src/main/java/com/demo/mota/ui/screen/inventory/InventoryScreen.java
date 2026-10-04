package com.demo.mota.ui.screen.inventory;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.InGameScreen;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

/**
 * 物品栏（{@link GamePhase#INVENTORY}）：{@link InventoryState} + {@link InventoryRenderer}。
 *
 * <p>目前没有专属快捷键，只能从游戏菜单「游戏选项 → 物品栏」进入；
 * 要加快捷键，在 {@code GameScreen} 的界面快捷键表里加一项并在 {@code GameFlow} 转移表中放行即可。
 */
public class InventoryScreen extends InGameScreen {

    private final GameEngine engine;
    private final InventoryRenderer renderer;
    private InventoryState state;

    public InventoryScreen(GameEngine engine, GameFlow flow, Canvas canvas, InventoryRenderer renderer) {
        super(flow, canvas);
        this.engine = engine;
        this.renderer = renderer;
    }

    @Override
    public void onNewGame() {
        this.state = new InventoryState(engine.getPlayerStateManager());
    }

    @Override
    public void onEnter() {
        state.open();
    }

    @Override
    protected GamePhase handleScreenKey(KeyCode code) {
        state.handle(toCommand(code));
        return state.isOpen() ? null : GamePhase.PLAYING;
    }

    @Override
    protected void render(GraphicsContext gc, double width, double height) {
        renderer.render(gc, state, width, height);
    }

    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP, W -> MenuCommand.UP;
            case DOWN, S -> MenuCommand.DOWN;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            case ESCAPE -> MenuCommand.BACK;
            default -> null;
        };
    }
}
