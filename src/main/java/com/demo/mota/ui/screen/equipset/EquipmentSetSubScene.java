package com.demo.mota.ui.screen.equipset;

import com.demo.mota.app.nav.Navigator;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.InGameSubScene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

import java.util.function.Consumer;

/**
 * 套装界面（对局中按 A，或经游戏选项「套装设定」）：{@link EquipmentSetState} + {@link EquipmentSetRenderer}。
 *
 * <p>只用方向键选择、回车换上并回到游戏；本界面不设数字快捷键（对局中直接 W+数字键换装）。
 * 换装的提示经 {@code messageSink} 交给对局界面的状态栏；伤害照旧由对局界面在回来时统一重算。
 */
public class EquipmentSetSubScene extends InGameSubScene {

    private final SessionService sessions;
    private final EquipmentSetRenderer renderer;
    private final Consumer<String> messageSink;
    private EquipmentSetState state;

    public EquipmentSetSubScene(Navigator navigator, Runnable onReturnToGame, SessionService sessions,
                                EquipmentSetRenderer renderer, Consumer<String> messageSink) {
        super(navigator, onReturnToGame);
        this.sessions = sessions;
        this.renderer = renderer;
        this.messageSink = messageSink;
    }

    @Override
    protected void onOpen() {
        state = new EquipmentSetState(sessions.session().player());
        state.open();
    }

    @Override
    protected boolean handleScreenKey(KeyCode code) {
        state.handle(toCommand(code));
        if (state.isOpen()) {
            return false;
        }
        String message = state.takeMessage();
        if (message != null) {
            messageSink.accept(message);
        }
        return true;
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
