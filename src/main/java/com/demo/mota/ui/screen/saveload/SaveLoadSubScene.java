package com.demo.mota.ui.screen.saveload;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.logging.Logger;
import com.demo.mota.app.nav.Navigator;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.engine.save.SaveException;
import com.demo.mota.engine.save.SaveRepository;
import com.demo.mota.engine.session.GameSession;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.CanvasSubScene;
import com.demo.mota.ui.screen.InGameSubScene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

import java.io.IOException;

/**
 * 存读档界面：{@link SaveLoadState} + {@link SaveLoadRenderer}。
 *
 * <p>入口有三处：标题「继续游戏」、结束横幅「读取存档」、游戏菜单「存档 / 读档」，都是叠在来源之上，
 * 所以 Esc 弹出即回到来源；从游戏菜单进来时 X 与其他对局内界面一致直接回到对局，从别处进来时 X 与 Esc 相同。
 * 不继承 {@code InGameSubScene}：从标题进来时并没有一局可回。
 *
 * <p>读档统一走 FXGL 的 {@code startNewGame()}：先在这里读出快照、造好一局（失败就留在本界面提示），
 * 交给会话服务后再开局，世界与界面都按新的一局重建。存档成功留在本界面，刷新档位并给出提示。
 */
public class SaveLoadSubScene extends CanvasSubScene {

    private static final Logger log = Logger.get(SaveLoadSubScene.class);

    private final Navigator navigator;
    private final SessionService sessions;
    private final SaveLoadRenderer renderer;

    private final SaveLoadState state = new SaveLoadState();
    private SaveLoadMode mode = SaveLoadMode.LOAD;
    /** 是否从游戏菜单进来（决定 X 是直接回到对局，还是与 Esc 一样回到来源） */
    private boolean fromGameMenu;

    public SaveLoadSubScene(Navigator navigator, SessionService sessions, SaveLoadRenderer renderer) {
        this.navigator = navigator;
        this.sessions = sessions;
        this.renderer = renderer;
    }

    /** 以指定用途叠到当前场景之上 */
    public void open(SaveLoadMode mode, boolean fromGameMenu) {
        this.mode = mode;
        this.fromGameMenu = fromGameMenu;
        navigator.push(this);
    }

    private SaveRepository repository() {
        return sessions.saves();
    }

    @Override
    protected void onOpen() {
        state.open(mode, repository().getSlots());
    }

    @Override
    protected void onKey(KeyCode code) {
        if (code == KeyCode.ESCAPE) {
            navigator.pop();
            return;
        }
        if (code == InGameSubScene.CLOSE_KEY) {
            if (fromGameMenu) {
                navigator.backToGame();
            } else {
                navigator.pop();
            }
            return;
        }
        String slotId = state.handle(toCommand(code));
        if (slotId != null) {
            if (mode == SaveLoadMode.SAVE) {
                save(slotId);
            } else if (load(slotId)) {
                return;
            }
        }
        render();
    }

    /** 存完留在本界面 */
    private void save(String slotId) {
        String name = SaveRepository.displayName(slotId);
        try {
            sessions.saveGame(slotId);
            state.updateSlots(repository().getSlots());
            state.setNotice("已保存到 " + name);
        } catch (IOException e) {
            state.setNotice("保存失败：" + e.getMessage());
        }
    }

    /** @return 读档成功（已开始进入对局）时为 true */
    private boolean load(String slotId) {
        String name = SaveRepository.displayName(slotId);
        GameSession restored;
        try {
            restored = sessions.prepareLoad(slotId);
        } catch (SaveException e) {
            log.warning("读取 " + name + " 失败：" + e.getMessage());
            state.updateSlots(repository().refresh());
            state.setNotice(name + " 读取失败：" + e.getMessage());
            return false;
        }
        sessions.request(restored, "已读取" + name);
        FXGL.getGameController().startNewGame();
        return true;
    }

    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP, W -> MenuCommand.UP;
            case DOWN, S -> MenuCommand.DOWN;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            default -> null;
        };
    }

    @Override
    protected void render(GraphicsContext gc, double width, double height) {
        renderer.render(gc, state, width, height);
    }
}
