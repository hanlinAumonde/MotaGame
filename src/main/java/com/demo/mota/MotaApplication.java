package com.demo.mota;

import com.demo.mota.engine.resource.ResourceManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MotaApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // FXML 也统一走 ResourceManager，保证外部资源目录同样可以覆盖界面布局
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
    }

    public static void main(String[] args) {
        launch();
    }
}
