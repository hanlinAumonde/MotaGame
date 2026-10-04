package com.demo.mota.ui.screen.equipment;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.InGameScreen;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

/**
 * 装备界面（{@link GamePhase#EQUIPMENT}，对局中按 Q）：{@link EquipmentState} + {@link EquipmentRenderer}。
 * 穿脱造成的伤害变化由对局界面在回到游戏时统一重算。
 */
public class EquipmentScreen extends InGameScreen {

    private final GameEngine engine;
    private final EquipmentRenderer renderer;
    private EquipmentState state;

    public EquipmentScreen(GameEngine engine, GameFlow flow, Canvas canvas, EquipmentRenderer renderer) {
        super(flow, canvas);
        this.engine = engine;
        this.renderer = renderer;
    }

    @Override
    public void onNewGame() {
        this.state = new EquipmentState(engine.getPlayerStateManager());
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
            case LEFT, A -> MenuCommand.LEFT;
            case RIGHT, D -> MenuCommand.RIGHT;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            case ESCAPE -> MenuCommand.BACK;
            default -> null;
        };
    }
}
