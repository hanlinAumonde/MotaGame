package com.demo.mota;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.demo.mota.app.MotaSceneFactory;
import com.demo.mota.app.MotaScenes;
import com.demo.mota.app.input.GameplayInput;
import com.demo.mota.app.services.AssetService;
import com.demo.mota.app.services.ContentService;
import com.demo.mota.app.services.DevToolsService;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.ui.MotaLayout;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * 应用入口：生命周期、场景、输入都交给 FXGL。
 *
 * <pre>
 * 启动场景（ContentService / AssetService 在后台线程载入内容与图片，进度画在上面）
 *   └─▶ 标题（FXGL 主菜单）──开始游戏 / 读档──▶ startNewGame()：加载场景（initGame / initUI）──▶ 对局（游戏场景）
 *                                                         对局 ⇄ 游戏菜单（X）、物品栏 / 装备 / 技能设置 / 套装 / 存读档 / 结束横幅（子场景）
 * </pre>
 * 新开一局与读档都走 {@code startNewGame()}：会失败的准备工作（读档、按快照造一局）在调用之前做完，
 * {@link #initGame()} 只装上准备好的这一局（{@link SessionService#installPending()}），{@link #initUI()} 搭对局画面。
 * 界面的组装见 {@link MotaScenes}，对局按键见 {@link GameplayInput}。
 *
 * <p>字体、样式表、窗口图标都按 FXGL 的约定放在 {@code assets/} 下、经 {@link GameSettings} 注册。
 * {@code -Dmota.dev=true} 开启开发模式：FXGL 的开发者面板（F1）/ 控制台（F2）/ 日志；
 * {@code -Dmota.profile=true} 另开性能监视。
 */
public class MotaApplication extends GameApplication {

    /** 界面组装处：菜单由 FXGL 在服务初始化时构造，对局画面与子场景在 initInput 时装配 */
    private final MotaScenes scenes = new MotaScenes();

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("魔塔");
        settings.setVersion("");
        settings.setWidth(MotaLayout.WIDTH);
        settings.setHeight(MotaLayout.HEIGHT);
        // 允许拖动缩放 / 最大化：FXGL 按比例缩放整个场景并留黑边；不提供全屏
        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);
        settings.setFullScreenAllowed(false);
        settings.setIntroEnabled(false);
        // 标题 = FXGL 主菜单，X 菜单 = FXGL 游戏菜单（菜单键由 FXGL 在对局与游戏菜单之间切换）
        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);
        settings.setMenuKey(KeyCode.X);
        settings.setSceneFactory(new MotaSceneFactory(scenes));
        settings.setCloseConfirmation(false);
        settings.setUserProfileEnabled(false);

        // 打包字体（assets/ui/fonts/）：界面经 ui.Fonts 取用，不再依赖系统里的黑体 / Consolas
        settings.setFontUI("NotoSansSC-Regular.ttf");
        settings.setFontGame("NotoSansSC-Bold.ttf");
        settings.setFontText("NotoSansSC-Regular.ttf");
        settings.setFontMono("Inconsolata-ExtraBold.ttf");
        // FXGL 自带的样式（开发者面板、对话框）+ 本作的样式（assets/ui/css/）
        settings.setCSSList(List.of("fxgl_dark.css", "mota.css"));
        settings.setAppIcon("ui/icon.png");

        // 按此顺序 onInit：资产服务要按内容表预载图片，会话服务要用内容与存档库
        settings.addEngineService(ContentService.class);
        settings.addEngineService(AssetService.class);
        settings.addEngineService(SessionService.class);

        boolean dev = DevToolsService.isDevMode();
        settings.setApplicationMode(dev ? ApplicationMode.DEVELOPER : ApplicationMode.RELEASE);
        settings.setDeveloperMenuEnabled(dev);
        settings.setProfilingEnabled(DevToolsService.isProfiling());
        // 发行版不让 FXGL 在运行目录写 system / logs；存档走自己的 SaveRepository，不受影响
        settings.setFileSystemWriteAllowed(dev);
        if (dev) {
            settings.addEngineService(DevToolsService.class);
        }
    }

    /** 主循环启动前调用一次（FX 线程，全部服务已就绪）：装配界面、注册对局按键 */
    @Override
    protected void initInput() {
        scenes.init();
        scenes.gameplayInput().register(FXGL.getGameScene().getInput());

        // 失焦后收不到松键事件：把「按住」的状态清掉（否则按住方向键切出窗口，人会一直走）。
        // FXGL 不处理失焦；先清修饰键再 clearAll()，免得补发的松开被当成单按 D
        FXGL.getPrimaryStage().focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                scenes.resetHeld();
                FXGL.getSceneService().getCurrentScene().getInput().clearAll();
            }
        });
    }

    /** 加载场景的后台线程：装上准备好的这一局（只做不会失败的事） */
    @Override
    protected void initGame() {
        FXGL.getService(SessionService.class).installPending();
    }

    /** 加载场景的后台线程：游戏场景尚未显示，挂上对局画面、生成地图实体 */
    @Override
    protected void initUI() {
        // 界面层不设底色（地图要透上来），底色设在场景背景上
        FXGL.getGameScene().setBackgroundColor(Color.web("#2a2a2a"));
        scenes.game().onNewGame();
    }

    /** 只在对局场景活动时每帧调用 */
    @Override
    protected void onUpdate(double tpf) {
        scenes.game().onFrame(tpf);
    }

    static void main(String[] args) {
        launch(MotaApplication.class, args);
    }
}
