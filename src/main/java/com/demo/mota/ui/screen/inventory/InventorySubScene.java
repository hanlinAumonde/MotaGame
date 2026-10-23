package com.demo.mota.ui.screen.inventory;

import com.demo.mota.app.nav.Navigator;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.InGameSubScene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

/**
 * 物品栏：{@link InventoryState} + {@link InventoryRenderer}。
 *
 * <p>目前没有专属快捷键，只能从游戏菜单「游戏选项 → 物品栏」进入；
 * 要加快捷键，在 {@code GameplayInput} 里加一个动作、经 {@code Navigator} 叠上本场景即可。
 */
public class InventorySubScene extends InGameSubScene {

    private final SessionService sessions;
    private final InventoryRenderer renderer;
    private InventoryState state;

    public InventorySubScene(Navigator navigator, Runnable onReturnToGame, SessionService sessions,
                             InventoryRenderer renderer) {
        super(navigator, onReturnToGame);
        this.sessions = sessions;
        this.renderer = renderer;
    }

    @Override
    protected void onOpen() {
        state = new InventoryState(sessions.session().player());
        state.open();
    }

    @Override
    protected boolean handleScreenKey(KeyCode code) {
        state.handle(toCommand(code));
        return !state.isOpen();
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
