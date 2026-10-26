package com.demo.mota.ui.kit;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.function.DoubleFunction;

/**
 * 界面节点的「画板」：在一个绝对布局的 {@link Pane} 上按坐标摆放节点，用法与原来的画布绘制一一对应——
 * 原来的 {@code painter.drawShadowText(gc, ...)} 就是这里的 {@link #text}，{@code drawPanel} 就是 {@link #panel}。
 *
 * <p>为什么不用 VBox / GridPane 这类布局容器：各界面的版式（每一行的基线、每块面板的位置）已经调好、截图对照过，
 * 按原坐标摆节点能保证换成节点后画面不走样；节点化的收益（窗口缩放时文字与线条按矢量清晰缩放、样式进 CSS、
 * 将来可以给节点挂鼠标事件）并不依赖布局容器。
 *
 * <p>样式：整屏底色、面板、选中框是 CSS 类（{@code mota.css} 的 {@code .mota-screen} / {@code .mota-panel} /
 * {@code .mota-selection}，伪类 {@code :active} = 有焦点 / 选中），文字统一带一个不模糊、偏移 2 像素的黑色投影
 * （与原来「先画一遍黑字再画彩字」的效果相同）。坐标约定同画布：文字的 y 是<b>基线</b>。
 */
public final class Sketch {

    public static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    private static final DropShadow SHADOW = new DropShadow(BlurType.ONE_PASS_BOX, Color.BLACK, 0, 0, 2, 2);

    private final Pane layer;
    private final TextMetrics metrics = new TextMetrics();

    public Sketch() {
        this(new Pane());
    }

    public Sketch(Pane layer) {
        this.layer = layer;
    }

    /** 画板本身（挂到场景里的节点） */
    public Pane node() {
        return layer;
    }

    public TextMetrics metrics() {
        return metrics;
    }

    /** 清空，准备按新状态重新摆放 */
    public void clear() {
        layer.getChildren().clear();
    }

    public <T extends Node> T add(T node) {
        layer.getChildren().add(node);
        return node;
    }

    /** 把节点放到 (x, y) 再加入 */
    public <T extends Node> T add(T node, double x, double y) {
        node.relocate(x, y);
        return add(node);
    }

    // ==================== 面板与形状 ====================

    /** 整屏深蓝渐变底（{@code .mota-screen}） */
    public Region screen(double width, double height) {
        return region("mota-screen", 0, 0, width, height, false);
    }

    /** 半透明面板 + 描边；{@code active} 时描边加粗加亮（{@code .mota-panel}） */
    public Region panel(double x, double y, double w, double h, boolean active) {
        return region("mota-panel", x, y, w, h, active);
    }

    /** 选中框：一层白色半透明填充 + 描边；{@code active} 时更亮（{@code .mota-selection}） */
    public Region selection(double x, double y, double w, double h, boolean active) {
        return region("mota-selection", x, y, w, h, active);
    }

    private Region region(String styleClass, double x, double y, double w, double h, boolean active) {
        Region region = new Region();
        region.getStyleClass().add(styleClass);
        region.pseudoClassStateChanged(ACTIVE, active);
        region.setPrefSize(w, h);
        region.setMinSize(w, h);
        region.setMaxSize(w, h);
        return add(region, x, y);
    }

    public Rectangle fill(double x, double y, double w, double h, Paint paint) {
        Rectangle rect = new Rectangle(x, y, w, h);
        rect.setFill(paint);
        return add(rect);
    }

    /** 描边矩形（描边以边线为中心，与画布的 strokeRect 相同） */
    public Rectangle outline(double x, double y, double w, double h, Paint stroke, double lineWidth) {
        Rectangle rect = new Rectangle(x, y, w, h);
        rect.setFill(null);
        rect.setStroke(stroke);
        rect.setStrokeWidth(lineWidth);
        return add(rect);
    }

    /** 虚线框（空槽位） */
    public Rectangle dashed(double x, double y, double w, double h, Paint stroke, double lineWidth, double dash) {
        Rectangle rect = outline(x, y, w, h, stroke, lineWidth);
        rect.getStrokeDashArray().setAll(dash, dash);
        return rect;
    }

    public Line line(double x1, double y1, double x2, double y2, Paint stroke, double lineWidth) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(stroke);
        line.setStrokeWidth(lineWidth);
        return add(line);
    }

    /**
     * 图片缩放到 w × h。按原尺寸显示时关掉平滑（像素图在窗口放大后依旧锐利），缩小显示时保留平滑
     * （与原来画布的缩放效果一致）。
     */
    public ImageView image(Image image, double x, double y, double w, double h) {
        ImageView view = new ImageView(image);
        view.setFitWidth(w);
        view.setFitHeight(h);
        view.setSmooth(w != image.getWidth() || h != image.getHeight());
        return add(view, x, y);
    }

    // ==================== 文字 ====================

    /** 带投影的文字，(x, y) 是基线左端 */
    public Text text(String text, Font font, Paint color, double x, double y) {
        Text node = new Text(x, y, text);
        node.setFont(font);
        node.setFill(color);
        node.setEffect(SHADOW);
        return add(node);
    }

    /** 以 centerX 为中心 */
    public Text centered(String text, Font font, Paint color, double centerX, double y) {
        return text(text, font, color, centerX - metrics.width(text, font) / 2, y);
    }

    /** 以 rightX 为右边界 */
    public Text right(String text, Font font, Paint color, double rightX, double y) {
        return text(text, font, color, rightX - metrics.width(text, font), y);
    }

    /** 以能放进 maxWidth 的最大字号（下限 minSize）显示 */
    public Text fitted(String text, DoubleFunction<Font> face, double preferredSize, double minSize, double maxWidth,
                       Paint color, double x, double y) {
        return text(text, metrics.fit(text, face, preferredSize, minSize, maxWidth), color, x, y);
    }

    public double width(String text, Font font) {
        return metrics.width(text, font);
    }
}
