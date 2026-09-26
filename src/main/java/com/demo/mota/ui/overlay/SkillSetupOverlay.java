package com.demo.mota.ui.overlay;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.menu.MenuCommand;
import com.demo.mota.engine.menu.SkillSetupMenu;
import com.demo.mota.ui.SkillSetupRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

/**
 * 技能设置（按 D）：{@link SkillSetupMenu} + {@link SkillSetupRenderer}。
 */
public class SkillSetupOverlay implements Overlay {

    private final GameEngine engine;
    private final SkillSetupRenderer renderer;

    public SkillSetupOverlay(GameEngine engine, SkillSetupRenderer renderer) {
        this.engine = engine;
        this.renderer = renderer;
    }

    @Override
    public KeyCode toggleKey() {
        return KeyCode.D;
    }

    @Override
    public boolean isOpen() {
        return engine.getSkillSetupMenu().isOpen();
    }

    @Override
    public void open() {
        engine.getSkillSetupMenu().open();
    }

    @Override
    public void close() {
        engine.getSkillSetupMenu().close();
    }

    @Override
    public boolean handleKey(KeyCode code) {
        return engine.getSkillSetupMenu().handle(toCommand(code));
    }

    @Override
    public void render(GraphicsContext gc, double width, double height) {
        renderer.render(gc, engine.getSkillSetupMenu(), width, height);
    }

    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP, W -> MenuCommand.UP;
            case DOWN, S -> MenuCommand.DOWN;
            case LEFT, A -> MenuCommand.LEFT;
            case RIGHT -> MenuCommand.RIGHT;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            case DELETE, BACK_SPACE -> MenuCommand.CLEAR;
            case ESCAPE -> MenuCommand.BACK;
            default -> null;
        };
    }
}
