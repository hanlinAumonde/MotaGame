package com.demo.mota.app.services;

import com.almasb.fxgl.core.EngineService;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.logging.Logger;
import javafx.scene.input.KeyCode;

/**
 * 开发模式（{@code -Dmota.dev=true}）下对 FXGL 开发者工具的调整，只在开发模式注册；
 * 性能监视另由 {@code -Dmota.profile=true} 开启。
 *
 * <p>FXGL 把开发者面板、控制台绑在<b>数字键 1 / 2</b>（窗口级输入，任何场景都生效），
 * 与「按住 D / W + 数字键」冲突。这些系统动作是在 FXGL 自己的 {@code onMainLoopStarting} 里绑定的，
 * 而自定义服务排在 FXGL 内置服务之后，轮到这里时它们已经绑好，改绑到 F1 / F2 即可。
 */
public final class DevToolsService extends EngineService {

    public static final String PROPERTY = "mota.dev";
    /** FXGL 的性能监视（FPS / CPU / 内存曲线）单独开关：它盖在画面左上角，会挡住状态栏和开发者面板 */
    public static final String PROFILE_PROPERTY = "mota.profile";

    private static final Logger log = Logger.get(DevToolsService.class);

    public static boolean isDevMode() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static boolean isProfiling() {
        return Boolean.getBoolean(PROFILE_PROPERTY);
    }

    @Override
    public void onMainLoopStarting() {
        Input input = FXGL.getSceneService().getInput();
        rebind(input, "Dev Options", KeyCode.F1);
        rebind(input, "Dev Console", KeyCode.F2);
    }

    private static void rebind(Input input, String actionName, KeyCode key) {
        try {
            if (!input.rebind(input.getActionByName(actionName), key)) {
                log.warning("开发者快捷键 " + actionName + " 改绑到 " + key + " 失败（该键已被占用）");
            }
        } catch (IllegalArgumentException e) {
            log.warning("找不到开发者快捷键 " + actionName + "（未开启开发者菜单？）");
        }
    }
}
