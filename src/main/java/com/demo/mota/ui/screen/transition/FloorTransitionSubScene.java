package com.demo.mota.ui.screen.transition;

import com.almasb.fxgl.scene.SubScene;
import com.demo.mota.app.nav.Navigator;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MotaLayout;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.animationBuilder;

/**
 * 换层过渡（子场景）：黑幕只盖地图区域，两侧的状态栏照常显示。
 *
 * <pre>
 *   落幕 0.15 秒 ──▶ 全黑：回调 onBlack（在黑幕下把地图换成新楼层）──▶ 停留 0.3 秒、写出楼层名 ──▶ 升幕 0.2 秒 ──▶ 自动弹出
 * </pre>
 * 期间按键只交给本场景（什么都不做），下层的对局暂停更新，地图不会被操作，换图也不会被看到。
 * 动画用 {@code animationBuilder(this)} 绑在本场景的更新上——默认的 {@code animationBuilder()} 绑的是游戏场景，此时它不更新。
 */
public final class FloorTransitionSubScene extends SubScene {

    private static final Duration FADE_OUT = Duration.seconds(0.15);
    private static final Duration HOLD = Duration.seconds(0.3);
    private static final Duration FADE_IN = Duration.seconds(0.2);

    private final Navigator navigator;
    private final Text caption = new Text();
    private final StackPane cover;
    private Runnable onBlack = () -> { };

    public FloorTransitionSubScene(Navigator navigator) {
        this.navigator = navigator;
        caption.setFill(Color.web("#f5f5f5"));
        caption.setTextAlignment(TextAlignment.CENTER);
        caption.setLineSpacing(14);
        cover = new StackPane(new Rectangle(MotaLayout.MAP_SIZE, MotaLayout.MAP_SIZE, Color.BLACK), caption);
        cover.setTranslateX(MotaLayout.MAP_X);
        cover.setTranslateY(MotaLayout.MAP_Y);
        getContentRoot().getChildren().add(cover);
    }

    /**
     * 叠上过渡并开始落幕。
     *
     * @param text    黑屏时显示的文字（可多行，逐行居中）
     * @param onBlack 黑幕落定的那一刻调用
     */
    public void play(String text, Runnable onBlack) {
        caption.setText(text);
        this.onBlack = onBlack;
        navigator.push(this);
    }

    @Override
    public void onCreate() {
        caption.setFont(Fonts.bold(34));
        cover.setOpacity(0);
        animationBuilder(this).duration(FADE_OUT)
                .onFinished(this::onCovered)
                .fadeIn(cover)
                .buildAndPlay();
    }

    private void onCovered() {
        onBlack.run();
        animationBuilder(this).delay(HOLD).duration(FADE_IN)
                .onFinished(navigator::pop)
                .fadeOut(cover)
                .buildAndPlay();
    }
}
