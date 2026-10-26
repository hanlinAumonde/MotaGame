package com.demo.mota.scene.skill;

import com.demo.mota.app.input.ChordKey;
import com.demo.mota.app.input.PresetHotkeys;
import com.demo.mota.app.nav.Navigator;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.scene.InGameSubScene;
import com.demo.mota.ui.MenuCommand;
import com.demo.mota.ui.kit.Sketch;
import javafx.scene.input.KeyCode;

import java.util.Map;

/**
 * 技能设置（对局中单按 D）：{@link SkillSetupState} + {@link SkillSetupView}。
 *
 * <p>按住 D 再按数字键（规则 {@code skill.hotkeys}）切换正在编辑的预设并激活，不做停用；与对局中同一按法。
 * 预设改动造成的伤害变化由对局界面在回到游戏时统一重算。
 */
public class SkillSetupSubScene extends InGameSubScene {

    private final SessionService sessions;
    private final SkillSetupView view;
    private final ChordKey presetChord = new ChordKey(PresetHotkeys.SKILL_CHORD);
    /** 数字键 → 预设下标（塔规则 {@code skill.hotkeys}） */
    private final Map<KeyCode, Integer> presetHotkeys;
    private SkillSetupState state;

    public SkillSetupSubScene(Navigator navigator, Runnable onReturnToGame, SessionService sessions,
                              SkillSetupView view) {
        super(navigator, onReturnToGame);
        this.sessions = sessions;
        this.view = view;
        this.presetHotkeys = PresetHotkeys.fromRules(sessions.content().rules());
        view.setHotkeys(sessions.content().rules().skill().hotkeys());
    }

    @Override
    protected void onOpen() {
        presetChord.reset();
        state = new SkillSetupState(sessions.session().player());
        state.open();
    }

    @Override
    protected boolean handleScreenKey(KeyCode code) {
        if (code == presetChord.key()) {
            presetChord.press(code);
            return false;
        }
        Integer preset = presetHotkeys.get(code);
        if (preset != null) {
            // 与对局中相同：按住 D 再按数字键才切换
            if (presetChord.isHeld()) {
                presetChord.markUsed();
                state.selectPreset(preset);
            }
            return false;
        }
        state.handle(toCommand(code));
        return !state.isOpen();
    }

    @Override
    protected boolean handleScreenKeyRelease(KeyCode code) {
        presetChord.release(code);
        return false;
    }

    @Override
    protected void draw(Sketch sketch) {
        view.draw(sketch, state);
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
