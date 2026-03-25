package com.demo.mota;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MotaApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MotaApplication.class.getResource("mota-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1080, 960);

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
