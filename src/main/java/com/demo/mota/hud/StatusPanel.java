package com.demo.mota.hud;

import com.almasb.fxgl.core.collection.PropertyMap;
import com.almasb.fxgl.dsl.FXGL;
import com.demo.mota.app.services.AssetService;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.kit.Sketch;
import com.demo.mota.ui.kit.TextMetrics;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.function.DoubleFunction;

/**
 * 左侧状态栏：头像与等级、五项属性、三种钥匙、区域与楼层、一句提示。
 *
 * <p>节点在每局开始时搭一次，文字<b>绑定</b>世界变量（{@link HudModel}）：数值变了只改那一个 {@link Text}，
 * 不再每次按键整块重画。属性值的字号随内容自适应（过长的数字缩小到放得下）。
 * 版式与节点化之前的画布版一致：墙砖底纹平铺（32 像素一块）、文字带 2 像素黑色投影。
 */
public final class StatusPanel {

    private static final double STATS_Y = 115;
    private static final double STATS_X = 14;
    private static final double STAT_LINE = 38;
    private static final double VALUE_X = STATS_X + 60;
    private static final double KEYS_Y = 340;
    private static final double KEY_ICON = 40;
    private static final double KEY_LINE = 58;
    private static final double MESSAGE_Y = 530;

    private final Sketch sketch = new Sketch();
    private final TextMetrics metrics = new TextMetrics();

    /**
     * 搭好状态栏并绑定当前这一局的世界变量。须在 {@code initGameVars} 之后（{@code initUI} 里）创建：
     * FXGL 每局都会清空并重新声明世界变量。
     */
    public StatusPanel(AssetService assets, double width, double height) {
        Pane node = sketch.node();
        node.setPrefSize(width, height);
        node.setMinSize(width, height);
        node.setMaxSize(width, height);
        PropertyMap vars = FXGL.getWorldProperties();

        //background(assets.sprite("wall_normal"), width, height);
        sketch.fill(0, 0, width, height, Color.BLACK);

        // 头像与等级
        Image avatar = assets.idleFrame(AssetService.PLAYER, Direction.DOWN);
        if (avatar != null) {
            sketch.image(avatar, 14, 18, 52, 52);
        }
        bind(sketch.text("", Fonts.mono(34), Color.WHITE, 14 + 52 + 18, 18 + 38),
                vars.intProperty(HudModel.LEVEL).asString());
        sketch.text("级", Fonts.bold(28), Color.WHITE, width - 42, 18 + 38);

        // 五项属性：标签 + 自适应字号的数值
        String[] labels = {"生命", "攻击", "防御", "金币", "经验"};
        String[] keys = {HudModel.HP, HudModel.ATK, HudModel.DEF, HudModel.GOLD, HudModel.EXP};
        double valueMaxWidth = width - VALUE_X - 8;
        for (int i = 0; i < labels.length; i++) {
            double y = STATS_Y + i * STAT_LINE;
            sketch.text(labels[i], Fonts.bold(22), Color.WHITE, STATS_X, y);
            Text value = sketch.text("", Fonts.mono(22), Color.WHITE, VALUE_X, y);
            bindFitted(value, vars.stringProperty(keys[i]), Fonts::mono, 22, 12, valueMaxWidth);
        }

        // 钥匙
        String[] keyItems = {"key-001-yellow", "key-001-blue", "key-001-red"};
        String[] keyVars = {HudModel.KEYS_YELLOW, HudModel.KEYS_BLUE, HudModel.KEYS_RED};
        for (int i = 0; i < keyItems.length; i++) {
            double y = KEYS_Y + i * KEY_LINE;
            Image icon = assets.itemImage(keyItems[i]);
            if (icon != null) {
                sketch.image(icon, STATS_X, y, KEY_ICON, KEY_ICON);
            }
            bind(sketch.text("", Fonts.mono(26), Color.WHITE, STATS_X + KEY_ICON + 14, y + KEY_ICON * 0.7),
                    Bindings.format("%02d", vars.intProperty(keyVars[i])));
            sketch.text("个", Fonts.bold(22), Color.WHITE, STATS_X + KEY_ICON + 64, y + KEY_ICON * 0.7);
        }

        // 区域与楼层
        bind(sketch.text("", Fonts.regular(16), Color.WHITE, 14, height - 58), vars.stringProperty(HudModel.REGION));
        bind(sketch.text("", Fonts.bold(26), Color.WHITE, 14, height - 25),
                Bindings.format("第 %d 层", vars.intProperty(HudModel.FLOOR)));

        // 提示：有内容时才显示（不带投影，与原来一样）
        Rectangle messageBox = sketch.fill(6, MESSAGE_Y, width - 12, 28, Color.rgb(0, 0, 0, 0.6));
        Text message = sketch.text("", Fonts.regular(15), Color.web("#ff6b6b"), 12, MESSAGE_Y + 20);
        message.setEffect(null);
        message.textProperty().bind(vars.stringProperty(HudModel.MESSAGE));
        messageBox.visibleProperty().bind(message.textProperty().isNotEmpty());
        message.visibleProperty().bind(messageBox.visibleProperty());
    }

    public Pane node() {
        return sketch.node();
    }

//    /** 墙砖底纹 **/
//    private void background(Image tile, double width, double height) {
//        if (tile != null) {
//           sketch.fill(0, 0, width, height, new ImagePattern(tile, 0, 0, 32, 32, false));
//       } else {
//           sketch.fill(0, 0, width, height, Color.BLACK);
//       }
//    }

    private static void bind(Text text, ObservableStringValue value) {
        text.textProperty().bind(value);
    }

    /** 绑定文字，并在内容变化时按可用宽度重选字号 */
    private void bindFitted(Text text, ObservableStringValue value, DoubleFunction<Font> face,
                            double preferredSize, double minSize, double maxWidth) {
        text.textProperty().bind(value);
        text.textProperty().addListener((obs, old, now) ->
                text.setFont(metrics.fit(now, face, preferredSize, minSize, maxWidth)));
        text.setFont(metrics.fit(text.getText(), face, preferredSize, minSize, maxWidth));
    }
}
