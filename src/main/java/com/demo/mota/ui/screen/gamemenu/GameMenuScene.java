package com.demo.mota.ui.screen.gamemenu;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.scene.Scene;
import com.demo.mota.engine.session.GameSession;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.CanvasView;
import com.demo.mota.ui.screen.gamemenu.option.GameOption;
import com.demo.mota.ui.screen.saveload.SaveLoadMode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * 游戏菜单（FXGL 的游戏菜单，{@link MenuType#GAME_MENU}）：{@link GameMenuState} + {@link GameMenuRenderer}。
 *
 * <p>开关由 FXGL 的菜单键（设为 X）处理：对局中按 X 叠上、菜单里按 X 弹回（源码：{@code initAppScenes} 的 menuKeyHandler），
 * 本类不碰 X。Esc 退到底即回到对局；选中某个游戏选项则换成对应界面（物品栏 / 装备 / 技能设置 / 套装）；
 * 选中「存档」/「读档」则叠上存读档界面（那里 Esc 回到本菜单）。
 *
 * <p>与标题菜单一样在内容载入之前就被构造，状态在每次叠上来时（{@link #onCreate()}）按当前这一局重建。
 */
public class GameMenuScene extends FXGLMenu {

    /** 游戏菜单要用到的东西，由组装处在服务就绪后提供 */
    public interface Host {
        GameSession session();

        GameMenuRenderer renderer();

        /** 换成某个游戏选项对应的界面（弹掉本菜单、叠上那个界面） */
        void openOption(GameOption option);

        /** 叠上存读档界面 */
        void openSaveLoad(SaveLoadMode mode);

        /** 本菜单弹回对局：对局界面重算伤害、清掉按住状态并重画 */
        void returnToGame();
    }

    private final Host host;
    private final CanvasView view = new CanvasView(this::render);
    private GameMenuState state;

    public GameMenuScene(Host host) {
        super(MenuType.GAME_MENU);
        this.host = host;
        getContentRoot().getChildren().add(view.node());
        getInput().addEventHandler(KeyEvent.KEY_PRESSED, e -> handleKey(e.getCode()));
    }

    @Override
    public void onCreate() {
        GameSession session = host.session();
        state = new GameMenuState(session.maps(), session.player());
        state.open();
        view.render();
    }

    /** 从存读档界面 Esc 回来：菜单回到首页 */
    @Override
    public void onEnteredFrom(Scene previous) {
        if (previous.isSubState()) {
            state.open();
            view.render();
        }
    }

    @Override
    public void onExitingTo(Scene next) {
        if (next == FXGL.getGameScene()) {
            host.returnToGame();
        }
    }

    private void handleKey(KeyCode code) {
        state.handle(toCommand(code));
        if (state.isOpen()) {
            view.render();
            return;
        }
        SaveLoadMode saveLoadRequest = state.takeSaveLoadRequest();
        if (saveLoadRequest != null) {
            host.openSaveLoad(saveLoadRequest);
            return;
        }
        GameOption handoff = state.takeHandoff();
        if (handoff != null) {
            host.openOption(handoff);
        } else {
            FXGL.getSceneService().popSubScene();
        }
    }

    private void render(GraphicsContext gc, double width, double height) {
        host.renderer().render(gc, state, host.session().player(), width, height);
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
