package com.demo.mota.ui.screen.equipset;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.InGameScreen;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

import java.util.function.Consumer;

/**
 * 套装界面（{@link GamePhase#EQUIPMENT_SETS}，对局中按 A，或经游戏选项「套装设定」）：
 * {@link EquipmentSetState} + {@link EquipmentSetRenderer}。
 *
 * <p>只用方向键选择、回车换上并回到游戏；本界面不设数字快捷键（对局中直接 W+数字键换装）。
 * 换装的提示经 {@code messageSink} 交给对局界面的状态栏；伤害照旧由对局界面在回来时统一重算。
 */
public class EquipmentSetScreen extends InGameScreen {

    private final GameEngine engine;
    private final EquipmentSetRenderer renderer;
    private final Consumer<String> messageSink;
    private EquipmentSetState state;

    public EquipmentSetScreen(GameEngine engine, GameFlow flow, Canvas canvas, EquipmentSetRenderer renderer,
                              Consumer<String> messageSink) {
        super(flow, canvas);
        this.engine = engine;
        this.renderer = renderer;
        this.messageSink = messageSink;
    }

    @Override
    public void onNewGame() {
        this.state = new EquipmentSetState(engine.getPlayerStateManager());
    }

    @Override
    public void onEnter() {
        state.open();
    }

    @Override
    protected GamePhase handleScreenKey(KeyCode code) {
        state.handle(toCommand(code));
        if (state.isOpen()) {
            return null;
        }
        String message = state.takeMessage();
        if (message != null) {
            messageSink.accept(message);
        }
        return GamePhase.PLAYING;
    }

    @Override
    protected void render(GraphicsContext gc, double width, double height) {
        renderer.render(gc, state, width, height);
    }

    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP -> MenuCommand.UP;
            case DOWN -> MenuCommand.DOWN;
            case LEFT -> MenuCommand.LEFT;
            case RIGHT -> MenuCommand.RIGHT;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            case ESCAPE -> MenuCommand.BACK;
            default -> null;
        };
    }
}
