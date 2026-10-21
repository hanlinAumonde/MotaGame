package com.demo.mota;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.LoadingScene;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.input.Input;
import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.engine.boot.GameBootstrap;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.ui.screen.game.map.EntityMapView;
import com.demo.mota.ui.screen.game.map.MapCellPainter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

/**
 * 应用入口：由 FXGL 托管窗口、场景与主循环。
 *
 * <p>FXGL 自带的主菜单 / 游戏菜单 / 开场动画全部关掉，生命周期仍由自己的 {@code GameFlow} 掌管；
 * 整套界面（{@link MotaController} 搭的画布）作为一个 UI 节点挂进 FXGL 的游戏场景，按键经 FXGL 的 {@link Input} 转发；
 * 中间的地图则是世界层里的实体（{@link EntityMapView}），从界面层留出的透明占位处透出来。
 * FXGL 自己的加载阶段（{@code initGame}）这里不用——资源加载仍走 {@code GameBootstrap}，进度画在我们的加载界面上。
 */
public class MotaApplication extends GameApplication {

    private MotaController controller;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("魔塔");
        settings.setVersion("");
        settings.setWidth(MotaController.WIDTH);
        settings.setHeight(MotaController.HEIGHT);
        settings.setManualResizeEnabled(false);
        settings.setIntroEnabled(false);
        settings.setMainMenuEnabled(false);
        // 关掉后 FXGL 不再占用 Esc（menuKey），存读档等界面的 Esc 返回照常可用
        settings.setGameMenuEnabled(false);
        settings.setDeveloperMenuEnabled(false);
        settings.setProfilingEnabled(false);
        settings.setCloseConfirmation(false);
        settings.setUserProfileEnabled(false);
        // 不让 FXGL 在运行目录写 system / logs 等文件；存档走自己的 SaveRepository，不受影响
        settings.setFileSystemWriteAllowed(false);
        settings.setApplicationMode(ApplicationMode.RELEASE);
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

        MapCellPainter cellPainter = new MapCellPainter(ResourceManager.getInstance(),
                (double) MotaController.MAP_SIZE / MAP_SIDE_LENGTH);
        EntityMapView mapView = new EntityMapView(GameEngine.getGameEngine(), cellPainter,
                MotaController.MAP_X, MotaController.MAP_Y);

        controller = new MotaController(mapView);
        FXGL.getGameScene().addUINode(controller.getRoot());
        startLoading(controller);
    }

    /**
     * 在后台线程跑完 {@code GameBootstrap}，加载界面同步显示进度，完成后迁到标题界面。
     *
     * <p>放后台是为了让进度条真的能动——同步加载会把 FX 线程占满，加载界面根本画不出来。
     * 进度回调发生在后台线程，因此一律经 {@link Platform#runLater} 切回 FX 线程再碰画布。
     */
    private void startLoading(MotaController controller) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                new GameBootstrap().run((progress, taskName) -> Platform.runLater(() -> {
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
                error.printStackTrace();
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
