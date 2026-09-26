package com.demo.mota.ui.overlay;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.menu.EquipmentMenu;
import com.demo.mota.engine.menu.MenuCommand;
import com.demo.mota.ui.EquipmentRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

/**
 * 装备设置（按 Q）：{@link EquipmentMenu} + {@link EquipmentRenderer}。
 */
public class EquipmentOverlay implements Overlay {

    private final GameEngine engine;
    private final EquipmentRenderer renderer;

    public EquipmentOverlay(GameEngine engine, EquipmentRenderer renderer) {
        this.engine = engine;
        this.renderer = renderer;
    }

    @Override
    public KeyCode toggleKey() {
        return KeyCode.Q;
    }

    @Override
    public boolean isOpen() {
        return engine.getEquipmentMenu().isOpen();
    }

    @Override
    public void open() {
        engine.getEquipmentMenu().open();
    }

    @Override
    public void close() {
        engine.getEquipmentMenu().close();
    }

    @Override
    public boolean handleKey(KeyCode code) {
        return engine.getEquipmentMenu().handle(toCommand(code));
    }

    @Override
    public void render(GraphicsContext gc, double width, double height) {
        renderer.render(gc, engine.getEquipmentMenu(), width, height);
    }

    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP, W -> MenuCommand.UP;
            case DOWN, S -> MenuCommand.DOWN;
            case LEFT, A -> MenuCommand.LEFT;
            case RIGHT, D -> MenuCommand.RIGHT;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            case ESCAPE -> MenuCommand.BACK;
            default -> null;
        };
    }
}
