package com.demo.mota.ui.dialog;

import com.almasb.fxgl.scene.SceneService;
import com.almasb.fxgl.scene.SubScene;
import com.almasb.fxgl.ui.DialogBox;
import com.almasb.fxgl.ui.DialogService;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.ui.kit.Sketch;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 游戏风格的对话框（替换 FXGL 自带的 {@code FXGLDialogService}，经 {@code setEngineServiceProvider} 注册）。
 *
 * <p>FXGL 自带的实现把对话框装进一个带标题栏的窗口，标题是写死的英文（「Confirm」「Message」），换掉
 * {@code DialogFactoryService} 也去不掉；所以直接实现 {@link DialogService}：一个叠在当前场景上的子场景，
 * 半透明黑色遮罩 + 居中的面板（与菜单同一套 {@code .mota-panel} 样式）+ 一行选项。
 * 包括 FXGL 自己的关窗确认在内，{@code getDialogService()} 弹出的所有对话框都是这个样子。
 *
 * <p>操作与菜单一致：←→（或 ↑↓）选择、Enter / Space 确认、Esc 等于选「取消 / 否」；选项也可以用鼠标点。
 * 对话框显示期间又来一个时，先显示新的，关掉后回到原来那个。
 */
public final class MotaDialogService extends DialogService {

    private static final DropShadow SHADOW = new DropShadow(BlurType.ONE_PASS_BOX, Color.BLACK, 0, 0, 2, 2);
    private static final double MESSAGE_WIDTH = 520;

    /** 由 FXGL 按字段类型注入 */
    private SceneService sceneService;

    private final DialogScene scene = new DialogScene();
    private final Deque<Dialog> stack = new ArrayDeque<>();

    // ==================== DialogService ====================

    @Override
    public void showMessageBox(String message) {
        showMessageBox(message, () -> { });
    }

    @Override
    public void showMessageBox(String message, Runnable callback) {
        show(new Dialog(message, null, List.of(new Option("确定", callback)), 0));
    }

    @Override
    public void showConfirmationBox(String message, Consumer<Boolean> resultCallback) {
        show(new Dialog(message, null, List.of(
                new Option("确定", () -> resultCallback.accept(true)),
                new Option("取消", () -> resultCallback.accept(false))), 1));
    }

    @SafeVarargs
    @Override
    public final <T> void showChoiceBox(String message, Consumer<T> resultCallback, T firstOption, T... options) {
        List<T> all = new ArrayList<>();
        all.add(firstOption);
        all.addAll(Arrays.asList(options));
        showChoiceBox(message, all, resultCallback);
    }

    @Override
    public <T> void showChoiceBox(String message, List<T> options, Consumer<T> resultCallback) {
        List<Option> choices = options.stream()
                .map(option -> new Option(String.valueOf(option), () -> resultCallback.accept(option)))
                .toList();
        show(new Dialog(message, null, choices, -1));
    }

    @Override
    public <T extends Enum<T>> void showChoiceBox(String message, Class<T> enumClass, Consumer<T> resultCallback) {
        showChoiceBox(message, List.of(enumClass.getEnumConstants()), resultCallback);
    }

    @Override
    public void showInputBox(String message, Consumer<String> resultCallback) {
        showInputBox(message, text -> true, resultCallback);
    }

    @Override
    public void showInputBox(String message, Predicate<String> filter, Consumer<String> resultCallback) {
        showInput(message, filter, resultCallback, false);
    }

    @Override
    public void showInputBoxWithCancel(String message, Predicate<String> filter, Consumer<String> resultCallback) {
        showInput(message, filter, resultCallback, true);
    }

    private void showInput(String message, Predicate<String> filter, Consumer<String> resultCallback, boolean cancel) {
        TextField field = new TextField();
        field.setFont(Fonts.regular(20));
        field.setMaxWidth(MESSAGE_WIDTH);
        List<Option> options = new ArrayList<>();
        options.add(new Option("确定", () -> resultCallback.accept(field.getText()),
                () -> filter.test(field.getText())));
        if (cancel) {
            options.add(new Option("取消", () -> resultCallback.accept("")));
        }
        show(new Dialog(message, field, options, cancel ? 1 : -1));
        Platform.runLater(field::requestFocus);
    }

    @Override
    public void showErrorBox(Throwable error) {
        showErrorBox("发生错误：" + error.getMessage(), () -> { });
    }

    @Override
    public void showErrorBox(String errorMessage, Runnable callback) {
        show(new Dialog(errorMessage, null, List.of(new Option("确定", callback)), 0));
    }

    /** 自定义内容 + FXGL 按钮：按钮被点时先关掉对话框，再执行它原来的动作 */
    @Override
    public void showBox(String message, Node content, Button... buttons) {
        Dialog dialog = new Dialog(message, content, List.of(), -1);
        for (Button button : buttons) {
            var action = button.getOnAction();
            button.setOnAction(event -> {
                close(dialog);
                if (action != null) {
                    action.handle(event);
                }
            });
        }
        dialog.extra = new HBox(16, buttons);
        show(dialog);
    }

    @Override
    public DialogBox showProgressBox(String message) {
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(MESSAGE_WIDTH * 0.8);
        Dialog dialog = new Dialog(message, bar, List.of(), -1);
        show(dialog);
        return () -> close(dialog);
    }

    @Override
    public void showProgressBox(String message, ReadOnlyDoubleProperty observable, Runnable callback) {
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(MESSAGE_WIDTH * 0.8);
        bar.progressProperty().bind(observable);
        Dialog dialog = new Dialog(message, bar, List.of(), -1);
        observable.addListener((obs, old, value) -> {
            if (value.doubleValue() >= 1 && stack.contains(dialog)) {
                close(dialog);
                callback.run();
            }
        });
        show(dialog);
    }

    // ==================== 显示与关闭 ====================

    private void show(Dialog dialog) {
        stack.push(dialog);
        scene.display(dialog);
        if (sceneService.getCurrentScene() != scene) {
            sceneService.pushSubScene(scene);
        }
    }

    private void close(Dialog dialog) {
        stack.remove(dialog);
        if (stack.isEmpty()) {
            if (sceneService.getCurrentScene() == scene) {
                sceneService.popSubScene();
            }
        } else {
            scene.display(stack.peek());
        }
    }

    /** 选中一项：先关对话框，再执行（回调里常常又会切场景） */
    private void choose(Dialog dialog, Option option) {
        if (!option.enabled().get()) {
            return;
        }
        close(dialog);
        option.action().run();
    }

    // ==================== 模型 ====================

    private record Option(String label, Runnable action, Supplier<Boolean> enabled) {
        Option(String label, Runnable action) {
            this(label, action, () -> true);
        }
    }

    private static final class Dialog {
        final String message;
        final Node content;
        final List<Option> options;
        /** Esc 等于选哪一项；-1 表示 Esc 不起作用（必须明确选一项） */
        final int cancelIndex;
        Node extra;
        int selected;

        Dialog(String message, Node content, List<Option> options, int cancelIndex) {
            this.message = message;
            this.content = content;
            this.options = options;
            this.cancelIndex = cancelIndex;
            // 默认停在第一项（确定）
        }
    }

    // ==================== 场景 ====================

    private final class DialogScene extends SubScene {

        private final StackPane root = new StackPane();
        private final VBox panel = new VBox(22);
        private Dialog current;
        private boolean acceptingKeys;

        DialogScene() {
            root.setPrefSize(MotaLayout.WIDTH, MotaLayout.HEIGHT);
            root.getStyleClass().add("mota-dialog-backdrop");
            panel.getStyleClass().add("mota-panel");
            panel.pseudoClassStateChanged(Sketch.ACTIVE, true);
            panel.setAlignment(Pos.CENTER);
            panel.setPadding(new Insets(30, 40, 26, 40));
            panel.setMaxSize(MESSAGE_WIDTH + 80, StackPane.USE_PREF_SIZE);
            root.getChildren().add(panel);
            getContentRoot().getChildren().add(root);
            getInput().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (acceptingKeys && current != null) onKey(e.getCode());
            });
        }

        /** 与菜单子场景相同：叠上本场景的那个按键不交给本场景，等它派发完才开始接收 */
        @Override
        public void onCreate() {
            acceptingKeys = false;
            Platform.runLater(() -> acceptingKeys = true);
        }

        void display(Dialog dialog) {
            current = dialog;
            Text message = new Text(dialog.message);
            message.setFont(Fonts.regular(22));
            message.setFill(Color.WHITE);
            message.setEffect(SHADOW);
            message.setWrappingWidth(MESSAGE_WIDTH);
            message.setTextAlignment(TextAlignment.CENTER);
            panel.getChildren().setAll(message);
            if (dialog.content != null) {
                panel.getChildren().add(dialog.content);
            }
            if (dialog.extra != null) {
                panel.getChildren().add(dialog.extra);
            }
            if (!dialog.options.isEmpty()) {
                panel.getChildren().add(optionRow(dialog));
            }
        }

        /** 一行选项：选中的那项是金色粗体、底下垫一块选中框（与菜单的 {@code .mota-selection} 相同） */
        private HBox optionRow(Dialog dialog) {
            HBox row = new HBox(28);
            row.setAlignment(Pos.CENTER);
            for (int i = 0; i < dialog.options.size(); i++) {
                boolean selected = i == dialog.selected;
                Text label = new Text(dialog.options.get(i).label());
                label.setFont(selected ? Fonts.bold(24) : Fonts.regular(22));
                label.setFill(selected ? Color.web("#f0c96b") : Color.web("#e8e8e8"));
                label.setEffect(SHADOW);
                StackPane cell = new StackPane(label);
                cell.setPadding(new Insets(6, 22, 6, 22));
                if (selected) {
                    cell.getStyleClass().add("mota-selection");
                    cell.pseudoClassStateChanged(Sketch.ACTIVE, true);
                }
                int index = i;
                cell.setOnMouseClicked(e -> choose(dialog, dialog.options.get(index)));
                cell.setOnMouseEntered(e -> {
                    // 只在换了选中项时重摆：重摆会换掉鼠标下的节点，无条件重摆会反复触发
                    if (dialog.selected != index) {
                        dialog.selected = index;
                        display(dialog);
                    }
                });
                row.getChildren().add(cell);
            }
            return row;
        }

        private void onKey(KeyCode code) {
            Dialog dialog = current;
            int count = dialog.options.size();
            switch (code) {
                case LEFT, UP, A, W -> {
                    if (count > 0 && !(dialog.content instanceof TextField)) {
                        dialog.selected = Math.floorMod(dialog.selected - 1, count);
                        display(dialog);
                    }
                }
                case RIGHT, DOWN, D, S -> {
                    if (count > 0 && !(dialog.content instanceof TextField)) {
                        dialog.selected = Math.floorMod(dialog.selected + 1, count);
                        display(dialog);
                    }
                }
                case ENTER, SPACE -> {
                    if (count > 0 && !(code == KeyCode.SPACE && dialog.content instanceof TextField)) {
                        choose(dialog, dialog.options.get(dialog.selected));
                    }
                }
                case ESCAPE -> {
                    if (dialog.cancelIndex >= 0) {
                        choose(dialog, dialog.options.get(dialog.cancelIndex));
                    }
                }
                default -> { }
            }
        }
    }
}
