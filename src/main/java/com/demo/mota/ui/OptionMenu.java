package com.demo.mota.ui;

import com.demo.mota.engine.menu.MenuCommand;

import java.util.List;

/**
 * 「一列选项 + 上下选择 + 确认」的通用状态机，标题界面与游戏结束提示框共用。
 *
 * <p>与 {@code GameMenu} 一样只管状态、不碰绘制；输入同样走 {@link MenuCommand} 语义，
 * 按键到语义的翻译留在各 Screen 里。
 *
 * <p><b>禁用项会被跳过</b>而不是选中后无响应：存档功能落地前，「继续游戏」「读档」
 * 这类占位按钮照常画出来（灰色），但光标不会停在上面，玩家一眼就知道现在用不了。
 */
public class OptionMenu {

    /**
     * 一个选项。
     *
     * @param id      供 Screen 判断玩家选了什么，与展示文本解耦
     * @param label   展示文本
     * @param enabled 是否可用；false 时画灰且不可选中
     */
    public record Option(String id, String label, boolean enabled) {
        public static Option of(String id, String label) {
            return new Option(id, label, true);
        }

        public static Option disabled(String id, String label) {
            return new Option(id, label, false);
        }
    }

    private final List<Option> options;
    private int selectedIndex;

    public OptionMenu(List<Option> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("OptionMenu requires at least one option");
        }
        this.options = List.copyOf(options);
        this.selectedIndex = firstEnabledIndex();
    }

    public List<Option> getOptions() {
        return options;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public Option getSelected() {
        return options.get(selectedIndex);
    }

    /** 回到第一个可用项，供重新进入界面时重置 */
    public void reset() {
        this.selectedIndex = firstEnabledIndex();
    }

    /**
     * 处理一次操作。
     *
     * @return {@code CONFIRM} 且选中项可用时返回该项 id，否则返回 {@code null}
     */
    public String handle(MenuCommand command) {
        if (command == null) return null;
        switch (command) {
            case UP -> move(-1);
            case DOWN -> move(1);
            case CONFIRM -> {
                Option selected = getSelected();
                return selected.enabled() ? selected.id() : null;
            }
            case BACK, CLOSE -> { /* 由各 Screen 自行决定是否有「返回」语义 */ }
        }
        return null;
    }

    /** 循环移动光标并跳过禁用项；全部禁用时原地不动 */
    private void move(int delta) {
        int size = options.size();
        for (int step = 1; step <= size; step++) {
            int candidate = Math.floorMod(selectedIndex + delta * step, size);
            if (options.get(candidate).enabled()) {
                selectedIndex = candidate;
                return;
            }
        }
    }

    /** 没有任何可用项时退回 0，保证 {@link #getSelected()} 永远有值 */
    private int firstEnabledIndex() {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).enabled()) return i;
        }
        return 0;
    }
}
