package com.demo.mota.ui.screen.equipment;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.ChordKey;
import com.demo.mota.ui.screen.InGameScreen;
import com.demo.mota.ui.screen.PresetHotkeys;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

import java.util.Map;

/**
 * 装备界面（{@link GamePhase#EQUIPMENT}，对局中按 Q）：{@link EquipmentState} + {@link EquipmentRenderer}。
 * 穿脱造成的伤害变化由对局界面在回到游戏时统一重算。按住 W 再按数字键把当前穿戴保存为套装。
 */
public class EquipmentScreen extends InGameScreen {

    /** 保存套装：按住 W 再按数字键（与对局中切换套装同一按法）；W 因此不作为「上」的别名 */
    private final ChordKey saveChord = new ChordKey(PresetHotkeys.EQUIPMENT_SET_CHORD);
    private final Map<KeyCode, Integer> setHotkeys = PresetHotkeys.equipmentSetsFromRules();
    private final GameEngine engine;
    private final EquipmentRenderer renderer;
    private EquipmentState state;

    public EquipmentScreen(GameEngine engine, GameFlow flow, Canvas canvas, EquipmentRenderer renderer) {
        super(flow, canvas);
        this.engine = engine;
        this.renderer = renderer;
    }

    @Override
    public void onNewGame() {
        this.state = new EquipmentState(engine.getPlayerStateManager());
    }

    @Override
    public void onEnter() {
        saveChord.reset();
        state.open();
    }

    @Override
    protected GamePhase handleScreenKey(KeyCode code) {
        if (code == saveChord.key()) {
            if (saveChord.press(code)) {
                state.armSave();
            }
            return null;
        }
        Integer set = setHotkeys.get(code);
        if (set != null) {
            // 数字键只在按住 W 时有意义
            if (saveChord.isHeld()) {
                saveChord.markUsed();
                state.saveSet(set);
            }
            return null;
        }
        state.handle(toCommand(code));
        return state.isOpen() ? null : GamePhase.PLAYING;
    }

    @Override
    protected boolean handleScreenKeyRelease(KeyCode code) {
        if (code != saveChord.key()) return false;
        saveChord.release(code);
        state.cancelSave();
        return true;
    }

    @Override
    protected void render(GraphicsContext gc, double width, double height) {
        renderer.render(gc, state, width, height);
    }

    private static MenuCommand toCommand(KeyCode code) {
        return switch (code) {
            case UP -> MenuCommand.UP;
            case DOWN, S -> MenuCommand.DOWN;
            case LEFT, A -> MenuCommand.LEFT;
            case RIGHT, D -> MenuCommand.RIGHT;
            case ENTER, SPACE -> MenuCommand.CONFIRM;
            case ESCAPE -> MenuCommand.BACK;
            default -> null;
        };
    }
}
