package com.demo.mota.app;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.LoadingScene;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.app.scene.StartupScene;
import com.demo.mota.ui.screen.gamemenu.GameMenuScene;
import com.demo.mota.ui.screen.loading.MotaLoadingScene;
import com.demo.mota.ui.screen.loading.MotaStartupScene;
import com.demo.mota.ui.screen.title.TitleMenu;

/**
 * 交给 FXGL 的场景工厂：启动场景（内容加载进度）、加载场景（每局装配）、标题（主菜单）、游戏菜单（X）。
 *
 * <p>调用时机都很早（源码）：启动场景在任何服务构造之前；加载场景与两个菜单在 FXGL 内置服务初始化时、
 * 我们的内容载入之前。所以这里只构造，不碰内容；菜单经 {@link MotaScenes} 在进入时再取。
 */
public final class MotaSceneFactory extends SceneFactory {

    private final MotaScenes scenes;

    public MotaSceneFactory(MotaScenes scenes) {
        this.scenes = scenes;
    }

    @Override
    public StartupScene newStartup(int width, int height) {
        return new MotaStartupScene(width, height);
    }

    @Override
    public LoadingScene newLoadingScene() {
        return new MotaLoadingScene();
    }

    @Override
    public FXGLMenu newMainMenu() {
        return new TitleMenu(scenes);
    }

    @Override
    public FXGLMenu newGameMenu() {
        return new GameMenuScene(scenes);
    }
}
