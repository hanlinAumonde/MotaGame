package com.demo.mota;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.boot.GameBootstrap;
import com.demo.mota.engine.resource.ResourceManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MotaApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // FXML 也统一走 ResourceManager，保证外部资源目录同样可以覆盖界面布局。
        // 此时 ResourceManager 只搭好 provider 链，精灵图与各配置表留给下面的加载任务。
        FXMLLoader fxmlLoader = new FXMLLoader(
                ResourceManager.getInstance().getResourceUrl("/com/demo/mota/mota-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1040, 840);

        // 获取 controller 并绑定键盘事件
        MotaController controller = fxmlLoader.getController();
        scene.setOnKeyPressed(controller::handleKeyPress);

        stage.setTitle("魔塔");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        // 让画布获得焦点以接收键盘事件
        scene.getRoot().requestFocus();

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

        task.setOnSucceeded(e -> GameEngine.getGameEngine().getGameFlow().toTitle());
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

    public static void main(String[] args) {
        launch();
    }
}
