package com.demo.mota.ui.screen.skill;

import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.app.GameFlow;
import com.demo.mota.engine.app.GamePhase;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.screen.InGameScreen;
import com.demo.mota.ui.screen.PresetHotkeys;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;

import java.util.Map;

/**
 * 技能设置（{@link GamePhase#SKILL_SETUP}，对局中按 D）：{@link SkillSetupState} + {@link SkillSetupRenderer}。
 *
 * <p>数字键（规则 {@code skill.hotkeys}）切换正在编辑的预设并激活，不做停用。
 * 预设改动造成的伤害变化由对局界面在回到游戏时统一重算。
 */
public class SkillSetupScreen extends InGameScreen {

    private final GameEngine engine;
    private final SkillSetupRenderer renderer;
    private final Map<KeyCode, Integer> presetHotkeys = PresetHotkeys.fromRules();
    private SkillSetupState state;

    public SkillSetupScreen(GameEngine engine, GameFlow flow, Canvas canvas, SkillSetupRenderer renderer) {
        super(flow, canvas);
        this.engine = engine;
        this.renderer = renderer;
    }

    @Override
    public void onNewGame() {
        this.state = new SkillSetupState(engine.getPlayerStateManager());
    }

    @Override
    public void onEnter() {
        state.open();
    }

    @Override
    protected GamePhase handleScreenKey(KeyCode code) {
        Integer preset = presetHotkeys.get(code);
        if (preset != null) {
            state.selectPreset(preset);
        } else {
            state.handle(toCommand(code));
        }
        return state.isOpen() ? null : GamePhase.PLAYING;
    }

    @Override
    protected void render(GraphicsContext gc, double width, double height) {
        renderer.render(gc, state, width, height);
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
