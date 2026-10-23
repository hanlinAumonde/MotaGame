package com.demo.mota.app.input;

import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.KeyTrigger;
import com.almasb.fxgl.input.TriggerListener;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.logging.Logger;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.rules.GameRules;
import com.demo.mota.ui.screen.ChordKey;
import com.demo.mota.ui.screen.PresetHotkeys;
import javafx.scene.input.KeyCode;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 对局的按键：在游戏场景的 {@link Input} 上注册 FXGL 动作（{@link UserAction}），翻译成对局命令（{@link Commands}）。
 *
 * <table>
 *   <caption>对局按键</caption>
 *   <tr><th>动作</th><th>默认键</th><th>按下</th><th>松开</th></tr>
 *   <tr><td>移动</td><td>↑ ↓ ← →</td><td>主角空闲则走一步，否则缓存为下一步</td><td>从「按住」集合移除</td></tr>
 *   <tr><td>转身 / 撤销</td><td>Z / Backspace</td><td>原地转向 / 撤销最近一个撤销点</td><td></td></tr>
 *   <tr><td>装备 / 套装</td><td>Q / A</td><td>叠上对应界面</td><td></td></tr>
 *   <tr><td>修饰键</td><td>D / W</td><td>记为按住</td><td>单按 D → 技能设置</td></tr>
 *   <tr><td>数字键</td><td>规则 {@code skill.hotkeys} / {@code equipment.setHotkeys}</td>
 *       <td>按住 D → 切换技能组；按住 W → 换上套装</td><td></td></tr>
 * </table>
 * 游戏菜单（X）是 FXGL 的菜单键，不在这里。
 *
 * <ul>
 *   <li>FXGL 的动作只在<b>真正按下</b>时触发一次，按住期间系统的自动重复不会再触发（源码：{@code Input.handlePressed}），
 *       连续行走靠「仍按着的方向」接续，见 {@code StepBuffer}；</li>
 *   <li>一个触发键只能绑一个动作：技能组与套装共用数字键，所以每个数字键是一个动作，按下时按哪个修饰键正按着分派；</li>
 *   <li>「按住 D 期间按了别的键」由一个 {@link TriggerListener} 统一判定——它能看到这个场景收到的全部按键，
 *       包括 FXGL 自己处理的菜单键 X，以及没有绑定动作的键；</li>
 *   <li><b>陷阱</b>：FXGL 在叠上子场景、或失焦后调用 {@code clearAll()} 时，会给仍按着的动作补发 {@code onActionEnd}，
 *       「只按着 D」会被误判为单按而弹出技能设置。所以离开对局之前（{@code Navigator}）、失焦时都要先 {@link #resetChords()}。</li>
 * </ul>
 * 单元测试见 {@code GameplayInputTest}（用 FXGL 的 {@code mockKeyPress} 直接驱动）。
 */
public final class GameplayInput {

    /** 按键翻译成的对局命令（由对局画面实现） */
    public interface Commands {
        void pressDirection(Direction direction);

        void releaseDirection(Direction direction);

        void turn();

        void undoLastAction();

        void openEquipment();

        void openEquipmentSets();

        void openSkillSetup();

        /** 按住 D 再按数字键：激活 / 停用第 {@code index} 套技能组 */
        void togglePreset(int index);

        /** 按住 W 再按数字键：换上第 {@code index} 套套装 */
        void applyEquipmentSet(int index);
    }

    private static final Logger log = Logger.get(GameplayInput.class);

    private final Commands commands;
    /** 按住 D 再按数字键：激活 / 停用技能组；单按 D：打开技能设置 */
    private final ChordKey presetChord = new ChordKey(PresetHotkeys.SKILL_CHORD);
    /** 按住 W 再按数字键：换上对应套装 */
    private final ChordKey setChord = new ChordKey(PresetHotkeys.EQUIPMENT_SET_CHORD);
    /** 数字键 → 预设下标，取自塔规则 {@code skill.hotkeys} */
    private final Map<KeyCode, Integer> presetHotkeys;
    /** 数字键 → 套装下标，取自塔规则 {@code equipment.setHotkeys} */
    private final Map<KeyCode, Integer> setHotkeys;

    public GameplayInput(Commands commands, GameRules rules) {
        this.commands = commands;
        this.presetHotkeys = PresetHotkeys.fromRules(rules);
        this.setHotkeys = PresetHotkeys.equipmentSetsFromRules(rules);
    }

    /** 注册到游戏场景的输入上（一次）：游戏场景的输入跨局保留，{@code startNewGame()} 不清动作 */
    public void register(Input input) {
        direction(input, "移动·上", KeyCode.UP, Direction.UP);
        direction(input, "移动·下", KeyCode.DOWN, Direction.DOWN);
        direction(input, "移动·左", KeyCode.LEFT, Direction.LEFT);
        direction(input, "移动·右", KeyCode.RIGHT, Direction.RIGHT);

        onPress(input, "转身", KeyCode.Z, commands::turn);
        onPress(input, "撤销", KeyCode.BACK_SPACE, commands::undoLastAction);
        onPress(input, "装备", KeyCode.Q, commands::openEquipment);
        onPress(input, "套装", KeyCode.A, commands::openEquipmentSets);

        add(input, new UserAction("技能组修饰键") {
            @Override
            protected void onActionBegin() {
                presetChord.press(presetChord.key());
            }

            @Override
            protected void onActionEnd() {
                if (presetChord.release(presetChord.key())) {
                    commands.openSkillSetup();
                }
            }
        }, presetChord.key());
        add(input, new UserAction("套装修饰键") {
            @Override
            protected void onActionBegin() {
                setChord.press(setChord.key());
            }

            @Override
            protected void onActionEnd() {
                setChord.release(setChord.key());
            }
        }, setChord.key());

        Set<KeyCode> digits = new TreeSet<>(presetHotkeys.keySet());
        digits.addAll(setHotkeys.keySet());
        for (KeyCode digit : digits) {
            onPress(input, "组合键·" + digit.getName(), digit, () -> chordDigit(digit));
        }

        input.addTriggerListener(new TriggerListener() {
            @Override
            protected void onKeyBegin(KeyTrigger trigger) {
                KeyCode key = trigger.getKey();
                if (key != presetChord.key() && key != setChord.key()) {
                    // 按住 D 期间按了任何别的键（数字、方向、X……），松开 D 时都不再算「单按」
                    presetChord.markUsed();
                    setChord.markUsed();
                }
            }
        });
    }

    /** 清掉修饰键的按住状态，见类注释的「陷阱」 */
    public void resetChords() {
        presetChord.reset();
        setChord.reset();
    }

    /** 数字键：按住 D 时切换技能组，按住 W 时换上套装，都没按住时不做任何事 */
    private void chordDigit(KeyCode digit) {
        Integer preset = presetHotkeys.get(digit);
        Integer set = setHotkeys.get(digit);
        if (presetChord.isHeld() && preset != null) {
            commands.togglePreset(preset);
        } else if (setChord.isHeld() && set != null) {
            commands.applyEquipmentSet(set);
        }
    }

    private void direction(Input input, String name, KeyCode key, Direction direction) {
        add(input, new UserAction(name) {
            @Override
            protected void onActionBegin() {
                commands.pressDirection(direction);
            }

            @Override
            protected void onActionEnd() {
                commands.releaseDirection(direction);
            }
        }, key);
    }

    private static void onPress(Input input, String name, KeyCode key, Runnable action) {
        add(input, new UserAction(name) {
            @Override
            protected void onActionBegin() {
                action.run();
            }
        }, key);
    }

    /** 规则里配的数字键若与其他动作撞键，忽略这一个并告警，不影响其余按键 */
    private static void add(Input input, UserAction action, KeyCode key) {
        try {
            input.addAction(action, key);
        } catch (IllegalArgumentException e) {
            log.warning("按键 " + key + " 无法绑定到「" + action.getName() + "」：" + e.getMessage());
        }
    }
}
