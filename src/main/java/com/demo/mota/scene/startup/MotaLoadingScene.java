package com.demo.mota.scene.startup;

import com.almasb.fxgl.app.scene.LoadingScene;
import com.demo.mota.ui.MotaLayout;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * 加载场景（FXGL 的 {@link LoadingScene}）：每次 {@code startNewGame()}（新开一局、读档）装配这一局时显示。
 *
 * <p>重活（读内容、读档、按快照造好一局）都在之前做完了，这里只剩生成实体、挂界面，一闪而过，
 * 所以不画进度条，只铺一层与标题相同的底色，免得闪出 FXGL 默认的加载界面。
 */
public class MotaLoadingScene extends LoadingScene {

    public MotaLoadingScene() {
        getContentRoot().getChildren().add(new Rectangle(MotaLayout.WIDTH, MotaLayout.HEIGHT, Color.web("#12131a")));
    }
}
