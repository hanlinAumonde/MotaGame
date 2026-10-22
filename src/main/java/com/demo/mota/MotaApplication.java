package com.demo.mota;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.LoadingScene;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.logging.Logger;
import com.demo.mota.app.services.AssetService;
import com.demo.mota.app.services.DevToolsService;
import com.demo.mota.content.ContentLoader;
import com.demo.mota.content.ContentSource;
import com.demo.mota.content.LoadingRunner;
import com.demo.mota.content.LoadingTask;
import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.ui.screen.game.map.EntityMapView;
import com.demo.mota.ui.screen.game.map.MapCellPainter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用入口：由 FXGL 托管窗口、场景与主循环。
 *
 * <p>FXGL 自带的主菜单 / 游戏菜单 / 开场动画全部关掉，生命周期仍由自己的 {@code GameFlow} 掌管；
 * 整套界面（{@link MotaController} 搭的画布）作为一个 UI 节点挂进 FXGL 的游戏场景，按键经 FXGL 的 {@link Input} 转发；
 * 中间的地图则是世界层里的实体（{@link EntityMapView}），从界面层留出的透明占位处透出来。
 * FXGL 自己的加载阶段（{@code initGame}）这里不用——塔的内容由 {@link ContentLoader} 在后台线程读取，进度画在我们的加载界面上；
 * 精灵目录则由 {@link AssetService} 在 FXGL 的启动期（服务初始化）载入。
 *
 * <p>字体、样式表、窗口图标都按 FXGL 的约定放在 {@code assets/} 下、经 {@link GameSettings} 注册。
 * {@code -Dmota.dev=true} 开启开发模式：FXGL 的开发者面板（F1）/ 控制台（F2）/ 日志；
 * {@code -Dmota.profile=true} 另开性能监视。
 */
public class MotaApplication extends GameApplication {

    private static final Logger log = Logger.get(MotaApplication.class);

    private MotaController controller;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("魔塔");
        settings.setVersion("");
        settings.setWidth(MotaController.WIDTH);
        settings.setHeight(MotaController.HEIGHT);
        // 允许拖动缩放 / 最大化：FXGL 按比例缩放整个场景并留黑边；不提供全屏
        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);
        settings.setFullScreenAllowed(false);
        settings.setIntroEnabled(false);
        settings.setMainMenuEnabled(false);
        // 关掉后 FXGL 不再占用 Esc（menuKey），存读档等界面的 Esc 返回照常可用
        settings.setGameMenuEnabled(false);
        settings.setCloseConfirmation(false);
        settings.setUserProfileEnabled(false);

        // 打包字体（assets/ui/fonts/）：界面经 ui.Fonts 取用，不再依赖系统里的黑体 / Consolas
        settings.setFontUI("NotoSansSC-Regular.ttf");
        settings.setFontGame("NotoSansSC-Bold.ttf");
        settings.setFontText("NotoSansSC-Regular.ttf");
        settings.setFontMono("Inconsolata-ExtraBold.ttf");
        // FXGL 自带的样式（开发者面板、对话框）+ 本作的样式（assets/ui/css/）
        settings.setCSSList(List.of("fxgl_dark.css", "mota.css"));
        settings.setAppIcon("ui/icon.png");

        settings.addEngineService(AssetService.class);

        boolean dev = DevToolsService.isDevMode();
        settings.setApplicationMode(dev ? ApplicationMode.DEVELOPER : ApplicationMode.RELEASE);
        settings.setDeveloperMenuEnabled(dev);
        settings.setProfilingEnabled(DevToolsService.isProfiling());
        // 发行版不让 FXGL 在运行目录写 system / logs；存档走自己的 SaveRepository，不受影响
        settings.setFileSystemWriteAllowed(dev);
        if (dev) {
            settings.addEngineService(DevToolsService.class);
        }
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public LoadingScene newLoadingScene() {
                // initGame 是空的，FXGL 的加载场景只会一闪而过；换成空白场景，免得闪出它默认的进度条
                return new LoadingScene() { };
            }
        });
    }

    @Override
    protected void initInput() {
        // 不注册 UserAction，按键原样交给当前界面（按住 / 松开的组合键判定由 ChordKey 自己做）
        Input input = FXGL.getInput();
        // initInput 早于 initUI，界面搭好之前的按键直接丢掉
        input.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (controller != null) controller.handleKeyPress(e);
        });
        input.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            if (controller != null) controller.handleKeyRelease(e);
        });
    }

    @Override
    protected void initUI() {
        // 界面层不设底色（地图要透上来），原先根节点的底色挪到场景背景上
        FXGL.getGameScene().setBackgroundColor(Color.web("#2a2a2a"));

        AssetService assets = FXGL.getService(AssetService.class);
        MapCellPainter cellPainter = new MapCellPainter(assets, (double) MotaController.MAP_SIZE / GameMap.SIDE);
        EntityMapView mapView = new EntityMapView(GameEngine.getGameEngine(), cellPainter,
                MotaController.MAP_X, MotaController.MAP_Y);

        controller = new MotaController(mapView, assets);
        FXGL.getGameScene().addUINode(controller.getRoot());
        // 失焦后收不到松键事件：通知界面把「按住」的状态清掉（否则按住方向键切出窗口，人会一直走）
        FXGL.getPrimaryStage().focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                controller.handleFocusLost();
            }
        });
        startLoading(controller, assets);
    }

    /**
     * 在后台线程跑完加载任务表，加载界面同步显示进度，完成后迁到标题界面。
     * 任务表 = 读塔的内容（{@link ContentLoader}）+ 装进引擎并扫描存档 + 预载内容表引用的图片。
     *
     * <p>放后台是为了让进度条真的能动——同步加载会把 FX 线程占满，加载界面根本画不出来。
     * 进度回调发生在后台线程，因此一律经 {@link Platform#runLater} 切回 FX 线程再碰画布。
     */
    private void startLoading(MotaController controller, AssetService assets) {
        GameEngine engine = GameEngine.getGameEngine();
        ContentLoader loader = new ContentLoader(ContentSource.getInstance());
        List<LoadingTask> tasks = new ArrayList<>(loader.tasks());
        tasks.add(new LoadingTask("扫描存档", 3, () -> {
            engine.install(loader.result());
            engine.getSaveRepository().refresh();
        }));
        tasks.add(new LoadingTask("载入图像资源", 20, () -> assets.preloadContentSprites(engine.getContent())));

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                LoadingRunner.run(tasks, (progress, taskName) -> Platform.runLater(() -> {
                    controller.getLoadingScreen().update(progress, taskName);
                    controller.getLoadingScreen().render();
                }));
                return null;
            }
        };

        task.setOnSucceeded(e -> GameEngine.getGameEngine().getGameFlow().to(GamePhase.TITLE));
        task.setOnFailed(e -> {
            Throwable error = task.getException();
            // 资源 / 配置缺失属于打包错误，把原因摆到界面上，同时保留完整堆栈便于排查
            if (error != null) {
                log.fatal("加载失败", error);
            }
            controller.getLoadingScreen().showError(error == null ? "未知错误" : String.valueOf(error.getMessage()));
            controller.getLoadingScreen().render();
        });

        Thread thread = new Thread(task, "mota-bootstrap");
        thread.setDaemon(true);
        thread.start();
    }

    static void main(String[] args) {
        launch(MotaApplication.class, args);
    }
}
