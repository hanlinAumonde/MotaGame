package com.demo.mota;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.ui.screen.Screen;
import com.demo.mota.ui.screen.equipment.EquipmentRenderer;
import com.demo.mota.ui.screen.equipment.EquipmentScreen;
import com.demo.mota.ui.screen.equipset.EquipmentSetRenderer;
import com.demo.mota.ui.screen.equipset.EquipmentSetScreen;
import com.demo.mota.ui.screen.game.GameScreen;
import com.demo.mota.ui.screen.game.map.MapView;
import com.demo.mota.ui.screen.gamemenu.GameMenuRenderer;
import com.demo.mota.ui.screen.gamemenu.GameMenuScreen;
import com.demo.mota.ui.screen.gameover.GameOverScreen;
import com.demo.mota.ui.screen.inventory.InventoryRenderer;
import com.demo.mota.ui.screen.inventory.InventoryScreen;
import com.demo.mota.ui.screen.loading.LoadingScreen;
import com.demo.mota.ui.screen.skill.SkillSetupRenderer;
import com.demo.mota.ui.screen.skill.SkillSetupScreen;
import com.demo.mota.ui.screen.title.TitleScreen;
import com.demo.mota.ui.screen.saveload.SaveLoadRenderer;
import com.demo.mota.ui.screen.saveload.SaveLoadScreen;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.EnumMap;
import java.util.Map;

/**
 * 界面总控，只做两件事：<b>按阶段路由</b>与<b>画布可见性</b>。
 *
 * <p>每个 {@link GamePhase} 对应一个 {@link Screen}，具体画什么、怎么响应按键都交给它。
 * 阶段迁移由 {@link GameFlow} 广播，本类订阅后切换当前界面、调 {@code onEnter} 并重绘——
 * 因此任何界面推进阶段后都不必回头通知 Controller，也不必自己处理「回到游戏要重画」。
 * 「新开一局」事件同样转发给全部界面（{@link Screen#onNewGame()}）。
 *
 * <p>画布两层（{@link #buildLayout()}）：游戏画面 {@code gameBox} 之上是整屏的 {@code screenCanvas}。
 * {@code PLAYING} 只显示前者；其余界面画在 {@code screenCanvas} 上——铺满整屏的直接盖住，
 * {@link Screen#overlaysGame()} 为 true 的（游戏结束提示框）则先画一遍游戏画面再叠上去。
 *
 * <p>布局由本类用代码搭好、经 {@link #getRoot()} 交给宿主（{@code MotaApplication} 把它挂进 FXGL 的游戏场景），
 * 本类不认识 FXGL；按键同样由宿主转发进来。
 *
 * <p>中间的地图不是画布：{@code gameBox} 在那里只留一块透明占位，地图由宿主注入的 {@link MapView}
 * 显示在界面层之下（FXGL 的世界层），从占位处透出来；它的显隐随 {@code gameBox} 一起切换。
 */
public class MotaController {

    /** 窗口尺寸：左状态栏 208 + 中地图 832 + 右技能装备栏 256；高 840（地图 832 + 底部留白） */
    public static final int WIDTH = 1296;
    public static final int HEIGHT = 840;

    private static final int STATUS_WIDTH = 208;
    private static final int SIDE_WIDTH = 256;

    /** 地图区域：左上角在窗口中的位置与边长，宿主据此摆放 {@link MapView} */
    public static final int MAP_X = STATUS_WIDTH;
    public static final int MAP_Y = 0;
    public static final int MAP_SIZE = 832;

    private final HBox gameBox;
    private final MapView mapView;
    private final Canvas statusCanvas = new Canvas(STATUS_WIDTH, MAP_SIZE);
    private final Canvas sideCanvas = new Canvas(SIDE_WIDTH, MAP_SIZE);
    private final Canvas screenCanvas = new Canvas(WIDTH, HEIGHT);
    private final StackPane root;

    private GameEngine engine;
    private GameFlow flow;

    private final Map<GamePhase, Screen> screens = new EnumMap<>(GamePhase.class);
    private Screen currentScreen;

    /** 对局界面单独留一个引用：叠在游戏画面上的界面要先画它 */
    private GameScreen gameScreen;
    /** 加载界面单独留一个引用：进度要从加载线程推进来 */
    private LoadingScreen loadingScreen;

    public MotaController(MapView mapView) {
        this.mapView = mapView;
        gameBox = new HBox(0, statusCanvas, mapSlot(), sideCanvas);
        root = buildLayout();

        engine = GameEngine.getGameEngine();
        flow = engine.getGameFlow();
        ResourceManager resources = ResourceManager.getInstance();

        loadingScreen = new LoadingScreen(screenCanvas);
        gameScreen = new GameScreen(engine, resources, flow, statusCanvas, mapView, sideCanvas);
        SaveLoadScreen saveLoadScreen = new SaveLoadScreen(engine, flow, screenCanvas, new SaveLoadRenderer(),
                gameScreen::showMessageNow);

        screens.put(GamePhase.LOADING, loadingScreen);
        screens.put(GamePhase.TITLE, new TitleScreen(screenCanvas, engine, saveLoadScreen::prepare));
        screens.put(GamePhase.PLAYING, gameScreen);
        screens.put(GamePhase.GAME_MENU,
                new GameMenuScreen(engine, flow, screenCanvas, new GameMenuRenderer(resources),
                        saveLoadScreen::prepare));
        screens.put(GamePhase.INVENTORY,
                new InventoryScreen(engine, flow, screenCanvas, new InventoryRenderer(resources)));
        screens.put(GamePhase.EQUIPMENT,
                new EquipmentScreen(engine, flow, screenCanvas, new EquipmentRenderer(resources)));
        screens.put(GamePhase.SKILL_SETUP,
                new SkillSetupScreen(engine, flow, screenCanvas, new SkillSetupRenderer(resources)));
        screens.put(GamePhase.EQUIPMENT_SETS, new EquipmentSetScreen(engine, flow, screenCanvas,
                new EquipmentSetRenderer(resources), gameScreen::showMessage));
        screens.put(GamePhase.GAME_OVER, new GameOverScreen(screenCanvas, flow,
                engine::canUndo, gameScreen::undoLastAction,
                () -> engine.getSaveRepository().hasAnySave(), saveLoadScreen::prepare));
        screens.put(GamePhase.SAVE_LOAD, saveLoadScreen);

        flow.addNewGameListener(() -> screens.values().forEach(Screen::onNewGame));
        flow.addListener(this::applyPhase);
        applyPhase(flow.getPhase());
    }

    /**
     * 自下而上两层：游戏画面 → 其余全部界面（加载 / 标题 / 游戏菜单 / 物品栏 / 装备 / 技能设置 / 结束）。
     * 最外层界面多数铺满整屏；游戏结束只画底部提示框、其余部分透出游戏画面（{@link Screen#overlaysGame()}）
     */
    private StackPane buildLayout() {
        StackPane pane = new StackPane(gameBox, screenCanvas);
        pane.setAlignment(Pos.TOP_LEFT);
        pane.setPrefSize(WIDTH, HEIGHT);
        // 不设底色：地图要从下面的世界层透上来，底色改由宿主设在场景背景上
        return pane;
    }

    /** 地图位置的透明占位：只撑开尺寸，让两侧画布落在原位 */
    private static Region mapSlot() {
        Region slot = new Region();
        slot.setMinSize(MAP_SIZE, MAP_SIZE);
        slot.setPrefSize(MAP_SIZE, MAP_SIZE);
        slot.setMaxSize(MAP_SIZE, MAP_SIZE);
        return slot;
    }

    /** 整个界面的根节点，由宿主挂到场景上 */
    public Parent getRoot() {
        return root;
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
        mapView.setVisible(showGame);
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

    public void handleKeyRelease(KeyEvent event) {
        if (currentScreen != null) {
            currentScreen.handleKeyRelease(event.getCode());
        }
    }

    /** 窗口失焦时由宿主调用，转给当前界面（见 {@link Screen#onFocusLost()}） */
    public void handleFocusLost() {
        if (currentScreen != null) {
            currentScreen.onFocusLost();
        }
    }
}
