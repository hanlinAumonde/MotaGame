package com.demo.mota.ui.screen;

import javafx.scene.input.KeyCode;

/**
 * 组合快捷键里的「修饰键」：按住它再按数字键（D+数字 切技能组、W+数字 切 / 存套装）。
 *
 * <p>靠按下 / 松开两个事件判断「是否按住」，并记下按住期间有没有组合过别的键——
 * 这样同一个键既能当修饰键，又能保留「单按」的含义（对局中单按 D 仍是打开技能设置，在松开时触发）。
 * 按住时键盘的自动重复只会反复送来按下事件，不影响判断。
 */
public final class ChordKey {

    private final KeyCode key;
    private boolean held;
    private boolean used;

    public ChordKey(KeyCode key) {
        this.key = key;
    }

    public KeyCode key() {
        return key;
    }

    /**
     * 按下事件。
     *
     * @return 这次是不是「刚按下」（是本键且此前未按住；自动重复的按下返回 false）
     */
    public boolean press(KeyCode code) {
        if (code != key || held) return false;
        held = true;
        used = false;
        return true;
    }

    public boolean isHeld() {
        return held;
    }

    /** 按住期间组合了一个键：松开时就不再算「单按」 */
    public void markUsed() {
        used = true;
    }

    /**
     * 松开事件。
     *
     * @return 松开的是本键，且按住期间没有组合任何键（即「单按」了一下）
     */
    public boolean release(KeyCode code) {
        if (code != key) return false;
        boolean tapped = held && !used;
        held = false;
        return tapped;
    }

    /** 切换界面时调用：松开事件可能落在别的界面里，不能让按住状态残留 */
    public void reset() {
        held = false;
        used = false;
    }
}
