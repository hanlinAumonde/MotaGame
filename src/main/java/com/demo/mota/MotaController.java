package com.demo.mota;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.ui.screen.GameOverOverlay;
import com.demo.mota.ui.screen.GameScreen;
import com.demo.mota.ui.screen.LoadingScreen;
import com.demo.mota.ui.screen.Screen;
import com.demo.mota.ui.screen.TitleScreen;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import java.util.EnumMap;
import java.util.Map;

/**
 * FXML Controller，现在只做两件事：<b>按阶段路由</b>与<b>画布可见性</b>。
 *
 * <p>具体画什么、怎么响应按键，都交给 {@code ui.screen} 下各个 {@link Screen}。
 * 阶段迁移由 {@link GameFlow} 广播，本类订阅后切换当前界面并重绘——
 * 因此「开始游戏」「返回标题」这些动作只需在各自的 Screen 里推进阶段，不必回头通知 Controller。
 *
 * <p>画布分三层（见 {@code mota-view.fxml}）：游戏画面 {@code gameBox} → 游戏内菜单
 * {@code menuCanvas} → 最外层 {@code screenCanvas}。{@code GAME_OVER} 是唯一一个
 * 「两层同时可见」的阶段：游戏画面留着，底部叠一条提示框。
 */
public class MotaController {

    @FXML private HBox gameBox;
    @FXML private Canvas mapCanvas;
    @FXML private Canvas statusCanvas;
    @FXML private Canvas menuCanvas;
    @FXML private Canvas screenCanvas;

    private GameEngine engine;
    private GameFlow flow;

    private final Map<GamePhase, Screen> screens = new EnumMap<>(GamePhase.class);
    private Screen currentScreen;

    /** 加载界面单独留一个引用：进度要从加载线程推进来 */
    private LoadingScreen loadingScreen;

    @FXML
    public void initialize() {
        engine = GameEngine.getGameEngine();
        flow = engine.getGameFlow();

        loadingScreen = new LoadingScreen(screenCanvas);
        screens.put(GamePhase.LOADING, loadingScreen);
        screens.put(GamePhase.TITLE, new TitleScreen(screenCanvas, engine));
        screens.put(GamePhase.PLAYING, new GameScreen(engine, ResourceManager.getInstance(), flow,
                statusCanvas, mapCanvas, menuCanvas));
        screens.put(GamePhase.GAME_OVER, new GameOverOverlay(screenCanvas, flow));

        flow.addListener(this::onPhaseChanged);
        applyPhase(flow.getPhase());
    }

    /** 供 {@code MotaApplication} 的加载任务推送进度 */
    public LoadingScreen getLoadingScreen() {
        return loadingScreen;
    }

    // ==================== 阶段路由 ====================

    private void onPhaseChanged(GamePhase phase) {
        applyPhase(phase);
    }

    private void applyPhase(GamePhase phase) {
        boolean inGame = phase == GamePhase.PLAYING || phase == GamePhase.GAME_OVER;
        gameBox.setVisible(inGame);
        // 标题 / 加载期间不该残留上一局的菜单
        menuCanvas.setVisible(false);
        screenCanvas.setVisible(phase != GamePhase.PLAYING);

        currentScreen = screens.get(phase);
        if (currentScreen != null) {
            currentScreen.onEnter();
        }

        // 游戏结束时提示框叠在游戏画面上，两层都要画
        if (phase == GamePhase.GAME_OVER) {
            screens.get(GamePhase.PLAYING).render();
        }
        renderCurrent();
    }

    private void renderCurrent() {
        if (currentScreen != null) {
            currentScreen.render();
        }
    }

    // ==================== 输入 ====================

    public void handleKeyPress(KeyEvent event) {
        if (currentScreen != null) {
            currentScreen.handleKey(event.getCode());
        }
    }
}
