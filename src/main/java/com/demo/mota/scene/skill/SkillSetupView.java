package com.demo.mota.scene.skill;

import com.demo.mota.app.services.AssetService;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.book.OwnedSkill;
import com.demo.mota.engine.skill.book.SkillBook;
import com.demo.mota.engine.skill.preset.SkillPreset;
import com.demo.mota.engine.skill.preset.SkillPresetBook;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.MotaLayout;
import com.demo.mota.ui.kit.Icons;
import com.demo.mota.ui.kit.Palette;
import com.demo.mota.ui.kit.Sketch;
import com.demo.mota.ui.kit.SkillCards;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能设置界面（按 D），只读 {@link SkillSetupState}。
 *
 * <p>上半屏：玩家名 + 头像、可滚动的技能卡片列表；
 * 下半屏：当前激活（即正在编辑）的那套预设——标题旁一排快捷键小方块标出是第几套，
 * 下面一行回合格（第 0 格为「战前」），空格显示「普攻」。
 */
public class SkillSetupView {

    private static final double WIDTH = MotaLayout.WIDTH;
    private static final double HEIGHT = MotaLayout.HEIGHT;
    private static final double PADDING = 12;
    private static final double COMBO_HEIGHT = 196;
    private static final Color ARMED_COLOR = Color.web("#7cfc7c");
    private static final Color SLOT_TEXT_COLOR = Color.web("#e8ecff");

    private final AssetService assets;
    private final Icons icons;
    private final SkillCards cards;
    /** 各套预设的快捷键（KeyCode 名），取自塔规则，只用来显示 */
    private List<String> hotkeys = List.of();

    public SkillSetupView(AssetService assets) {
        this.assets = assets;
        this.icons = new Icons(assets);
        this.cards = new SkillCards(icons);
    }

    public void setHotkeys(List<String> hotkeys) {
        this.hotkeys = List.copyOf(hotkeys);
    }

    public void draw(Sketch s, SkillSetupState menu) {
        s.screen(WIDTH, HEIGHT);
        double comboY = HEIGHT - PADDING - COMBO_HEIGHT;
        drawSkillList(s, menu, comboY - PADDING);
        drawCombo(s, menu, PADDING, comboY, WIDTH - 2 * PADDING, COMBO_HEIGHT);
    }

    // ==================== 技能列表 ====================

    private void drawSkillList(Sketch s, SkillSetupState menu, double bottom) {
        s.panel(PADDING, PADDING, WIDTH - 2 * PADDING, bottom - PADDING, false);
        PlayerStateManager player = menu.getPlayer();

        Font nameFont = Fonts.bold(34);
        String name = player.getCharacterName() == null ? "勇者" : player.getCharacterName();
        double nameWidth = s.width(name, nameFont);
        s.centered(name, nameFont, Palette.TITLE, WIDTH / 2, 58);
        Image avatar = assets.idleFrame(AssetService.PLAYER, Direction.DOWN);
        if (avatar != null) {
            s.image(avatar, WIDTH / 2 + nameWidth / 2 + 14, 22, 44, 44);
        }
        s.text("技能列表：", Fonts.bold(22), Palette.SECTION, 40, 100);

        List<Skill> skills = menu.getSkills();
        double cardX = 40;
        double cardW = WIDTH - 80;
        double top = 116;
        double areaBottom = bottom - 12;
        if (skills.isEmpty()) {
            s.text("尚未掌握任何技能", Fonts.bold(24), Palette.DISABLED, cardX + 20, top + 50);
            return;
        }

        List<List<String>> layouts = new ArrayList<>();
        List<Double> heights = new ArrayList<>();
        for (Skill skill : skills) {
            List<String> lines = cards.layout(skill, cardW);
            layouts.add(lines);
            heights.add(cards.height(lines));
        }

        // 选中项越过可视区底部时才向下滚，保证选中卡片始终完整可见
        int selected = menu.getSelectedSkillIndex();
        int first = 0;
        while (first < selected && spanHeight(heights, first, selected) > areaBottom - top) {
            first++;
        }

        SkillBook book = player.getSkillBook();
        double y = top;
        int last = first;
        for (int i = first; i < skills.size(); i++) {
            double h = heights.get(i);
            if (y + h > areaBottom) break;
            Skill skill = skills.get(i);
            OwnedSkill owned = book.get(skill.skillId());
            boolean fromEquipment = owned != null && owned.isFromEquipment();
            boolean dimmed = owned != null && skill.isPassive() && !owned.isEnabled();
            cards.card(s, skill, layouts.get(i), cardX, y, cardW, h,
                    i == selected, cardTags(skill, fromEquipment), dimmed);
            y += h + 10;
            last = i + 1;
        }

        if (first > 0) {
            s.centered("▲", Fonts.bold(18), Palette.HINT, WIDTH / 2, top - 2);
        }
        if (last < skills.size()) {
            s.centered("▼", Fonts.bold(18), Palette.HINT, WIDTH / 2, bottom - 2);
        }
    }

    /** 卡片标题后的标记：主动技能标每场可释放次数，装备附带的标 [装备] */
    private static String cardTags(Skill skill, boolean fromEquipment) {
        StringBuilder tags = new StringBuilder();
        if (skill.isActive()) {
            tags.append(skill.hasCastLimit() ? "[每战 " + skill.maxCasts() + " 次]" : "[每战不限次]");
        }
        if (fromEquipment) {
            tags.append(tags.isEmpty() ? "" : " ").append("[装备]");
        }
        return tags.toString();
    }

    /** 从 first 到 last（含）的卡片连同间距的总高度 */
    private static double spanHeight(List<Double> heights, int first, int last) {
        double total = 0;
        for (int i = first; i <= last; i++) {
            total += heights.get(i) + 10;
        }
        return total;
    }

    // ==================== 技能组合 ====================

    private void drawCombo(Sketch s, SkillSetupState menu, double x, double y, double w, double h) {
        s.panel(x, y, w, h, true);
        PlayerStateManager player = menu.getPlayer();
        SkillPreset preset = menu.getEditingPreset();

        Font titleFont = Fonts.bold(24);
        String title = "技能组合 · " + preset.getName();
        s.text(title, titleFont, Palette.SECTION, x + 20, y + 36);
        boolean active = menu.isEditingPresetActive();
        double tabsX = x + 44 + s.width(title, titleFont);
        drawPresetTabs(s, player.getPresetBook(), menu.getEditingPresetIndex(), active, tabsX, y + 14);
        if (!active) {
            // 游戏中停用了技能组：只在这种情况下提示，放入技能即重新激活
            s.text("当前未激活（全程普攻）", Fonts.bold(18), Palette.DISABLED,
                    tabsX + player.getPresetBook().presetCount() * 36 + 12, y + 36);
        }
        if (!menu.getNotice().isEmpty()) {
            s.right(menu.getNotice(), Fonts.bold(18), Palette.NOTICE, x + w - 20, y + 36);
        } else if (!player.isPresetEditable()) {
            s.right("本塔的技能组合由规则固定", Fonts.bold(18), Palette.HINT, x + w - 20, y + 36);
        }

        int slots = preset.slotCount();
        double gap = 8;
        double slotW = (w - 40 - gap * (slots - 1)) / slots;
        double slotH = 72;
        double slotY = y + 72;
        SkillBook book = player.getSkillBook();

        for (int round = 0; round < slots; round++) {
            double sx = x + 20 + round * (slotW + gap);
            s.centered(round == 0 ? "战前" : "第" + round + "回", Fonts.regular(15), Palette.HINT,
                    sx + slotW / 2, slotY - 8);
            if (round == menu.getSelectedRound()) {
                s.selection(sx - 3, slotY - 3, slotW + 6, slotH + 6, true);
            }
            drawSlot(s, book, preset.getSlot(round), round, sx, slotY, slotW, slotH);
        }

        String hint = player.isPresetEditable()
                ? "↑↓ 选技能   ←→ 选回合   Enter 放入   Delete 卸下   D+数字键 切换技能组   X 返回游戏"
                : "↑↓ 查看技能   ←→ 查看回合   D+数字键 切换技能组   X 返回游戏";
        s.text(hint, Fonts.regular(16), Palette.HINT, x + 20, y + h - 18);
    }

    /**
     * 一排小方块，每块标着该套预设的快捷键（1~9、0），当前那套高亮；
     * 编排过技能的预设底色稍亮，一眼看出哪些键上有东西。
     */
    private void drawPresetTabs(Sketch s, SkillPresetBook book, int current, boolean active, double x, double y) {
        double size = 30;
        double gap = 6;
        for (int i = 0; i < book.presetCount(); i++) {
            double tx = x + i * (size + gap);
            boolean selected = i == current;
            boolean filled = !book.get(i).isEmpty();
            // 当前那套：激活时绿色，未激活（全程普攻）时只画白框
            s.fill(tx, y, size, size, selected && active ? Color.rgb(124, 252, 124, 0.35)
                    : filled ? Color.rgb(255, 255, 255, 0.16) : Color.rgb(255, 255, 255, 0.05));
            s.outline(tx, y, size, size, selected ? (active ? ARMED_COLOR : Color.WHITE) : Palette.BORDER_DIM,
                    selected ? 2 : 1);
            s.centered(keyLabel(i), Fonts.mono(18),
                    selected ? Color.WHITE : filled ? SLOT_TEXT_COLOR : Palette.DISABLED, tx + size / 2, y + 22);
        }
    }

    /** 第 i 套预设的快捷键显示名：规则里写的是 {@code DIGIT1} 这类 KeyCode 名，去掉前缀只留数字 */
    private String keyLabel(int presetIndex) {
        if (presetIndex >= hotkeys.size()) return "";
        String key = hotkeys.get(presetIndex);
        return key.startsWith("DIGIT") ? key.substring(5) : key;
    }

    private void drawSlot(Sketch s, SkillBook book, String skillId, int round,
                          double x, double y, double w, double h) {
        if (skillId == null) {
            s.fill(x, y, w, h, Color.rgb(255, 255, 255, 0.06));
            // 战前没有「普攻」可言，空着就是什么也不做
            s.centered(round == 0 ? "—" : "普攻", Fonts.bold(20), SLOT_TEXT_COLOR, x + w / 2, y + h / 2 + 7);
            return;
        }
        OwnedSkill owned = book.get(skillId);
        if (owned == null) {
            // 预设引用的技能暂时不在手上（例如装备卸下了）：保留占位，战斗中按普攻处理
            s.fill(x, y, w, h, Color.rgb(120, 40, 40, 0.35));
            s.centered("失效", Fonts.bold(16), Palette.DISABLED, x + w / 2, y + h / 2 + 6);
            return;
        }
        double size = Math.min(w, h) - 8;
        icons.skill(s, owned.skill(), x + (w - size) / 2, y + (h - size) / 2, size, size);
    }
}
