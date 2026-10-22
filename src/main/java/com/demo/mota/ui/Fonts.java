package com.demo.mota.ui;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.ui.FontType;
import javafx.scene.text.Font;

/**
 * 界面字体的唯一入口：取 FXGL 注册的打包字体（{@code GameSettings.setFontUI / Game / Mono}，
 * 文件在 {@code assets/ui/fonts/}），不再依赖机器上装没装黑体 / Consolas。
 *
 * <ul>
 *   <li>{@link #regular} —— {@code FontType.UI}，Noto Sans SC 常规（正文、说明）</li>
 *   <li>{@link #bold} —— {@code FontType.GAME}，Noto Sans SC 粗体（标题、选项、名称）</li>
 *   <li>{@link #mono} —— {@code FontType.MONO}，Inconsolata 特粗（数值、伤害数字）</li>
 * </ul>
 *
 * <p>FXGL 在全部引擎服务初始化之后才异步注册这几种字体，注册完成之前取到的是 JavaFX 默认字体；
 * 界面每次绘制都现取，注册完成后的下一次绘制即恢复正常。
 */
public final class Fonts {

    private Fonts() {
    }

    public static Font regular(double size) {
        return FXGL.getUIFactoryService().newFont(FontType.UI, size);
    }

    public static Font bold(double size) {
        return FXGL.getUIFactoryService().newFont(FontType.GAME, size);
    }

    public static Font mono(double size) {
        return FXGL.getUIFactoryService().newFont(FontType.MONO, size);
    }
}
