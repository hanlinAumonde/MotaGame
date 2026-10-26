package com.demo.mota.scene.startup;

import com.almasb.fxgl.app.scene.StartupScene;
import com.demo.mota.app.services.StartupProgress;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;

/**
 * 启动场景（FXGL 的 {@link StartupScene}）：服务初始化期间显示，进度条 + 当前任务名。
 *
 * <p>进度来自 {@link StartupProgress}（内容服务、资产服务的任务表），本类只存下最近一次的值并重绘。
 * 加载失败时把原因写在任务名处，FXGL 随后弹出致命错误框。
 *
 * <p>FXGL 在<b>任何服务构造之前</b>就创建并显示本场景（源码：{@code FXGLApplication.start}），
 * 取不到 FXGL 注册的字体，所以这里直接从打包的字体文件载入用到的两种字号。
 */
public class MotaStartupScene extends StartupScene {

    private static final Color BACKGROUND = Color.web("#12131a");
    private static final Color BAR_BACKGROUND = Color.web("#2a2d3a");
    private static final Color BAR_FILL = Color.web("#f0c96b");

    private static final String FONTS = "/assets/ui/fonts/";

    private final Sketch sketch = new Sketch();
    private final double width;
    private final double height;
    private final Font titleFont = loadFont("NotoSansSC-Bold.ttf", 56);
    private final Font taskFont = loadFont("NotoSansSC-Regular.ttf", 18);

    private double progress;
    private String taskName = "准备中";

    public MotaStartupScene(int width, int height) {
        super(width, height);
        this.width = width;
        this.height = height;
        getContentRoot().getChildren().add(sketch.node());
        StartupProgress.get().setListener(this::update);
        render();
    }

    private static Font loadFont(String file, double size) {
        try (InputStream in = MotaStartupScene.class.getResourceAsStream(FONTS + file)) {
            Font font = in == null ? null : Font.loadFont(in, size);
            return font != null ? font : Font.font(size);
        } catch (IOException e) {
            return Font.font(size);
        }
    }

    /** 由 {@link StartupProgress} 在 FX 线程上推送 */
    private void update(double progress, String taskName) {
        this.progress = Math.max(0, Math.min(1, progress));
        if (taskName != null && !taskName.isBlank()) {
            this.taskName = taskName;
        }
        render();
    }

    private void render() {
        sketch.clear();
        double w = width;
        double h = height;
        sketch.fill(0, 0, w, h, BACKGROUND);
        sketch.centered("魔  塔", titleFont, Color.web("#f5f5f5"), w / 2, h * 0.38);

        double barWidth = w * 0.5;
        double barHeight = 18;
        double barX = (w - barWidth) / 2;
        double barY = h * 0.55;
        roundRect(barX, barY, barWidth, barHeight, BAR_BACKGROUND);
        roundRect(barX, barY, barWidth * progress, barHeight, BAR_FILL);

        sketch.centered(taskName + "  " + Math.round(progress * 100) + "%", taskFont, Color.web("#c9cdd8"),
                w / 2, barY + barHeight + 34);
    }

    private void roundRect(double x, double y, double w, double h, Color color) {
        Rectangle rect = sketch.fill(x, y, w, h, color);
        rect.setArcWidth(h);
        rect.setArcHeight(h);
    }
}
