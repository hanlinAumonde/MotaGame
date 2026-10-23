package com.demo.mota.ui.screen;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.ui.FontType;
import com.demo.mota.ui.MotaLayout;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * 场景内容的过渡做法（FXGL 迁移阶段 5）：一块铺满窗口的画布，交给原来的 {@code XxxRenderer} 画。
 * 阶段 7 各界面改写为节点树（{@code XxxView}）后删除。
 *
 * <p>FXGL 的打包字体在全部服务初始化之后才异步注册（源码：{@code FXGLApplication.postServicesInit}），
 * 标题菜单可能抢在那之前画第一帧。这里留一个绑定了字体的隐形 {@link Text} 作探针：
 * 字体注册完成、探针字体随之变化时，若画布正显示着就重画一次。
 */
public final class CanvasView {

    /** 画一帧：画布的绘图上下文与尺寸 */
    @FunctionalInterface
    public interface Painter {
        void paint(GraphicsContext gc, double width, double height);
    }

    private final Canvas canvas = new Canvas(MotaLayout.WIDTH, MotaLayout.HEIGHT);
    private final Painter painter;
    /** 字体探针，见类注释（必须强引用，否则绑定与监听会随它一起被回收） */
    private final Text fontProbe;

    public CanvasView(Painter painter) {
        this.painter = painter;
        // FontType.TEXT 是 FXGL 最后注册的一种字体：它变了，其余几种也都已就绪
        fontProbe = FXGL.getUIFactoryService().newText("", Color.TRANSPARENT, FontType.TEXT, 12);
        fontProbe.fontProperty().addListener((obs, old, font) -> {
            if (canvas.getScene() != null) {
                render();
            }
        });
    }

    public Canvas node() {
        return canvas;
    }

    public void render() {
        painter.paint(canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight());
    }
}
