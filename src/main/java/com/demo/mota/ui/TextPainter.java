package com.demo.mota.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Canvas 文本绘制的公共工具：阴影描边、按宽度自适应字号、宽度测量与折行。
 *
 * <p>内部持有一个离屏 {@link Text} 节点用于测量，非线程安全，
 * 每个渲染器各自持有一个实例即可（均运行在 JavaFX 应用线程上）。
 */
public class TextPainter {

    private final Text measurer = new Text();

    /** 黑色偏移描边 + 白色正文 */
    public void drawShadowText(GraphicsContext gc, String text, double x, double y) {
        drawShadowText(gc, text, x, y, Color.WHITE);
    }

    /** 黑色偏移描边 + 指定颜色正文 */
    public void drawShadowText(GraphicsContext gc, String text, double x, double y, Color color) {
        gc.setFill(Color.BLACK);
        gc.fillText(text, x + 2, y + 2);
        gc.setFill(color);
        gc.fillText(text, x, y);
    }

    /** 以 rightX 为右边界右对齐绘制 */
    public void drawRightAlignedShadowText(GraphicsContext gc, String text, double rightX, double y, Color color) {
        drawShadowText(gc, text, rightX - measureWidth(text, gc.getFont()), y, color);
    }

    /** 以 centerX 为中心居中绘制 */
    public void drawCenteredShadowText(GraphicsContext gc, String text, double centerX, double y, Color color) {
        drawShadowText(gc, text, centerX - measureWidth(text, gc.getFont()) / 2, y, color);
    }

    /** 以能放进 maxWidth 的最大字号绘制（字号下限 minSize） */
    public void drawFittedShadowText(GraphicsContext gc, String text, String family,
                                     double x, double y, double maxWidth,
                                     double preferredSize, double minSize) {
        gc.setFont(fitFont(text, family, preferredSize, minSize, maxWidth));
        drawShadowText(gc, text, x, y);
    }

    /**
     * 从 preferredSize 起逐级缩小字号，直到文本宽度不超过 maxWidth。
     * 缩到 minSize 仍放不下时返回 minSize 字号（此时应配合紧凑记法使用）。
     */
    public Font fitFont(String text, String family, double preferredSize, double minSize, double maxWidth) {
        double size = preferredSize;
        Font font = Font.font(family, FontWeight.BOLD, size);
        while (size > minSize && measureWidth(text, font) > maxWidth) {
            size -= 1;
            font = Font.font(family, FontWeight.BOLD, size);
        }
        return font;
    }

    public double measureWidth(String text, Font font) {
        measurer.setFont(font);
        measurer.setText(text);
        return measurer.getLayoutBounds().getWidth();
    }

    /** 按可用宽度把一行文本折成多行（逐字符累积，适配中文无空格断行） */
    public List<String> wrap(String text, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            line.append(text.charAt(i));
            if (measureWidth(line.toString(), font) > maxWidth && line.length() > 1) {
                line.deleteCharAt(line.length() - 1);
                lines.add(line.toString());
                line = new StringBuilder().append(text.charAt(i));
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
