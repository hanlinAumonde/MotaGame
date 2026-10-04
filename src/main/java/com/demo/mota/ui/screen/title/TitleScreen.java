package com.demo.mota.ui.screen.title;

import com.demo.mota.ui.screen.Screen;
import com.demo.mota.engine.GameEngine;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.OptionMenu;
import com.demo.mota.ui.TextPainter;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * 标题界面：游戏标题 + 开始游戏 / 继续游戏 / 退出游戏。
 *
 * <p>「继续游戏」在存档系统落地前是禁用占位项（画灰、光标跳过），
 * 待 {@code SaveManager} 就位后把它的 {@code enabled} 改成「是否存在存档」即可。
 */
public class TitleScreen implements Screen {

    private static final String START = "start";
    private static final String CONTINUE = "continue";
    private static final String QUIT = "quit";

    /** 新开一局从第几层起步 */
    private static final int INITIAL_FLOOR = 1;

    private static final Color BACKGROUND = Color.web("#12131a");
    private static final Color SELECTED = Color.web("#f0c96b");
    private static final Color NORMAL = Color.web("#e8e8e8");
    private static final Color DISABLED = Color.web("#666a77");

    private final Canvas canvas;
    private final GameEngine engine;
    private final TextPainter painter = new TextPainter();

    private final OptionMenu options = new OptionMenu(List.of(
            OptionMenu.Option.of(START, "开始游戏"),
            OptionMenu.Option.disabled(CONTINUE, "继续游戏"),
            OptionMenu.Option.of(QUIT, "退出游戏")
    ));

    public TitleScreen(Canvas canvas, GameEngine engine) {
        this.canvas = canvas;
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        options.reset();
    }

    @Override
    public boolean handleKey(KeyCode code) {
        MenuCommand command = toCommand(code);
        if (command == null) return false;

        String chosen = options.handle(command);
        if (chosen == null) {
            render();
            return true;
        }
        switch (chosen) {
            // startNewGame 内部会把阶段推进到 PLAYING，界面切换由 Controller 订阅阶段变更完成
            case START -> engine.startNewGame(INITIAL_FLOOR);
            case QUIT -> Platform.exit();
            default -> render();
        }
        return true;
    }

    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP, W -> MenuCommand.UP;
            case DOWN, S -> MenuCommand.DOWN;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            default -> null;
        };
    }

    @Override
    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, w, h);

        gc.setFont(Font.font("SimHei", FontWeight.BOLD, 76));
        painter.drawCenteredShadowText(gc, "魔  塔", w / 2, h * 0.3, Color.web("#f5f5f5"));

        gc.setFont(Font.font("SimHei", FontWeight.NORMAL, 20));
        painter.drawCenteredShadowText(gc, "W / S 选择    Enter 确认", w / 2, h * 0.3 + 44,
                Color.web("#8d93a3"));

        double optionY = h * 0.5;
        double lineHeight = 54;
        List<OptionMenu.Option> list = options.getOptions();
        for (int i = 0; i < list.size(); i++) {
            OptionMenu.Option option = list.get(i);
            boolean selected = i == options.getSelectedIndex();
            double y = optionY + i * lineHeight;

            gc.setFont(Font.font("SimHei", selected ? FontWeight.BOLD : FontWeight.NORMAL, selected ? 32 : 28));
            Color color = !option.enabled() ? DISABLED : selected ? SELECTED : NORMAL;
            painter.drawCenteredShadowText(gc, option.label(), w / 2, y, color);

            if (selected) {
                double markerX = w / 2 - painter.measureWidth(option.label(), gc.getFont()) / 2 - 34;
                painter.drawShadowText(gc, "▶", markerX, y, SELECTED);
            }
        }
    }
}
