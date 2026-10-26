package com.demo.mota.scene.title;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.scene.Scene;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.ui.OptionMenu;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * 标题界面（FXGL 的主菜单，{@link MenuType#MAIN_MENU}）：游戏标题 + 开始游戏 / 继续游戏 / 退出游戏。
 *
 * <p>FXGL 在<b>服务初始化阶段、内容载入之前</b>就构造了主菜单（源码：{@code GameApplicationService.initAppScenes}），
 * 所以构造时只挂按键，要用到内容与存档的事都经 {@link Actions} 在进入时再做。
 * 「继续游戏」只在有能读取的存档时可选（否则画灰、光标跳过），选中后叠上读档界面挑一个档位；
 * 选项在每次进入标题、以及从读档界面 Esc 回来时按存档索引重建。
 */
public class TitleMenu extends FXGLMenu {

    /** 标题菜单要做的事，由组装处在服务就绪后提供 */
    public interface Actions {
        boolean hasAnySave();

        /** 准备好全新的一局并开局 */
        void startNewGame();

        /** 叠上读档界面 */
        void openLoad();

        void exit();
    }

    private static final String START = "start";
    private static final String CONTINUE = "continue";
    private static final String QUIT = "quit";

    private static final Color BACKGROUND = Color.web("#12131a");
    private static final Color SELECTED = Color.web("#f0c96b");
    private static final Color NORMAL = Color.web("#e8e8e8");
    private static final Color DISABLED = Color.web("#666a77");

    private final Actions actions;
    private final Sketch sketch = new Sketch();

    private boolean hasSave;
    private OptionMenu options = buildOptions(false);

    public TitleMenu(Actions actions) {
        super(MenuType.MAIN_MENU);
        this.actions = actions;
        getContentRoot().getChildren().add(sketch.node());
        // 标题可能抢在 FXGL 注册打包字体之前显示：注册完成时若正显示着就重摆一次
        Fonts.whenRegistered(() -> {
            if (sketch.node().getScene() != null) {
                render();
            }
        });
        getInput().addEventHandler(KeyEvent.KEY_PRESSED, e -> handleKey(e.getCode()));
    }

    private static OptionMenu buildOptions(boolean hasSave) {
        return new OptionMenu(List.of(
                OptionMenu.Option.of(START, "开始游戏"),
                new OptionMenu.Option(CONTINUE, "继续游戏", hasSave),
                OptionMenu.Option.of(QUIT, "退出游戏")
        ));
    }

    @Override
    public void onCreate() {
        refresh();
    }

    /**
     * 从叠在上面的界面回来（读档界面、对话框）：存档索引可能变了，「继续游戏」能否选跟着变；
     * 没变就保留光标——从「退出游戏？」对话框取消回来，光标应当还停在「退出游戏」上。
     */
    @Override
    public void onEnteredFrom(Scene previous) {
        if (previous.isSubState() && hasSave != actions.hasAnySave()) {
            refresh();
        }
    }

    private void refresh() {
        hasSave = actions.hasAnySave();
        options = buildOptions(hasSave);
        render();
    }

    private void handleKey(KeyCode code) {
        MenuCommand command = toCommand(code);
        if (command == null) return;

        String chosen = options.handle(command);
        if (chosen == null) {
            render();
            return;
        }
        switch (chosen) {
            case START -> actions.startNewGame();
            case CONTINUE -> actions.openLoad();
            case QUIT -> actions.exit();
            default -> render();
        }
    }

    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP, W -> MenuCommand.UP;
            case DOWN, S -> MenuCommand.DOWN;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            default -> null;
        };
    }

    private void render() {
        sketch.clear();
        double w = MotaLayout.WIDTH;
        double h = MotaLayout.HEIGHT;
        sketch.fill(0, 0, w, h, BACKGROUND);
        sketch.centered("魔  塔", Fonts.bold(76), Color.web("#f5f5f5"), w / 2, h * 0.3);
        sketch.centered("W / S 选择    Enter 确认", Fonts.regular(20), Color.web("#8d93a3"), w / 2, h * 0.3 + 44);

        double optionY = h * 0.5;
        double lineHeight = 54;
        List<OptionMenu.Option> list = options.getOptions();
        for (int i = 0; i < list.size(); i++) {
            OptionMenu.Option option = list.get(i);
            boolean selected = i == options.getSelectedIndex();
            double y = optionY + i * lineHeight;
            Font font = selected ? Fonts.bold(32) : Fonts.regular(28);
            Color color = !option.enabled() ? DISABLED : selected ? SELECTED : NORMAL;
            sketch.centered(option.label(), font, color, w / 2, y);
            if (selected) {
                double markerX = w / 2 - sketch.width(option.label(), font) / 2 - 34;
                sketch.text("▶", font, SELECTED, markerX, y);
            }
        }
    }
}
