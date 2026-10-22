package com.demo.mota.ui.screen.gameover;

import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.OptionMenu;
import com.demo.mota.ui.TextPainter;
import com.demo.mota.ui.screen.Screen;
import com.demo.mota.ui.screen.saveload.SaveLoadMode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 游戏结束提示框：贴在屏幕底部的一条横幅，读档 / 返回标题。
 *
 * <p>与标题、加载两个界面不同，它<b>不铺满整屏</b>——玩家倒在哪儿、被谁打死的，
 * 画面上要看得见，所以只在底部画一条框，上方的游戏画面照常透出来。
 * 「读取存档」只在有能读取的存档时可选，进入读档界面挑一个档位（Esc 回到这里）。
 *
 * <p>还有撤销点时按 Backspace 撤回致死的那一战（回到战前、迁回对局），横幅上相应地多一行提示。
 */
public class GameOverScreen implements Screen {

    private static final String LOAD = "load";
    private static final String TITLE = "title";

    private static final Color PANEL = Color.rgb(16, 17, 24, 0.92);
    private static final Color BORDER = Color.web("#b03a3a");
    private static final Color SELECTED = Color.web("#f0c96b");
    private static final Color NORMAL = Color.web("#e8e8e8");
    private static final Color DISABLED = Color.web("#666a77");

    private static final double PANEL_HEIGHT = 150;

    private final Canvas canvas;
    private final GameFlow flow;
    private final TextPainter painter = new TextPainter();

    /** 是否还有可撤销的行动 */
    private final BooleanSupplier canUndo;
    /** 撤销（即 {@code GameScreen.undoLastAction}：恢复快照、迁回对局并写状态栏提示） */
    private final Runnable undo;

    /** 是否有能读取的存档 */
    private final BooleanSupplier hasSave;
    /** 进入存读档界面前交代用途（即 {@code SaveLoadScreen.prepare}） */
    private final Consumer<SaveLoadMode> saveLoad;

    private OptionMenu options = buildOptions(false);

    public GameOverScreen(Canvas canvas, GameFlow flow, BooleanSupplier canUndo, Runnable undo,
                          BooleanSupplier hasSave, Consumer<SaveLoadMode> saveLoad) {
        this.canvas = canvas;
        this.flow = flow;
        this.canUndo = canUndo;
        this.undo = undo;
        this.hasSave = hasSave;
        this.saveLoad = saveLoad;
    }

    private static OptionMenu buildOptions(boolean hasSave) {
        return new OptionMenu(List.of(
                new OptionMenu.Option(LOAD, "读取存档", hasSave),
                OptionMenu.Option.of(TITLE, "返回标题")
        ));
    }

    /** 只画底部一条横幅，上方的游戏画面要透出来 */
    @Override
    public boolean overlaysGame() {
        return true;
    }

    @Override
    public void onEnter() {
        options = buildOptions(hasSave.getAsBoolean());
    }

    @Override
    public void handleKey(KeyCode code) {
        if (code == KeyCode.BACK_SPACE) {
            if (canUndo.getAsBoolean()) {
                undo.run();
            }
            return;
        }
        MenuCommand command = toCommand(code);
        if (command == null) return;

        String chosen = options.handle(command);
        if (chosen == null) {
            render();
            return;
        }
        if (TITLE.equals(chosen)) {
            // 这一局的引擎状态原样留着，下次「开始游戏」时由 startNewGame 整套重建
            flow.to(GamePhase.TITLE);
        } else if (LOAD.equals(chosen)) {
            saveLoad.accept(SaveLoadMode.LOAD);
            flow.to(GamePhase.SAVE_LOAD);
        }
    }

    /** 结束框是左右排布的，左右键与 W/S 一样能切换选项 */
    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP, W, LEFT, A -> MenuCommand.UP;
            case DOWN, S, RIGHT, D -> MenuCommand.DOWN;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            default -> null;
        };
    }

    @Override
    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // 整屏清空（只清不填），让上方的游戏画面透出来
        gc.clearRect(0, 0, w, h);

        double panelY = h - PANEL_HEIGHT;
        gc.setFill(PANEL);
        gc.fillRect(0, panelY, w, PANEL_HEIGHT);
        gc.setStroke(BORDER);
        gc.setLineWidth(3);
        gc.strokeLine(0, panelY + 1.5, w, panelY + 1.5);

        gc.setFont(Fonts.bold(30));
        painter.drawCenteredShadowText(gc, "你死了", w / 2, panelY + 50, Color.web("#ff7a7a"));
        if (canUndo.getAsBoolean()) {
            gc.setFont(Fonts.regular(16));
            painter.drawCenteredShadowText(gc, "按 Backspace 撤销这一战", w / 2, panelY + 74, NORMAL);
        }

        List<OptionMenu.Option> list = options.getOptions();
        double slotWidth = w / (list.size() + 1);
        double optionY = panelY + 108;
        for (int i = 0; i < list.size(); i++) {
            OptionMenu.Option option = list.get(i);
            boolean selected = i == options.getSelectedIndex();
            double centerX = slotWidth * (i + 1);

            gc.setFont(selected ? Fonts.bold(26) : Fonts.regular(23));
            Color color = !option.enabled() ? DISABLED : selected ? SELECTED : NORMAL;
            painter.drawCenteredShadowText(gc, option.label(), centerX, optionY, color);

            if (selected) {
                double markerX = centerX - painter.measureWidth(option.label(), gc.getFont()) / 2 - 28;
                painter.drawShadowText(gc, "▶", markerX, optionY, SELECTED);
            }
        }
    }
}
