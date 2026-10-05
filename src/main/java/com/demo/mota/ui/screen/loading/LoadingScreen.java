package com.demo.mota.ui.screen.loading;

import com.demo.mota.ui.screen.Screen;
import com.demo.mota.ui.TextPainter;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 资源加载界面：进度条 + 当前任务名。
 *
 * <p>进度由后台跑 {@code GameBootstrap} 的线程经 {@code Platform.runLater} 推过来，
 * 本类只负责存下最近一次的值并重绘，不认识任何加载逻辑。
 */
public class LoadingScreen implements Screen {

    private static final Color BACKGROUND = Color.web("#12131a");
    private static final Color BAR_BACKGROUND = Color.web("#2a2d3a");
    private static final Color BAR_FILL = Color.web("#f0c96b");

    private final Canvas canvas;
    private final TextPainter painter = new TextPainter();

    private double progress;
    private String taskName = "准备中";

    public LoadingScreen(Canvas canvas) {
        this.canvas = canvas;
    }

    /** 由加载线程侧推送进度，调用方负责切回 JavaFX 线程 */
    public void update(double progress, String taskName) {
        this.progress = Math.max(0, Math.min(1, progress));
        if (taskName != null && !taskName.isBlank()) {
            this.taskName = taskName;
        }
    }

    /** 加载失败时把错误摆在界面上，而不是只留一条控制台日志 */
    public void showError(String message) {
        this.taskName = "加载失败：" + message;
    }

    @Override
    public void handleKey(KeyCode code) {
        // 加载期间吞掉所有按键，避免抢先进入尚未就绪的对局
    }

    @Override
    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, w, h);

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 56));
        painter.drawCenteredShadowText(gc, "魔  塔", w / 2, h * 0.38, Color.web("#f5f5f5"));

        double barWidth = w * 0.5;
        double barHeight = 18;
        double barX = (w - barWidth) / 2;
        double barY = h * 0.55;

        gc.setFill(BAR_BACKGROUND);
        gc.fillRoundRect(barX, barY, barWidth, barHeight, barHeight, barHeight);
        gc.setFill(BAR_FILL);
        gc.fillRoundRect(barX, barY, barWidth * progress, barHeight, barHeight, barHeight);

        gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 18));
        painter.drawCenteredShadowText(gc, taskName + "  " + Math.round(progress * 100) + "%",
                w / 2, barY + barHeight + 34, Color.web("#c9cdd8"));
    }
}
