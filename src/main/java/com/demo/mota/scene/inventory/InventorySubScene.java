package com.demo.mota.scene.inventory;

import com.demo.mota.app.nav.Navigator;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.scene.InGameSubScene;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.input.KeyCode;

/**
 * 物品栏：{@link InventoryState} + {@link InventoryView}。
 *
 * <p>目前没有专属快捷键，只能从游戏菜单「游戏选项 → 物品栏」进入；
 * 要加快捷键，在 {@code GameplayInput} 里加一个动作、经 {@code Navigator} 叠上本场景即可。
 */
public class InventorySubScene extends InGameSubScene {

    private final SessionService sessions;
    private final InventoryView view;
    private InventoryState state;

    public InventorySubScene(Navigator navigator, Runnable onReturnToGame, SessionService sessions,
                             InventoryView view) {
        super(navigator, onReturnToGame);
        this.sessions = sessions;
        this.view = view;
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
    protected void draw(Sketch sketch) {
        view.draw(sketch, state);
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
