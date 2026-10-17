package com.demo.mota.ui.screen.gamemenu;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.InGameScreen;
import com.demo.mota.ui.screen.saveload.SaveLoadMode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

import java.util.function.Consumer;

/**
 * 游戏菜单（{@link GamePhase#GAME_MENU}，对局中按 X）：{@link GameMenuState} + {@link GameMenuRenderer}。
 *
 * <p>Esc 退到底即回到游戏；选中某个游戏选项则迁往它对应的阶段（物品栏 / 装备 / 技能设置）；
 * 选中「存档」/「读档」则交代用途后迁往存读档界面（那里 Esc 回到本菜单）。
 */
public class GameMenuScreen extends InGameScreen {

    private final GameEngine engine;
    private final GameMenuRenderer renderer;
    /** 进入存读档界面前交代用途（即 {@code SaveLoadScreen.prepare}） */
    private final Consumer<SaveLoadMode> saveLoad;
    private GameMenuState state;

    public GameMenuScreen(GameEngine engine, GameFlow flow, Canvas canvas, GameMenuRenderer renderer,
                          Consumer<SaveLoadMode> saveLoad) {
        super(flow, canvas);
        this.engine = engine;
        this.renderer = renderer;
        this.saveLoad = saveLoad;
    }

    @Override
    public void onNewGame() {
        this.state = new GameMenuState(engine.getMapManager(), engine.getPlayerStateManager());
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
        SaveLoadMode saveLoadRequest = state.takeSaveLoadRequest();
        if (saveLoadRequest != null) {
            saveLoad.accept(saveLoadRequest);
            return GamePhase.SAVE_LOAD;
        }
        GamePhase handoff = state.takeHandoff();
        return handoff != null ? handoff : GamePhase.PLAYING;
    }

    @Override
    protected void render(GraphicsContext gc, double width, double height) {
        renderer.render(gc, state, engine.getPlayerStateManager(), width, height);
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
