package com.demo.mota.ui.screen.saveload;

import com.almasb.fxgl.logging.Logger;
import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.engine.save.SaveException;
import com.demo.mota.engine.save.SaveRepository;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.InGameScreen;
import com.demo.mota.ui.screen.Screen;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * 存读档界面（{@link GamePhase#SAVE_LOAD}）：{@link SaveLoadState} + {@link SaveLoadRenderer}。
 *
 * <p>入口有三处：标题「继续游戏」、结束框「读取存档」、游戏菜单「存档 / 读档」。
 * 进入前由来源界面调 {@link #prepare} 交代用途，同时记下来源阶段——Esc 回到来源；
 * 从游戏菜单进来时 X 与其他对局内界面一致直接回到游戏。
 * 不继承 {@code InGameScreen}：从标题进来时并没有一局可回。
 *
 * <p>读档成功即进入对局（引擎经 {@code resumeSession} 迁到 {@code PLAYING}），提示经 {@code gameMessage} 交给对局界面；
 * 存档成功留在本界面，刷新档位并给出提示。
 */
public class SaveLoadScreen implements Screen {

    private static final Logger log = Logger.get(SaveLoadScreen.class);

    private final GameEngine engine;
    private final GameFlow flow;
    private final Canvas canvas;
    private final SaveLoadRenderer renderer;
    /** 读档后显示在对局状态栏的提示（并重绘对局界面） */
    private final Consumer<String> gameMessage;

    private final SaveLoadState state = new SaveLoadState();
    private SaveLoadMode mode = SaveLoadMode.LOAD;
    private GamePhase origin = GamePhase.TITLE;

    public SaveLoadScreen(GameEngine engine, GameFlow flow, Canvas canvas, SaveLoadRenderer renderer,
                          Consumer<String> gameMessage) {
        this.engine = engine;
        this.flow = flow;
        this.canvas = canvas;
        this.renderer = renderer;
        this.gameMessage = gameMessage;
    }

    /** 来源界面在迁往本阶段之前调用：交代用途，并把当前阶段记为返回目标 */
    public void prepare(SaveLoadMode mode) {
        this.mode = mode;
        this.origin = flow.getPhase();
    }

    private SaveRepository repository() {
        return engine.getSaveRepository();
    }

    @Override
    public void onEnter() {
        state.open(mode, repository().getSlots());
    }

    @Override
    public void handleKey(KeyCode code) {
        if (code == KeyCode.ESCAPE) {
            flow.to(origin);
            return;
        }
        if (code == InGameScreen.CLOSE_KEY) {
            flow.to(origin == GamePhase.GAME_MENU ? GamePhase.PLAYING : origin);
            return;
        }
        String slotId = state.handle(toCommand(code));
        if (slotId != null) {
            boolean left = mode == SaveLoadMode.SAVE ? save(slotId) : load(slotId);
            if (left) return;
        }
        render();
    }

    /** @return 恒为 false：存完留在本界面 */
    private boolean save(String slotId) {
        String name = SaveRepository.displayName(slotId);
        try {
            engine.saveGame(slotId);
            state.updateSlots(repository().getSlots());
            state.setNotice("已保存到 " + name);
        } catch (IOException e) {
            state.setNotice("保存失败：" + e.getMessage());
        }
        return false;
    }

    /** @return 读档成功（已进入对局）时为 true */
    private boolean load(String slotId) {
        String name = SaveRepository.displayName(slotId);
        try {
            engine.loadGame(slotId);
        } catch (SaveException e) {
            log.warning("读取 " + name + " 失败：" + e.getMessage());
            state.updateSlots(repository().refresh());
            state.setNotice(name + " 读取失败：" + e.getMessage());
            return false;
        }
        gameMessage.accept("已读取" + name);
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
    public void render() {
        renderer.render(canvas.getGraphicsContext2D(), state, canvas.getWidth(), canvas.getHeight());
    }
}
