package com.demo.mota.ui.overlay;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.menu.GameMenu;
import com.demo.mota.engine.menu.MenuCommand;
import com.demo.mota.ui.MenuRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

/**
 * 游戏菜单（按 X）：{@link GameMenu} + {@link MenuRenderer}。菜单状态每局重建，因此每次都从引擎现取。
 */
public class GameMenuOverlay implements Overlay {

    private final GameEngine engine;
    private final MenuRenderer renderer;

    public GameMenuOverlay(GameEngine engine, MenuRenderer renderer) {
        this.engine = engine;
        this.renderer = renderer;
    }

    @Override
    public KeyCode toggleKey() {
        return KeyCode.X;
    }

    @Override
    public boolean isOpen() {
        return engine.getGameMenu().isOpen();
    }

    @Override
    public void open() {
        engine.getGameMenu().open();
    }

    @Override
    public void close() {
        engine.getGameMenu().close();
    }

    @Override
    public boolean handleKey(KeyCode code) {
        engine.getGameMenu().handle(toCommand(code));
        return false;
    }

    @Override
    public void render(GraphicsContext gc, double width, double height) {
        renderer.render(gc, engine.getGameMenu(), engine.getPlayerStateManager(), width, height);
    }

    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP, W -> MenuCommand.UP;
            case DOWN, S -> MenuCommand.DOWN;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            case ESCAPE -> MenuCommand.BACK;
            default -> null;
        };
    }
}
