package com.demo.mota;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.ui.screen.Screen;
import com.demo.mota.ui.screen.equipment.EquipmentRenderer;
import com.demo.mota.ui.screen.equipment.EquipmentScreen;
import com.demo.mota.ui.screen.game.GameScreen;
import com.demo.mota.ui.screen.gamemenu.GameMenuRenderer;
import com.demo.mota.ui.screen.gamemenu.GameMenuScreen;
import com.demo.mota.ui.screen.gameover.GameOverScreen;
import com.demo.mota.ui.screen.inventory.InventoryRenderer;
import com.demo.mota.ui.screen.inventory.InventoryScreen;
import com.demo.mota.ui.screen.loading.LoadingScreen;
import com.demo.mota.ui.screen.skill.SkillSetupRenderer;
import com.demo.mota.ui.screen.skill.SkillSetupScreen;
import com.demo.mota.ui.screen.title.TitleScreen;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import java.util.EnumMap;
import java.util.Map;

/**
 * FXML Controller，只做两件事：<b>按阶段路由</b>与<b>画布可见性</b>。
 *
 * <p>每个 {@link GamePhase} 对应一个 {@link Screen}，具体画什么、怎么响应按键都交给它。
 * 阶段迁移由 {@link GameFlow} 广播，本类订阅后切换当前界面、调 {@code onEnter} 并重绘——
 * 因此任何界面推进阶段后都不必回头通知 Controller，也不必自己处理「回到游戏要重画」。
 * 「新开一局」事件同样转发给全部界面（{@link Screen#onNewGame()}）。
 *
 * <p>画布两层（见 {@code mota-view.fxml}）：游戏画面 {@code gameBox} 之上是整屏的 {@code screenCanvas}。
 * {@code PLAYING} 只显示前者；其余界面画在 {@code screenCanvas} 上——铺满整屏的直接盖住，
 * {@link Screen#overlaysGame()} 为 true 的（游戏结束提示框）则先画一遍游戏画面再叠上去。
 */
public class MotaController {

    @FXML private HBox gameBox;
    @FXML private Canvas mapCanvas;
    @FXML private Canvas statusCanvas;
    @FXML private Canvas sideCanvas;
    @FXML private Canvas screenCanvas;

    private GameEngine engine;
    private GameFlow flow;

    private final Map<GamePhase, Screen> screens = new EnumMap<>(GamePhase.class);
    private Screen currentScreen;

    /** 对局界面单独留一个引用：叠在游戏画面上的界面要先画它 */
    private GameScreen gameScreen;
    /** 加载界面单独留一个引用：进度要从加载线程推进来 */
    private LoadingScreen loadingScreen;

    @FXML
    public void initialize() {
        engine = GameEngine.getGameEngine();
        flow = engine.getGameFlow();
        ResourceManager resources = ResourceManager.getInstance();

        loadingScreen = new LoadingScreen(screenCanvas);
        gameScreen = new GameScreen(engine, resources, flow, statusCanvas, mapCanvas, sideCanvas);

        screens.put(GamePhase.LOADING, loadingScreen);
        screens.put(GamePhase.TITLE, new TitleScreen(screenCanvas, engine));
        screens.put(GamePhase.PLAYING, gameScreen);
        screens.put(GamePhase.GAME_MENU,
                new GameMenuScreen(engine, flow, screenCanvas, new GameMenuRenderer(resources)));
        screens.put(GamePhase.INVENTORY,
                new InventoryScreen(engine, flow, screenCanvas, new InventoryRenderer(resources)));
        screens.put(GamePhase.EQUIPMENT,
                new EquipmentScreen(engine, flow, screenCanvas, new EquipmentRenderer(resources)));
        screens.put(GamePhase.SKILL_SETUP,
                new SkillSetupScreen(engine, flow, screenCanvas, new SkillSetupRenderer(resources)));
        screens.put(GamePhase.GAME_OVER, new GameOverScreen(screenCanvas, flow));

        flow.addNewGameListener(() -> screens.values().forEach(Screen::onNewGame));
        flow.addListener(this::applyPhase);
        applyPhase(flow.getPhase());
    }

    /** 供 {@code MotaApplication} 的加载任务推送进度 */
    public LoadingScreen getLoadingScreen() {
        return loadingScreen;
    }

    // ==================== 阶段路由 ====================

    private void applyPhase(GamePhase phase) {
        currentScreen = screens.get(phase);
        boolean playing = phase == GamePhase.PLAYING;
        boolean showGame = playing || currentScreen.overlaysGame();

        gameBox.setVisible(showGame);
        screenCanvas.setVisible(!playing);

        currentScreen.onEnter();
        if (showGame && !playing) {
            // 叠在游戏画面上的界面：先把游戏画面画好，让它从下面透出来
            gameScreen.render();
        }
        currentScreen.render();
    }

    // ==================== 输入 ====================

    public void handleKeyPress(KeyEvent event) {
        if (currentScreen != null) {
            currentScreen.handleKey(event.getCode());
        }
    }
}
