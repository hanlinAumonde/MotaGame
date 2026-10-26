package com.demo.mota.ui.kit;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * 文字测量（原 {@code TextPainter} 去掉绘制后剩下的部分）：宽度、按宽度自适应字号、中文折行。
 *
 * <p>内部持有一个离屏 {@link Text} 节点用于测量，非线程安全，每个使用方各自持有一个实例。
 */
public final class TextMetrics {

    private final Text measurer = new Text();

    public double width(String text, Font font) {
        measurer.setFont(font);
        measurer.setText(text);
        return measurer.getLayoutBounds().getWidth();
    }

    /**
     * 从 preferredSize 起逐级缩小字号，直到文本宽度不超过 maxWidth；缩到 minSize 仍放不下时返回 minSize 字号。
     *
     * @param face 字体（按字号取），如 {@code Fonts::bold}、{@code Fonts::mono}
     */
    public Font fit(String text, DoubleFunction<Font> face, double preferredSize, double minSize, double maxWidth) {
        double size = preferredSize;
        Font font = face.apply(size);
        while (size > minSize && width(text, font) > maxWidth) {
            size -= 1;
            font = face.apply(size);
        }
        return font;
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
            if (width(line.toString(), font) > maxWidth && line.length() > 1) {
                line.deleteCharAt(line.length() - 1);
                lines.add(line.toString());
                line = new StringBuilder().append(text.charAt(i));
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
