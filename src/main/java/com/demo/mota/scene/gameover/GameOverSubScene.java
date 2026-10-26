package com.demo.mota.scene.gameover;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.scene.Scene;
import com.demo.mota.app.nav.Navigator;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.scene.MenuSubScene;
import com.demo.mota.scene.saveload.SaveLoadMode;
import com.demo.mota.scene.saveload.SaveLoadSubScene;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.ui.OptionMenu;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * 游戏结束横幅：贴在屏幕底部的一条框，读档 / 返回标题。
 *
 * <p>它<b>不铺满整屏</b>——玩家倒在哪儿、被谁打死的，画面上要看得见。子场景叠在对局之上时，
 * 下层的对局照常显示（只是不再更新），这里只在底部摆一条框、其余保持透明。
 * 「读取存档」只在有能读取的存档时可选，叠上读档界面挑一个档位（Esc 回到这里）。
 *
 * <p>还有撤销点时按 Backspace 撤回致死的那一战（弹回对局、回到战前），横幅上相应地多一行提示。
 */
public class GameOverSubScene extends MenuSubScene {

    private static final String LOAD = "load";
    private static final String TITLE = "title";

    private static final Color PANEL = Color.rgb(16, 17, 24, 0.92);
    private static final Color BORDER = Color.web("#b03a3a");
    private static final Color SELECTED = Color.web("#f0c96b");
    private static final Color NORMAL = Color.web("#e8e8e8");
    private static final Color DISABLED = Color.web("#666a77");

    private static final double PANEL_HEIGHT = 150;

    private final Navigator navigator;
    private final SessionService sessions;
    private final SaveLoadSubScene saveLoad;
    /** 撤销（即对局界面的 {@code undoLastAction}：恢复快照并写状态栏提示），在弹回对局之后调用 */
    private final Runnable undo;

    private OptionMenu options = buildOptions(false);

    public GameOverSubScene(Navigator navigator, SessionService sessions, SaveLoadSubScene saveLoad, Runnable undo) {
        this.navigator = navigator;
        this.sessions = sessions;
        this.saveLoad = saveLoad;
        this.undo = undo;
    }

    private static OptionMenu buildOptions(boolean hasSave) {
        return new OptionMenu(List.of(
                new OptionMenu.Option(LOAD, "读取存档", hasSave),
                OptionMenu.Option.of(TITLE, "返回标题")
        ));
    }

    @Override
    protected void onOpen() {
        options = buildOptions(sessions.saves().hasAnySave());
    }

    /** 从读档界面 Esc 回到这里：存档可能刚被别处写过，选项按存档索引重建 */
    @Override
    public void onEnteredFrom(Scene previous) {
        if (previous == saveLoad) {
            onOpen();
            render();
        }
    }

    @Override
    protected void onKey(KeyCode code) {
        if (code == KeyCode.BACK_SPACE) {
            if (sessions.canUndo()) {
                navigator.pop();
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
            // 这一局原样留着，下次「开始游戏」时 startNewGame 整套清场重建
            FXGL.getDialogService().showConfirmationBox("返回标题？这一局的进度会丢失。", yes -> {
                if (yes) {
                    FXGL.getGameController().gotoMainMenu();
                }
            });
        } else if (LOAD.equals(chosen)) {
            saveLoad.open(SaveLoadMode.LOAD, false);
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
    protected void draw(Sketch s) {
        double w = MotaLayout.WIDTH;
        double h = MotaLayout.HEIGHT;
        // 只摆底部一条横幅，其余透明，让下层的对局透出来
        double panelY = h - PANEL_HEIGHT;
        s.fill(0, panelY, w, PANEL_HEIGHT, PANEL);
        s.line(0, panelY + 1.5, w, panelY + 1.5, BORDER, 3);

        s.centered("你死了", Fonts.bold(30), Color.web("#ff7a7a"), w / 2, panelY + 50);
        if (sessions.canUndo()) {
            s.centered("按 Backspace 撤销这一战", Fonts.regular(16), NORMAL, w / 2, panelY + 74);
        }

        List<OptionMenu.Option> list = options.getOptions();
        double slotWidth = w / (list.size() + 1);
        double optionY = panelY + 108;
        for (int i = 0; i < list.size(); i++) {
            OptionMenu.Option option = list.get(i);
            boolean selected = i == options.getSelectedIndex();
            double centerX = slotWidth * (i + 1);
            Font font = selected ? Fonts.bold(26) : Fonts.regular(23);
            Color color = !option.enabled() ? DISABLED : selected ? SELECTED : NORMAL;
            s.centered(option.label(), font, color, centerX, optionY);
            if (selected) {
                double markerX = centerX - s.width(option.label(), font) / 2 - 28;
                s.text("▶", font, SELECTED, markerX, optionY);
            }
        }
    }
}
