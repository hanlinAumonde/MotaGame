package com.demo.mota.scene.equipment;

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
 * 装备界面（对局中按 Q）：{@link EquipmentState} + {@link EquipmentView}。
 * 穿脱造成的伤害变化由对局界面在回到游戏时统一重算。按住 W 再按数字键把当前穿戴保存为套装。
 */
public class EquipmentSubScene extends InGameSubScene {

    /** 保存套装：按住 W 再按数字键（与对局中切换套装同一按法）；W 因此不作为「上」的别名 */
    private final ChordKey saveChord = new ChordKey(PresetHotkeys.EQUIPMENT_SET_CHORD);
    /** 数字键 → 套装下标（塔规则 {@code equipment.setHotkeys}） */
    private final Map<KeyCode, Integer> setHotkeys;
    private final SessionService sessions;
    private final EquipmentView view;
    private EquipmentState state;

    public EquipmentSubScene(Navigator navigator, Runnable onReturnToGame, SessionService sessions,
                             EquipmentView view) {
        super(navigator, onReturnToGame);
        this.sessions = sessions;
        this.view = view;
        this.setHotkeys = PresetHotkeys.equipmentSetsFromRules(sessions.content().rules());
    }

    @Override
    protected void onOpen() {
        saveChord.reset();
        state = new EquipmentState(sessions.session().player());
        state.open();
    }

    @Override
    protected boolean handleScreenKey(KeyCode code) {
        if (code == saveChord.key()) {
            if (saveChord.press(code)) {
                state.armSave();
            }
            return false;
        }
        Integer set = setHotkeys.get(code);
        if (set != null) {
            // 数字键只在按住 W 时有意义
            if (saveChord.isHeld()) {
                saveChord.markUsed();
                state.saveSet(set);
            }
            return false;
        }
        state.handle(toCommand(code));
        return !state.isOpen();
    }

    @Override
    protected boolean handleScreenKeyRelease(KeyCode code) {
        if (code != saveChord.key()) return false;
        saveChord.release(code);
        state.cancelSave();
        return true;
    }

    @Override
    protected void draw(Sketch sketch) {
        view.draw(sketch, state);
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
